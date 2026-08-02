package com.example.ocare.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,                  // 서명 키. HS256 을 쓰므로 최소 32바이트 이상이어야 한다.
        Duration accessTokenValidity,   // 액세스 토큰 유효기간
        Duration refreshTokenValidity   // 리프레시 토큰 유효기간
) {
}
