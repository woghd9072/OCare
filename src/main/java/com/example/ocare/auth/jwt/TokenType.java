package com.example.ocare.auth.jwt;

/**
 * 토큰 용도.
 *
 * <p>액세스 토큰과 리프레시 토큰은 같은 키로 서명되므로, 용도를 토큰 안에 명시하지 않으면
 * 유효기간이 긴 리프레시 토큰을 그대로 API 인증에 사용할 수 있게 된다.
 * 발급 시 이 값을 클레임에 담고 검증 시 용도가 일치하는지 확인한다.
 */
public enum TokenType {
    ACCESS,
    REFRESH
}
