package com.example.ocare.auth.dto;

/**
 * 토큰 발급 응답.
 *
 * @param accessToken  API 호출 시 Authorization 헤더에 담을 토큰
 * @param refreshToken 액세스 토큰 만료 시 재발급에 사용하는 토큰
 * @param tokenType    인증 방식. 클라이언트가 헤더를 조립할 때 사용한다
 * @param expiresIn    액세스 토큰의 남은 유효 시간(초). 클라이언트가 만료 시점을 예측해
 *                     미리 재발급할 수 있도록 내려준다. 토큰을 직접 열어보게 하지 않기 위함이다.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {

    private static final String BEARER = "Bearer";

    public static TokenResponse of(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, refreshToken, BEARER, expiresInSeconds);
    }
}
