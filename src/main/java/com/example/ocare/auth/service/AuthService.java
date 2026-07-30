package com.example.ocare.auth.service;

import com.example.ocare.auth.dto.LoginRequest;
import com.example.ocare.auth.dto.TokenResponse;
import com.example.ocare.auth.jwt.JwtTokenProvider;
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
