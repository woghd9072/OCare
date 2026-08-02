package com.example.ocare.health.query;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 조회 API 통합 테스트.
 *
 * <p>실제 입력 데이터를 수집한 뒤 조회해, 응답 값이 원본 합계와 맞는지까지 확인한다.
 * 조회만 흉내 내면 수집·집계·조회가 실제로 연결되어 있는지는 검증되지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthQueryIntegrationTest {

    private static final String DAILY_URL = "/api/v1/health-data/daily";
    private static final String MONTHLY_URL = "/api/v1/health-data/monthly";
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
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String ownerToken;
    private String strangerToken;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleaner.clean();
        Set<String> keys = redisTemplate.keys("health:dedup:*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        memberRepository.save(Member.create("주인", "owner", "owner@example.com",
                passwordEncoder.encode(PASSWORD)));
        memberRepository.save(Member.create("타인", "stranger", "stranger@example.com",
                passwordEncoder.encode(PASSWORD)));
        ownerToken = login("owner@example.com");
        strangerToken = login("stranger@example.com");

        registerRecordKey(SAMSUNG_KEY, "SamsungHealth");
        registerRecordKey(APPLE_KEY, "Health Kit");
        ingest("INPUT_DATA1.json");
        ingest("INPUT_DATA4.json");
    }

    @Test
    @DisplayName("일별 조회 결과가 원본 합계와 일치한다")
    void findDaily() throws Exception {
        mockMvc.perform(daily(ownerToken, SAMSUNG_KEY, "2024-11-15", "2024-11-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordKey").value(SAMSUNG_KEY))
                .andExpect(jsonPath("$.data.days").value(3))
                .andExpect(jsonPath("$.data.summaries[0].date").value("2024-11-15"))
                .andExpect(jsonPath("$.data.summaries[0].steps").value(7243))
                .andExpect(jsonPath("$.data.summaries[0].calories").value(289.21))
                .andExpect(jsonPath("$.data.summaries[0].distance").value(5.41949))
                .andExpect(jsonPath("$.data.summaries[0].entryCount").value(38))
                .andExpect(jsonPath("$.data.summaries[1].steps").value(10717));
    }

    @Test
    @DisplayName("애플 데이터는 칼로리 미제공 표시와 함께 내려간다")
    void findDailyForAppleSource() throws Exception {
        mockMvc.perform(daily(ownerToken, APPLE_KEY, "2024-11-16", "2024-11-16"))
                .andExpect(status().isOk())
                // 정확한 합이 12449.99999999999997 이라 합산 후 반올림해야 12450 이 된다
                .andExpect(jsonPath("$.data.summaries[0].steps").value(12450))
                .andExpect(jsonPath("$.data.summaries[0].calories").value(0.0))
                // 이 플래그가 없으면 클라이언트가 "활동이 없었다" 로 잘못 표시한다
                .andExpect(jsonPath("$.data.summaries[0].caloriesSupported").value(false));
    }

    @Test
    @DisplayName("데이터가 없는 기간은 빈 목록과 조회 조건을 함께 준다")
    void findDailyWithNoData() throws Exception {
        mockMvc.perform(daily(ownerToken, SAMSUNG_KEY, "2025-01-01", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days").value(0))
                .andExpect(jsonPath("$.data.summaries").isEmpty())
                // 조건이 함께 오지 않으면 "활동이 없던 기간" 인지 "조회 범위 밖" 인지 알 수 없다
                .andExpect(jsonPath("$.data.from").value("2025-01-01"))
                .andExpect(jsonPath("$.data.to").value("2025-01-31"));
    }

    @Test
    @DisplayName("하루만 조회할 수 있다")
    void findDailySingleDay() throws Exception {
        mockMvc.perform(daily(ownerToken, SAMSUNG_KEY, "2024-11-15", "2024-11-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days").value(1));
    }

    @Test
    @DisplayName("월별 조회 결과가 일별 값의 합과 일치한다")
    void findMonthly() throws Exception {
        mockMvc.perform(monthly(ownerToken, SAMSUNG_KEY, "2024-11", "2024-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.months").value(2))
                .andExpect(jsonPath("$.data.summaries[0].month").value("2024-11"))
                .andExpect(jsonPath("$.data.summaries[0].steps").value(124783))
                .andExpect(jsonPath("$.data.summaries[0].activeDays").value(16))
                .andExpect(jsonPath("$.data.summaries[1].month").value("2024-12"))
                .andExpect(jsonPath("$.data.summaries[1].steps").value(115592));
    }

    @Test
    @DisplayName("측정 데이터가 있는 날만 활동 일수로 센다")
    void monthlyActiveDaysReflectsRealCoverage() throws Exception {
        mockMvc.perform(monthly(ownerToken, APPLE_KEY, "2024-12", "2024-12"))
                .andExpect(status().isOk())
                // 애플 파일은 12월 15일까지만 있다. 12월 전체 31일이 아니어야 한다.
                .andExpect(jsonPath("$.data.summaries[0].activeDays").value(15));
    }

    @Test
    @DisplayName("조회 시작이 종료보다 늦으면 400 으로 거부한다")
    void rejectsReversedPeriod() throws Exception {
        mockMvc.perform(daily(ownerToken, SAMSUNG_KEY, "2024-11-17", "2024-11-15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(monthly(ownerToken, SAMSUNG_KEY, "2024-12", "2024-11"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("조회 기간 상한을 넘으면 400 으로 거부한다")
    void rejectsTooLongPeriod() throws Exception {
        // 상한이 없으면 수 년치를 한 번에 요청해 응답이 지나치게 커진다
        mockMvc.perform(daily(ownerToken, SAMSUNG_KEY, "2024-01-01", "2025-06-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("366일")));

        mockMvc.perform(monthly(ownerToken, SAMSUNG_KEY, "2023-01", "2025-12"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("타인의 레코드키를 조회하면 403 으로 거부한다")
    void rejectsQueryForOthersRecordKey() throws Exception {
        // 인증만으로는 막히지 않는 지점이다. 조회 조건으로 받은 레코드키를 그대로 믿으면
        // 로그인한 누구나 남의 건강 데이터를 열람할 수 있다.
        mockMvc.perform(daily(strangerToken, SAMSUNG_KEY, "2024-11-15", "2024-11-17"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("RECORD_KEY_FORBIDDEN"));

        mockMvc.perform(monthly(strangerToken, SAMSUNG_KEY, "2024-11", "2024-12"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("등록되지 않은 레코드키는 404 로 거부한다")
    void rejectsUnregisteredRecordKey() throws Exception {
        mockMvc.perform(daily(ownerToken, "not-registered", "2024-11-15", "2024-11-17"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RECORD_KEY_NOT_FOUND"));
    }

    @Test
    @DisplayName("인증 없이 조회하면 401 로 거부한다")
    void rejectsUnauthenticated() throws Exception {
        mockMvc.perform(get(DAILY_URL)
                        .param("recordKey", SAMSUNG_KEY)
                        .param("from", "2024-11-15")
                        .param("to", "2024-11-17"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("필수 파라미터가 없거나 형식이 틀리면 400 으로 거부한다")
    void rejectsInvalidParameters() throws Exception {
        mockMvc.perform(get(DAILY_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .param("recordKey", SAMSUNG_KEY))
                .andExpect(status().isBadRequest());

        // 월별 조회에 일자까지 넣은 경우
        mockMvc.perform(monthly(ownerToken, SAMSUNG_KEY, "2024-11-15", "2024-12"))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.RequestBuilder daily(String token, String recordKey,
                                                                     String from, String to) {
        return get(DAILY_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("recordKey", recordKey)
                .param("from", from)
                .param("to", to);
    }

    private org.springframework.test.web.servlet.RequestBuilder monthly(String token, String recordKey,
                                                                       String from, String to) {
        return get(MONTHLY_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("recordKey", recordKey)
                .param("from", from)
                .param("to", to);
    }

    private void ingest(String fileName) throws Exception {
        mockMvc.perform(post("/api/v1/health-data")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new String(new ClassPathResource("health/" + fileName).getContentAsByteArray(),
                                StandardCharsets.UTF_8)))
                .andExpect(status().isOk());
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
