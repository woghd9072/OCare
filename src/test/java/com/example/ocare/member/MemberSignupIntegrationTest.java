package com.example.ocare.member;

import com.example.ocare.member.entity.Member;
import com.example.ocare.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회원가입 API 통합 테스트.
 *
 * <p>실제 MySQL(ocare_test)에 붙어 동작한다. 중복 검증은 DB 의 UNIQUE 제약과 맞물려 있어
 * 저장소를 흉내 내면 검증의 의미가 사라지기 때문이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberSignupIntegrationTest {

    private static final String SIGNUP_URL = "/api/v1/auth/signup";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입에 성공하면 201 을 응답하고 비밀번호는 해시로 저장된다")
    void signupSuccess() throws Exception {
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("신재홍", "cosmic", "cosmic@example.com", "ocare1234")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                // 성공 응답에는 data 와 error 가 모두 실리지 않는다
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        Member saved = memberRepository.findByEmail("cosmic@example.com").orElseThrow();
        assertThat(saved.getName()).isEqualTo("신재홍");
        assertThat(saved.getNickname()).isEqualTo("cosmic");
        assertThat(saved.getCreatedAt()).isNotNull();

        // 평문이 그대로 저장되지 않았고, 원래 비밀번호로는 검증에 성공해야 한다
        assertThat(saved.getPassword()).isNotEqualTo("ocare1234");
        assertThat(passwordEncoder.matches("ocare1234", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("이미 가입된 이메일로 다시 가입하면 409 를 응답한다")
    void duplicatedEmail() throws Exception {
        signup("신재홍", "cosmic", "cosmic@example.com", "ocare1234");

        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("다른사람", "other", "cosmic@example.com", "ocare1234")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MEMBER_EMAIL_DUPLICATED"));

        assertThat(memberRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로 가입하면 409 를 응답한다")
    void duplicatedNickname() throws Exception {
        signup("신재홍", "cosmic", "cosmic@example.com", "ocare1234");

        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("다른사람", "cosmic", "other@example.com", "ocare1234")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MEMBER_NICKNAME_DUPLICATED"));

        assertThat(memberRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("검증에 실패한 항목이 모두 fieldErrors 로 내려간다")
    void validationFailure() throws Exception {
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("", "a", "not-an-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.fieldErrors[*].field",
                        containsInAnyOrder("name", "nickname", "email", "password", "password")));

        assertThat(memberRepository.count()).isZero();
    }

    @Test
    @DisplayName("비밀번호에 숫자가 없으면 가입이 거부된다")
    void passwordWithoutDigit() throws Exception {
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("신재홍", "cosmic", "cosmic@example.com", "onlyletters")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("password"));

        assertThat(memberRepository.count()).isZero();
    }

    private void signup(String name, String nickname, String email, String password) throws Exception {
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(name, nickname, email, password)))
                .andExpect(status().isCreated());
    }

    private String signupJson(String name, String nickname, String email, String password) {
        return """
                {"name":"%s","nickname":"%s","email":"%s","password":"%s"}
                """.formatted(name, nickname, email, password);
    }
}
