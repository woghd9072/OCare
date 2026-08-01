package com.example.ocare.health.ingest;

import com.example.ocare.member.entity.Member;
import com.example.ocare.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.ocare.support.DatabaseCleaner;
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
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 수집 API 통합 테스트.
 *
 * <p>과제로 제공된 실제 파일을 그대로 API 에 보내 저장까지 확인한다.
 * 재전송이 정상 흐름인 서비스이므로, 같은 데이터를 두 번 보내도 값이 두 배가 되지 않는지가 핵심이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthIngestIntegrationTest {

    private static final String URL = "/api/v1/health-data";
    private static final String PASSWORD = "ocare1234";
    private static final String SAMSUNG_KEY = "7836887b-b12a-440f-af0f-851546504b13";
    private static final String APPLE_KEY = "7b012e6e-ba2b-49c7-bc2e-473b7b58e72e";

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String ownerToken;
    private String strangerToken;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleaner.clean();

        Set<String> dedupKeys = redisTemplate.keys("health:dedup:*");
        if (!dedupKeys.isEmpty()) {
            redisTemplate.delete(dedupKeys);
        }

        createMember("주인", "owner", "owner@example.com");
        createMember("타인", "stranger", "stranger@example.com");
        ownerToken = login("owner@example.com");
        strangerToken = login("stranger@example.com");

        registerRecordKey(SAMSUNG_KEY, "SamsungHealth");
        registerRecordKey(APPLE_KEY, "Health Kit");
    }

    @Test
    @DisplayName("삼성 실데이터 1,066건을 모두 저장한다")
    void ingestSamsungFile() throws Exception {
        mockMvc.perform(ingest(ownerToken, "INPUT_DATA1.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.received").value(1066))
                .andExpect(jsonPath("$.data.saved").value(1066))
                .andExpect(jsonPath("$.data.duplicated").value(0))
                .andExpect(jsonPath("$.data.affectedDates.length()").value(32));

        assertThat(countRecords(SAMSUNG_KEY)).isEqualTo(1066);
        assertDailyTotal(SAMSUNG_KEY, "2024-11-15", "7243", "289.21", "5.419490");
    }

    @Test
    @DisplayName("애플 실데이터 1,459건을 소수 걸음수까지 보존해 저장한다")
    void ingestAppleFile() throws Exception {
        mockMvc.perform(ingest(ownerToken, "INPUT_DATA3.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(1459))
                .andExpect(jsonPath("$.data.affectedDates.length()").value(31));

        assertDailyTotal(APPLE_KEY, "2024-11-15", "7542.462902", "0", "6.033970");
    }

    @Test
    @DisplayName("같은 파일을 다시 보내도 데이터가 늘지 않는다")
    void resendingSamePayloadIsIdempotent() throws Exception {
        mockMvc.perform(ingest(ownerToken, "INPUT_DATA1.json")).andExpect(status().isOk());

        MvcResult second = mockMvc.perform(ingest(ownerToken, "INPUT_DATA1.json"))
                .andExpect(status().isOk())
                // 재전송은 오류가 아니라 정상 흐름이므로 200 으로 응답하되, 저장 0건임을 알려준다
                .andExpect(jsonPath("$.data.saved").value(0))
                .andExpect(jsonPath("$.data.duplicated").value(1066))
                .andReturn();

        assertThat(countRecords(SAMSUNG_KEY)).isEqualTo(1066);
        assertThat(countUploads()).isEqualTo(1);
        // 같은 payload 는 새 이력을 만들지 않고 기존 이력을 돌려준다
        assertThat(second.getResponse().getContentAsString()).contains("\"uploadId\":");
        assertDailyTotal(SAMSUNG_KEY, "2024-11-15", "7243", "289.21", "5.419490");
    }

    @Test
    @DisplayName("Redis 표시가 사라져도 DB 제약이 중복 저장을 막는다")
    void databaseConstraintIsFinalDefense() throws Exception {
        mockMvc.perform(ingest(ownerToken, "INPUT_DATA1.json")).andExpect(status().isOk());

        // 캐시가 비워진 상태를 만든다. 이때 payload 이력까지 지우면 1차/2차 방어가 모두 무력화되어
        // 마지막 방어선인 dedup_key UNIQUE 제약만 남는다.
        redisTemplate.delete(redisTemplate.keys("health:dedup:*"));
        // 길이가 같은 다른 값으로 바꿔 payload 단위 판별이 걸리지 않게 한다.
        // 컬럼이 CHAR(64) 라 문자열을 덧붙이면 저장 단계에서 잘린다.
        jdbcTemplate.update("UPDATE health_uploads SET payload_hash = SHA2(payload_hash, 256)");

        mockMvc.perform(ingest(ownerToken, "INPUT_DATA1.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(0))
                .andExpect(jsonPath("$.data.duplicated").value(1066));

        assertThat(countRecords(SAMSUNG_KEY)).isEqualTo(1066);
        assertDailyTotal(SAMSUNG_KEY, "2024-11-15", "7243", "289.21", "5.419490");
    }

    @Test
    @DisplayName("타인의 레코드키로 데이터를 보내면 403 으로 거부한다")
    void rejectsIngestForOthersRecordKey() throws Exception {
        mockMvc.perform(ingest(strangerToken, "INPUT_DATA1.json"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("RECORD_KEY_FORBIDDEN"));

        assertThat(countRecords(SAMSUNG_KEY)).isZero();
    }

    @Test
    @DisplayName("등록되지 않은 레코드키는 404 로 거부한다")
    void rejectsUnregisteredRecordKey() throws Exception {
        jdbcTemplate.update("DELETE FROM record_keys WHERE record_key = ?", SAMSUNG_KEY);

        mockMvc.perform(ingest(ownerToken, "INPUT_DATA1.json"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RECORD_KEY_NOT_FOUND"));
    }

    @Test
    @DisplayName("수집이 중단된 레코드키는 409 로 거부한다")
    void rejectsInactiveRecordKey() throws Exception {
        jdbcTemplate.update("UPDATE record_keys SET status = 'INACTIVE' WHERE record_key = ?", SAMSUNG_KEY);

        mockMvc.perform(ingest(ownerToken, "INPUT_DATA1.json"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RECORD_KEY_INACTIVE"));

        assertThat(countRecords(SAMSUNG_KEY)).isZero();
    }

    @Test
    @DisplayName("인증 없이 수집을 시도하면 401 로 거부한다")
    void rejectsUnauthenticated() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(readFile("INPUT_DATA1.json")))
                .andExpect(status().isUnauthorized());

        assertThat(countRecords(SAMSUNG_KEY)).isZero();
    }

    @Test
    @DisplayName("알 수 없는 출처가 담긴 payload 는 400 으로 거부한다")
    void rejectsUnknownSource() throws Exception {
        String tampered = readFile("INPUT_DATA1.json").replace("\"SamsungHealth\"", "\"FitbitHealth\"");

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tampered))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        assertThat(countRecords(SAMSUNG_KEY)).isZero();
    }

    private void assertDailyTotal(String recordKey, String date, String steps, String calories, String distanceKm) {
        assertThat(sumOf(recordKey, date, "steps")).as("%s 걸음수", date).isEqualByComparingTo(steps);
        assertThat(sumOf(recordKey, date, "calories")).as("%s 칼로리", date).isEqualByComparingTo(calories);
        assertThat(sumOf(recordKey, date, "distance_km")).as("%s 이동거리", date).isEqualByComparingTo(distanceKm);
    }

    private BigDecimal sumOf(String recordKey, String date, String column) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(" + column + "), 0) FROM health_records "
                        + "WHERE record_key = ? AND measured_date = ?",
                BigDecimal.class, recordKey, date);
    }

    private Integer countRecords(String recordKey) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM health_records WHERE record_key = ?", Integer.class, recordKey);
    }

    private Integer countUploads() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM health_uploads", Integer.class);
    }

    private org.springframework.test.web.servlet.RequestBuilder ingest(String token, String fileName)
            throws IOException {
        return post(URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFile(fileName));
    }

    private String readFile(String fileName) throws IOException {
        return new String(new ClassPathResource("health/" + fileName).getContentAsByteArray(),
                StandardCharsets.UTF_8);
    }

    private void createMember(String name, String nickname, String email) {
        memberRepository.save(Member.create(name, nickname, email, passwordEncoder.encode(PASSWORD)));
    }

    private void registerRecordKey(String recordKey, String source) throws Exception {
        mockMvc.perform(post("/api/v1/record-keys")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recordKey":"%s","source":"%s"}
                                """.formatted(recordKey, source)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int start = body.indexOf("\"accessToken\":\"") + 15;
        return body.substring(start, body.indexOf('"', start));
    }
}
