package com.example.ocare.health.summary;

import com.example.ocare.member.entity.Member;
import com.example.ocare.member.repository.MemberRepository;
import com.example.ocare.support.DatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 입력 데이터를 수집한 뒤 집계값이 원본 합계와 일치하는지 검증한다.
 *
 * <p>기대값은 원본 JSON 을 직접 합산해 구한 값이다. 집계 로직이 계산한 값을 그대로
 * 기대값으로 쓰면 로직이 틀려도 테스트가 통과하므로, 코드와 무관한 기준을 사용한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthSummaryAccuracyTest {

    private static final String PASSWORD = "ocare1234";
    private static final String SAMSUNG_KEY = "7836887b-b12a-440f-af0f-851546504b13";
    private static final String APPLE_KEY = "e27ba7ef-8bb2-424c-af1d-877e826b7487";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleaner.clean();
        Set<String> keys = redisTemplate.keys("health:dedup:*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        memberRepository.save(Member.create("집계검증", "accuracy", "accuracy@example.com",
                passwordEncoder.encode(PASSWORD)));
        token = login();
        registerRecordKey(SAMSUNG_KEY, "SamsungHealth");
        registerRecordKey(APPLE_KEY, "Health Kit");
    }

    @Test
    @DisplayName("삼성 데이터의 일별 집계가 원본 합계와 일치한다")
    void samsungDailySummaryMatchesSource() throws Exception {
        ingest("INPUT_DATA1.json");

        // 기대값은 원본 JSON 을 직접 합산해 구한 값이다.
        assertDaily(SAMSUNG_KEY, "2024-11-15", 7243, "289.2100", "5.419490", 38);

        // 칼로리만 원본 합(425.52994815)과 0.0001 차이가 난다.
        // health_records.calories 가 DECIMAL(10,4) 라 각 구간이 저장될 때 이미 4자리로 반올림되고,
        // 그 값들을 더하기 때문이다. 걸음수처럼 합산 후 한 번만 반올림할 수 있는 값이 아니라
        // 저장 정밀도에서 비롯된 차이이며, 0.0001 kcal 은 서비스 의미상 무시할 수 있는 수준이다.
        assertDaily(SAMSUNG_KEY, "2024-11-16", 10717, "425.5300", "8.020480", 44);
        assertThat(dailyRowCount(SAMSUNG_KEY)).isEqualTo(32);
    }

    @Test
    @DisplayName("애플 데이터의 소수 걸음수가 합산 후 한 번만 반올림된다")
    void appleDailySummaryRoundsOnceAfterSummation() throws Exception {
        ingest("INPUT_DATA4.json");

        // 정확한 합은 12449.99999999999997 이다.
        // 구간별로 반올림했다면 12449 가 되어 실제 걸음수와 어긋난다.
        assertThat(dailySteps(APPLE_KEY, "2024-11-16")).isEqualTo(12450);
        assertThat(dailySteps(APPLE_KEY, "2024-11-15")).isEqualTo(6672);
        assertThat(dailyRowCount(APPLE_KEY)).isEqualTo(31);
    }

    @Test
    @DisplayName("모든 일별 집계가 원본 합계와 일치한다")
    void allDailySummariesMatchRawRecords() throws Exception {
        ingest("INPUT_DATA1.json");
        ingest("INPUT_DATA4.json");

        // 집계 테이블과 원본을 전 구간에 걸쳐 대조한다. 한 건이라도 어긋나면 실패한다.
        Integer mismatches = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM health_daily_summaries d
                JOIN (SELECT record_key, measured_date,
                             ROUND(SUM(steps)) AS steps, SUM(calories) AS calories,
                             SUM(distance_km) AS distance_km, COUNT(*) AS entry_count
                      FROM health_records
                      GROUP BY record_key, measured_date) r
                  ON r.record_key = d.record_key AND r.measured_date = d.summary_date
                WHERE d.steps <> r.steps
                   OR d.calories <> r.calories
                   OR d.distance_km <> r.distance_km
                   OR d.entry_count <> r.entry_count
                """, Integer.class);

        assertThat(mismatches).isZero();
        assertThat(dailyRowCount(SAMSUNG_KEY) + dailyRowCount(APPLE_KEY)).isEqualTo(63);
    }

    @Test
    @DisplayName("월별 집계가 일별 값의 합과 일치한다")
    void monthlySummaryEqualsSumOfDaily() throws Exception {
        ingest("INPUT_DATA1.json");

        Integer mismatches = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM health_monthly_summaries m
                JOIN (SELECT record_key, DATE_FORMAT(summary_date, '%Y-%m') AS mon,
                             SUM(steps) AS steps, SUM(calories) AS calories,
                             SUM(distance_km) AS distance_km, COUNT(*) AS active_days
                      FROM health_daily_summaries
                      GROUP BY record_key, mon) d
                  ON d.record_key = m.record_key AND d.mon = m.summary_month
                WHERE m.steps <> d.steps
                   OR m.calories <> d.calories
                   OR m.distance_km <> d.distance_km
                   OR m.active_days <> d.active_days
                """, Integer.class);

        assertThat(mismatches).isZero();
    }

    @Test
    @DisplayName("측정 데이터가 있는 날만 활동 일수로 센다")
    void activeDaysCountsOnlyDaysWithData() throws Exception {
        ingest("INPUT_DATA4.json");

        // 애플 파일은 12월 15일까지만 있다. 12월 전체 31일이 아니라 실제 측정된 15일이어야 한다.
        assertThat(activeDays(APPLE_KEY, "2024-11")).isEqualTo(16);
        assertThat(activeDays(APPLE_KEY, "2024-12")).isEqualTo(15);
    }

    @Test
    @DisplayName("애플 데이터는 칼로리 미제공으로 표시된다")
    void appleSummaryMarksCaloriesUnsupported() throws Exception {
        ingest("INPUT_DATA1.json");
        ingest("INPUT_DATA4.json");

        assertThat(caloriesSupported(APPLE_KEY)).isFalse();
        assertThat(caloriesSupported(SAMSUNG_KEY)).isTrue();
    }

    @Test
    @DisplayName("같은 파일을 다시 수집해도 집계값이 두 배가 되지 않는다")
    void resendDoesNotDoubleSummary() throws Exception {
        ingest("INPUT_DATA1.json");
        ingest("INPUT_DATA1.json");

        assertDaily(SAMSUNG_KEY, "2024-11-15", 7243, "289.2100", "5.419490", 38);
        assertDaily(SAMSUNG_KEY, "2024-11-16", 10717, "425.5300", "8.020480", 44);
        assertThat(dailyRowCount(SAMSUNG_KEY)).isEqualTo(32);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM health_monthly_summaries WHERE record_key = ?",
                Integer.class, SAMSUNG_KEY)).isEqualTo(2);
    }

    private void assertDaily(String recordKey, String date, int steps, String calories,
                             String distanceKm, int entryCount) {
        assertThat(dailySteps(recordKey, date)).as("%s 걸음수", date).isEqualTo(steps);
        assertThat(dailyDecimal(recordKey, date, "calories")).as("%s 칼로리", date)
                .isEqualByComparingTo(calories);
        assertThat(dailyDecimal(recordKey, date, "distance_km")).as("%s 이동거리", date)
                .isEqualByComparingTo(distanceKm);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT entry_count FROM health_daily_summaries WHERE record_key = ? AND summary_date = ?",
                Integer.class, recordKey, date)).as("%s 구간 수", date).isEqualTo(entryCount);
    }

    private Integer dailySteps(String recordKey, String date) {
        return jdbcTemplate.queryForObject(
                "SELECT steps FROM health_daily_summaries WHERE record_key = ? AND summary_date = ?",
                Integer.class, recordKey, date);
    }

    private BigDecimal dailyDecimal(String recordKey, String date, String column) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM health_daily_summaries WHERE record_key = ? AND summary_date = ?",
                BigDecimal.class, recordKey, date);
    }

    private Integer dailyRowCount(String recordKey) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM health_daily_summaries WHERE record_key = ?", Integer.class, recordKey);
    }

    private Integer activeDays(String recordKey, String month) {
        return jdbcTemplate.queryForObject(
                "SELECT active_days FROM health_monthly_summaries WHERE record_key = ? AND summary_month = ?",
                Integer.class, recordKey, month);
    }

    private Boolean caloriesSupported(String recordKey) {
        return jdbcTemplate.queryForObject(
                "SELECT MIN(calories_supported) FROM health_daily_summaries WHERE record_key = ?",
                Boolean.class, recordKey);
    }

    private void ingest(String fileName) throws Exception {
        mockMvc.perform(post("/api/v1/health-data")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new String(new ClassPathResource("health/" + fileName).getContentAsByteArray(),
                                StandardCharsets.UTF_8)))
                .andExpect(status().isOk());
    }

    private void registerRecordKey(String recordKey, String source) throws Exception {
        mockMvc.perform(post("/api/v1/record-keys")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recordKey":"%s","source":"%s"}
                                """.formatted(recordKey, source)))
                .andExpect(status().isCreated());
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"accuracy@example.com","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int start = body.indexOf("\"accessToken\":\"") + 15;
        return body.substring(start, body.indexOf('"', start));
    }
}
