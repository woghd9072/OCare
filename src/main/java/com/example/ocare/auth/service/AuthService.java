package com.example.ocare.auth.service;

import com.example.ocare.auth.dto.LoginRequest;
import com.example.ocare.auth.dto.TokenResponse;
import com.example.ocare.auth.jwt.JwtTokenProvider;
import com.example.ocare.auth.jwt.TokenType;
import com.example.ocare.auth.token.RefreshTokenStore;
import com.example.ocare.common.exception.BusinessException;
import com.example.ocare.common.exception.ErrorCode;
import com.example.ocare.member.entity.Member;
import com.example.ocare.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    /**
     * 이메일과 비밀번호로 로그인하고 토큰을 발급한다.
     *
     * <p>회원이 없는 경우와 비밀번호가 틀린 경우를 같은 오류로 처리한다.
     * 두 경우를 구분해 응답하면 어떤 이메일이 가입되어 있는지 확인하는 수단이 되기 때문이다.
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        return issueTokens(member.getId());
    }

    /**
     * 리프레시 토큰으로 새 토큰을 발급한다.
     *
     * <p>검증을 두 단계로 한다.
     * <ol>
     *   <li>토큰 자체의 서명과 만료, 그리고 용도가 REFRESH 인지</li>
     *   <li>저장소에 보관된 값과 일치하는지</li>
     * </ol>
     * 서명이 유효해도 저장소에 없으면 이미 로그아웃했거나 회전으로 폐기된 토큰이므로 거부한다.
     * 이 두 번째 검사가 없으면 서버가 발급 이후를 전혀 통제할 수 없다.
     *
     * <p>재발급에 성공하면 리프레시 토큰도 새로 만들어 저장소를 덮어쓴다(회전).
     * 이전 토큰이 계속 유효하면, 탈취된 토큰이 유효기간 내내 재발급에 쓰일 수 있다.
     */
    public TokenResponse refresh(String refreshToken) {
        Long memberId = jwtTokenProvider.getMemberId(refreshToken, TokenType.REFRESH);

        if (!refreshTokenStore.matches(memberId, refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "만료되었거나 사용할 수 없는 토큰입니다.");
        }

        return issueTokens(memberId);
    }

    /**
     * 로그아웃. 저장소의 리프레시 토큰을 지워 재발급을 막는다.
     */
    public void logout(Long memberId) {
        refreshTokenStore.delete(memberId);
    }

    /**
     * 액세스/리프레시 토큰을 발급하고 리프레시 토큰을 저장소에 보관한다.
     */
    private TokenResponse issueTokens(Long memberId) {
        String accessToken = jwtTokenProvider.createAccessToken(memberId);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberId);

        refreshTokenStore.save(memberId, refreshToken, jwtTokenProvider.getRefreshTokenValidity());

        return TokenResponse.of(accessToken, refreshToken,
                jwtTokenProvider.getAccessTokenValidity().toSeconds());
    }
}
