package com.example.ocare.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 발급과 검증을 담당한다.
 *
 * <p>토큰에는 회원 식별자(subject)와 용도(type)만 담는다.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TOKEN_TYPE = "type";

    private final SecretKey secretKey;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;

    public JwtTokenProvider(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = properties.accessTokenValidity();
        this.refreshTokenValidity = properties.refreshTokenValidity();
    }

    public String createAccessToken(Long memberId) {
        return createToken(memberId, TokenType.ACCESS, accessTokenValidity);
    }

    public String createRefreshToken(Long memberId) {
        return createToken(memberId, TokenType.REFRESH, refreshTokenValidity);
    }

    private String createToken(Long memberId, TokenType tokenType, Duration validity) {
        Instant now = Instant.now();
        return Jwts.builder()
                // JWT 의 iat/exp 는 초 단위라, 같은 회원이 같은 초에 발급받으면
                // 나머지 클레임이 동일해 완전히 같은 토큰이 만들어진다.
                // 그러면 재발급해도 이전 리프레시 토큰과 값이 같아 토큰 회전이 무력화된다.
                // 매번 다른 식별자를 넣어 토큰이 항상 유일하도록 보장한다.
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(memberId))
                .claim(CLAIM_TOKEN_TYPE, tokenType.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰을 검증하고 회원 식별자를 반환한다.
     *
     * @param tokenType 기대하는 토큰 용도. 리프레시 토큰으로 API 를 호출하는 것을 막기 위해 확인한다.
     * @throws InvalidTokenException 서명 불일치, 만료, 용도 불일치 등 검증에 실패한 모든 경우
     */
    public Long getMemberId(String token, TokenType tokenType) {
        Claims claims = parseClaims(token);

        String actualType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!tokenType.name().equals(actualType)) {
            throw new InvalidTokenException("토큰 용도가 올바르지 않습니다.");
        }

        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidTokenException("토큰에 담긴 회원 식별자가 올바르지 않습니다.", e);
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // 만료/위조/형식 오류를 구분해 응답하지 않는다. 공격자에게 단서를 주지 않기 위함이며,
            // 원인은 로그로만 남긴다.
            log.debug("JWT 검증 실패: {}", e.getMessage());
            throw new InvalidTokenException("유효하지 않은 토큰입니다.", e);
        }
    }

    /**
     * 리프레시 토큰의 남은 유효기간. Redis 저장 시 TTL 로 사용한다.
     */
    public Duration getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    /**
     * 액세스 토큰 유효기간. 응답의 expiresIn 값으로 사용한다.
     */
    public Duration getAccessTokenValidity() {
        return accessTokenValidity;
    }
}
