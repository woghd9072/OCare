package com.example.ocare.health.query;

import com.example.ocare.member.entity.Member;
import com.example.ocare.member.repository.MemberRepository;
import com.example.ocare.support.DatabaseCleaner;
import java.nio.charset.StandardCharsets;
import java.util.Set;
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
import org.springframework.test.web.servlet.RequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 조회 캐시가 API 흐름에 제대로 물려 있는지 검증한다.
 *
 * <p>캐시 자체의 동작은 별도 테스트에서 확인했다. 여기서는 캐시가 실제로 쓰이는지,
 * 그리고 수집으로 값이 바뀐 뒤 옛 값이 남지 않는지를 본다.
 * 후자가 캐시를 넣을 때 가장 깨지기 쉬운 부분이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthQueryCacheIntegrationTest {

    private static final String DAILY_URL = "/api/v1/health-data/daily";
    private static final String PASSWORD = "ocare1234";
    private static final String SAMSUNG_KEY = "7836887b-b12a-440f-af0f-851546504b13";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String ownerToken;
    private String strangerToken;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleaner.clean();
        clearRedis("health:dedup:*");
        clearRedis("health:summary:*");

        memberRepository.save(Member.create("주인", "owner", "owner@example.com",
                passwordEncoder.encode(PASSWORD)));
        memberRepository.save(Member.create("타인", "stranger", "stranger@example.com",
                passwordEncoder.encode(PASSWORD)));
        ownerToken = login("owner@example.com");
        strangerToken = login("stranger@example.com");

        registerRecordKey(SAMSUNG_KEY);
        ingest("INPUT_DATA1.json");
    }

    @Test
    @DisplayName("첫 조회 결과가 캐시에 저장된다")
    void firstQueryPopulatesCache() throws Exception {
        assertThat(cacheKeys()).isEmpty();

        mockMvc.perform(daily(ownerToken, "2024-11-15", "2024-11-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summaries[0].steps").value(7243));

        assertThat(cacheKeys()).hasSize(1);
    }

    @Test
    @DisplayName("두 번째 조회는 DB 를 읽지 않고 같은 결과를 준다")
    void secondQueryUsesCache() throws Exception {
        mockMvc.perform(daily(ownerToken, "2024-11-15", "2024-11-17")).andExpect(status().isOk());

        // 집계 테이블을 비워도 캐시가 살아 있으면 같은 결과가 나온다.
        // 캐시를 거치지 않았다면 빈 결과가 나올 것이다.
        jdbcTemplate.update("DELETE FROM health_daily_summaries");

        mockMvc.perform(daily(ownerToken, "2024-11-15", "2024-11-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days").value(3))
                .andExpect(jsonPath("$.data.summaries[0].steps").value(7243));
    }

    @Test
    @DisplayName("수집으로 값이 바뀌면 캐시된 옛 값이 아니라 새 값을 준다")
    void ingestInvalidatesCache() throws Exception {
        mockMvc.perform(daily(ownerToken, "2024-11-15", "2024-11-17"))
                .andExpect(jsonPath("$.data.summaries[0].steps").value(7243));

        // 같은 레코드키에 다른 payload 를 수집해 11월 15일 값을 늘린다
        ingestExtraEntry();

        mockMvc.perform(daily(ownerToken, "2024-11-15", "2024-11-17"))
                .andExpect(status().isOk())
                // 무효화가 새면 여기서 7243 이 그대로 나온다
                .andExpect(jsonPath("$.data.summaries[0].steps").value(7343));
    }

    @Test
    @DisplayName("무효화는 해당 레코드키에만 적용된다")
    void invalidationDoesNotAffectOtherRecordKeys() throws Exception {
        mockMvc.perform(daily(ownerToken, "2024-11-15", "2024-11-17")).andExpect(status().isOk());
        int before = cacheKeys().size();

        // 다른 레코드키를 등록하고 수집한다
        registerRecordKey("e27ba7ef-8bb2-424c-af1d-877e826b7487");
        ingest("INPUT_DATA4.json");

        // 기존 레코드키의 캐시 항목은 그대로 남아 있어야 한다
        assertThat(cacheKeys()).hasSizeGreaterThanOrEqualTo(before);
        jdbcTemplate.update("DELETE FROM health_daily_summaries WHERE record_key = ?", SAMSUNG_KEY);
        mockMvc.perform(daily(ownerToken, "2024-11-15", "2024-11-17"))
                .andExpect(jsonPath("$.data.summaries[0].steps").value(7243));
    }

    @Test
    @DisplayName("캐시가 있어도 타인은 조회할 수 없다")
    void cacheDoesNotBypassOwnershipCheck() throws Exception {
        mockMvc.perform(daily(ownerToken, "2024-11-15", "2024-11-17")).andExpect(status().isOk());

        // 소유권 확인을 캐시보다 뒤에 두면 여기서 캐시된 남의 데이터가 그대로 나간다
        mockMvc.perform(daily(strangerToken, "2024-11-15", "2024-11-17"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Redis 가 비어도 조회는 정상 동작한다")
    void queryWorksWithoutCache() throws Exception {
        mockMvc.perform(daily(ownerToken, "2024-11-15", "2024-11-17")).andExpect(status().isOk());

        clearRedis("health:summary:*");

        mockMvc.perform(daily(ownerToken, "2024-11-15", "2024-11-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summaries[0].steps").value(7243));
    }

    /**
     * 11월 15일에 100걸음을 더하는 payload 를 수집한다.
     */
    private void ingestExtraEntry() throws Exception {
        String payload = """
                {
                  "recordkey": "%s",
                  "type": "steps",
                  "lastUpdate": "2024-12-17 10:00:00 +0000",
                  "data": {
                    "source": {"mode": 9, "product": {"name": "Android", "vender": "Samsung"},
                               "name": "SamsungHealth", "type": ""},
                    "entries": [
                      {"period": {"from": "2024-11-15 23:50:00", "to": "2024-11-15 23:59:59"},
                       "distance": {"unit": "km", "value": 0.08},
                       "calories": {"unit": "kcal", "value": 4.0},
                       "steps": 100}
                    ]
                  }
                }
                """.formatted(SAMSUNG_KEY);

        mockMvc.perform(post("/api/v1/health-data")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(1));
    }

    private Set<String> cacheKeys() {
        return redisTemplate.keys("health:summary:daily:*");
    }

    private void clearRedis(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private RequestBuilder daily(String token, String from, String to) {
        return get(DAILY_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("recordKey", SAMSUNG_KEY)
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

    private void registerRecordKey(String recordKey) throws Exception {
        mockMvc.perform(post("/api/v1/record-keys")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recordKey":"%s","source":"SamsungHealth"}
                                """.formatted(recordKey)))
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
