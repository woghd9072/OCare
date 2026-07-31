package com.example.ocare.recordkey;

import com.example.ocare.common.exception.BusinessException;
import com.example.ocare.health.HealthSource;
import com.example.ocare.member.entity.Member;
import com.example.ocare.member.repository.MemberRepository;
import com.example.ocare.recordkey.entity.RecordKey;
import com.example.ocare.recordkey.repository.RecordKeyRepository;
import com.example.ocare.recordkey.service.RecordKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 레코드키 등록과 소유권 검증 통합 테스트.
 *
 * <p>실제 입력 데이터에서 확인된 recordkey 와 출처 표기를 그대로 사용한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecordKeyIntegrationTest {

    private static final String URL = "/api/v1/record-keys";
    private static final String PASSWORD = "ocare1234";

    /** INPUT_DATA1 의 recordkey (삼성헬스) */
    private static final String SAMSUNG_KEY = "7836887b-b12a-440f-af0f-851546504b13";
    /** INPUT_DATA3 의 recordkey (애플 HealthKit) */
    private static final String APPLE_KEY = "7b012e6e-ba2b-49c7-bc2e-473b7b58e72e";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RecordKeyRepository recordKeyRepository;

    @Autowired
    private RecordKeyService recordKeyService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long ownerId;
    private Long strangerId;
    private String ownerToken;
    private String strangerToken;

    @BeforeEach
    void setUp() throws Exception {
        recordKeyRepository.deleteAll();
        memberRepository.deleteAll();

        ownerId = createMember("주인", "owner", "owner@example.com");
        strangerId = createMember("타인", "stranger", "stranger@example.com");
        ownerToken = login("owner@example.com");
        strangerToken = login("stranger@example.com");
    }

    @Test
    @DisplayName("레코드키를 등록하면 원본 출처 표기가 그대로 저장된다")
    void register() throws Exception {
        mockMvc.perform(register(ownerToken, SAMSUNG_KEY, "SamsungHealth"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recordKey").value(SAMSUNG_KEY))
                .andExpect(jsonPath("$.data.source").value("SamsungHealth"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        RecordKey saved = recordKeyRepository.findByRecordKey(SAMSUNG_KEY).orElseThrow();
        assertThat(saved.getSource()).isEqualTo(HealthSource.SAMSUNG_HEALTH);
        assertThat(saved.isOwnedBy(ownerId)).isTrue();
    }

    @Test
    @DisplayName("공백이 들어간 출처 표기도 받아들인다")
    void registerAppleSource() throws Exception {
        mockMvc.perform(register(ownerToken, APPLE_KEY, "Health Kit"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.source").value("Health Kit"));

        assertThat(recordKeyRepository.findByRecordKey(APPLE_KEY).orElseThrow().getSource())
                .isEqualTo(HealthSource.APPLE_HEALTH);
    }

    @Test
    @DisplayName("같은 레코드키를 다시 등록하면 409 로 거부한다")
    void registerDuplicated() throws Exception {
        mockMvc.perform(register(ownerToken, SAMSUNG_KEY, "SamsungHealth"))
                .andExpect(status().isCreated());

        mockMvc.perform(register(ownerToken, SAMSUNG_KEY, "SamsungHealth"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RECORD_KEY_DUPLICATED"));
    }

    @Test
    @DisplayName("다른 회원이 등록한 레코드키도 같은 409 로 거부한다")
    void registerKeyOwnedByOther() throws Exception {
        mockMvc.perform(register(ownerToken, SAMSUNG_KEY, "SamsungHealth"))
                .andExpect(status().isCreated());

        // 소유자를 알려주지 않는다. 임의의 키로 등록 여부를 확인하는 수단이 되기 때문이다.
        mockMvc.perform(register(strangerToken, SAMSUNG_KEY, "SamsungHealth"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RECORD_KEY_DUPLICATED"));

        assertThat(recordKeyRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("알 수 없는 출처는 400 으로 거부한다")
    void registerUnknownSource() throws Exception {
        mockMvc.perform(register(ownerToken, SAMSUNG_KEY, "FitbitHealth"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        assertThat(recordKeyRepository.count()).isZero();
    }

    @Test
    @DisplayName("목록 조회는 본인이 등록한 레코드키만 반환한다")
    void findMineReturnsOnlyOwn() throws Exception {
        mockMvc.perform(register(ownerToken, SAMSUNG_KEY, "SamsungHealth")).andExpect(status().isCreated());
        mockMvc.perform(register(ownerToken, APPLE_KEY, "Health Kit")).andExpect(status().isCreated());

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 타인의 목록은 비어 있어야 한다
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("소유자는 자신의 레코드키를 가져올 수 있다")
    void getOwnedRecordKey() throws Exception {
        mockMvc.perform(register(ownerToken, SAMSUNG_KEY, "SamsungHealth")).andExpect(status().isCreated());

        RecordKey found = recordKeyService.getOwnedRecordKey(ownerId, SAMSUNG_KEY);

        assertThat(found.getRecordKey()).isEqualTo(SAMSUNG_KEY);
        assertThat(found.isActive()).isTrue();
    }

    @Test
    @DisplayName("타인의 레코드키에 접근하면 403 으로 거부한다")
    void getRecordKeyOwnedByOther() throws Exception {
        mockMvc.perform(register(ownerToken, SAMSUNG_KEY, "SamsungHealth")).andExpect(status().isCreated());

        // 인증만으로는 막히지 않는 지점이다. 로그인한 타인이 남의 키를 넣어 요청할 수 있으므로
        // 소유권 검증이 반드시 필요하다.
        assertThatThrownBy(() -> recordKeyService.getOwnedRecordKey(strangerId, SAMSUNG_KEY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("본인이 등록한 레코드키가 아닙니다.");
    }

    @Test
    @DisplayName("등록되지 않은 레코드키는 404 로 거부한다")
    void getUnregisteredRecordKey() {
        assertThatThrownBy(() -> recordKeyService.getOwnedRecordKey(ownerId, "not-registered-key"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("등록되지 않은 레코드키입니다.");
    }

    @Test
    @DisplayName("인증 없이 등록하면 401 을 응답한다")
    void registerWithoutToken() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(SAMSUNG_KEY, "SamsungHealth")))
                .andExpect(status().isUnauthorized());
    }

    private Long createMember(String name, String nickname, String email) {
        return memberRepository.save(
                Member.create(name, nickname, email, passwordEncoder.encode(PASSWORD))
        ).getId();
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int start = body.indexOf("\"accessToken\":\"") + 15;
        return body.substring(start, body.indexOf('"', start));
    }

    private org.springframework.test.web.servlet.RequestBuilder register(String token, String recordKey, String source) {
        return post(URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(recordKey, source));
    }

    private String registerJson(String recordKey, String source) {
        return """
                {"recordKey":"%s","source":"%s","productName":"Android","productVendor":"Samsung"}
                """.formatted(recordKey, source);
    }
}
