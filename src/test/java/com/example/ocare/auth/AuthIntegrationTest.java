package com.example.ocare.auth;

import com.example.ocare.auth.jwt.JwtTokenProvider;
import com.example.ocare.auth.token.RefreshTokenStore;
import com.example.ocare.member.entity.Member;
import com.example.ocare.member.repository.MemberRepository;
import com.example.ocare.support.DatabaseCleaner;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 로그인·재발급·로그아웃과 인증 필터의 통합 테스트.
 *
 * <p>실제 MySQL 과 Redis 에 붙어 동작한다. 토큰 회전과 로그아웃은 저장소 상태에 의존하므로
 * 흉내 내면 검증의 의미가 사라진다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String EMAIL = "auth-test@example.com";
    private static final String PASSWORD = "ocare1234";
    private static final String PROTECTED_URL = "/api/v1/health-data/daily";

    /**
     * 인증 통과 여부만 보고 싶을 때 사용하는, 매핑되지 않은 보호 경로.
     *
     * <p>실제 API 경로를 쓰면 그 API 의 파라미터 검증 결과에 따라 상태 코드가 달라져
     * 인증과 무관한 이유로 테스트가 깨진다.
     */
    private static final String UNMAPPED_PROTECTED_URL = "/api/v1/health-data/no-such-endpoint";

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Long memberId;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        Member member = memberRepository.save(
                Member.create("인증테스트", "authtester", EMAIL, passwordEncoder.encode(PASSWORD))
        );
        memberId = member.getId();
        refreshTokenStore.delete(memberId);
    }

    @Test
    @DisplayName("이메일과 비밀번호가 일치하면 토큰을 발급한다")
    void loginSuccess() throws Exception {
        mockMvc.perform(login(EMAIL, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").isNumber());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401 로 거부한다")
    void loginWithWrongPassword() throws Exception {
        mockMvc.perform(login(EMAIL, "wrongpassword1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
    }

    @Test
    @DisplayName("가입되지 않은 이메일도 비밀번호 오류와 같은 응답을 준다")
    void loginWithUnknownEmail() throws Exception {
        mockMvc.perform(login("unknown@example.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                // 가입 여부를 추측할 수 없도록 응답이 동일해야 한다
                .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"))
                .andExpect(jsonPath("$.error.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("토큰 없이 보호된 경로에 접근하면 401 을 응답한다")
    void accessWithoutToken() throws Exception {
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("액세스 토큰이 있으면 인증을 통과한다")
    void accessWithValidToken() throws Exception {
        String accessToken = extract(login(), "accessToken");

        // 매핑되지 않은 경로라 404 가 나오지만, 401 이 아니라는 것은 인증 단계를 통과했다는 뜻이다.
        mockMvc.perform(get(UNMAPPED_PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("리프레시 토큰으로는 API 인증을 통과할 수 없다")
    void accessWithRefreshToken() throws Exception {
        String refreshToken = extract(login(), "refreshToken");

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("페이로드가 변조된 토큰은 거부한다")
    void accessWithTamperedToken() throws Exception {
        String accessToken = extract(login(), "accessToken");

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperPayload(accessToken)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 토큰의 페이로드 한 글자를 바꿔 서명과 어긋나게 만든다.
     *
     * <p>서명 끝에 문자를 덧붙이는 방식은 쓰지 않는다. HS384 서명은 48바이트이고
     * base64url 로 64자에 정확히 들어맞아, 한 글자를 더해도 남는 비트가 버려져
     * 디코딩 결과가 같아진다. 즉 실제로는 변조되지 않아 검증을 통과한다.
     */
    private String tamperPayload(String token) {
        String[] parts = token.split("\\.");
        String payload = parts[1];
        char lastChar = payload.charAt(payload.length() - 1);
        char replacement = lastChar == 'A' ? 'B' : 'A';
        return parts[0] + "." + payload.substring(0, payload.length() - 1) + replacement + "." + parts[2];
    }

    @Test
    @DisplayName("같은 회원이 연속으로 발급받아도 토큰은 매번 달라진다")
    void tokensAreUnique() throws Exception {
        String first = extract(login(), "refreshToken");
        String second = extract(login(), "refreshToken");

        // JWT 의 iat/exp 는 초 단위라, 고유 식별자가 없으면 같은 초에 발급된 토큰이 동일해진다.
        // 그 경우 아래 토큰 회전 검증이 무의미해진다.
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("재발급에 성공하면 이전 리프레시 토큰은 무효가 된다")
    void refreshRotatesToken() throws Exception {
        String oldRefreshToken = extract(login(), "refreshToken");

        MvcResult refreshed = mockMvc.perform(refresh(oldRefreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        String newRefreshToken = extract(refreshed, "refreshToken");

        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        mockMvc.perform(refresh(oldRefreshToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(refresh(newRefreshToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("액세스 토큰으로는 재발급할 수 없다")
    void refreshWithAccessToken() throws Exception {
        String accessToken = extract(login(), "accessToken");

        mockMvc.perform(refresh(accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("발급한 적 없는 리프레시 토큰은 서명이 유효해도 거부한다")
    void refreshWithUnknownToken() throws Exception {
        // 서명은 유효하지만 저장소에 없는 토큰. 로그아웃 이후나 회전으로 폐기된 상황과 같다.
        String notStored = jwtTokenProvider.createRefreshToken(memberId);

        mockMvc.perform(refresh(notStored))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃하면 재발급이 막힌다")
    void logout() throws Exception {
        MvcResult loginResult = login();
        String accessToken = extract(loginResult, "accessToken");
        String refreshToken = extract(loginResult, "refreshToken");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());

        assertThat(refreshTokenStore.matches(memberId, refreshToken)).isFalse();
        mockMvc.perform(refresh(refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없이 로그아웃을 호출하면 401 을 응답한다")
    void logoutWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(login(EMAIL, PASSWORD))
                .andExpect(status().isOk())
                .andReturn();
    }

    private RequestBuilder login(String email, String password) {
        return post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password));
    }

    private RequestBuilder refresh(String refreshToken) {
        return post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":"%s"}
                        """.formatted(refreshToken));
    }

    private String extract(MvcResult result, String field) throws Exception {
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"" + field + "\":\"") + field.length() + 4;
        return body.substring(start, body.indexOf('"', start));
    }
}
