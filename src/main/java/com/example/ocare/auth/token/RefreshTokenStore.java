package com.example.ocare.auth.token;

import java.time.Duration;

/**
 * 리프레시 토큰 저장소.
 *
 * <p>JWT 는 서명만으로 검증되므로 서버가 발급 이후를 통제할 수 없다.
 * 로그아웃하거나 토큰이 탈취된 경우에도 유효기간이 끝날 때까지 계속 통과한다.
 * 그래서 서버에 보관하고, 재발급 시 보관된 값과 일치하는지 확인한다.
 * 보관된 값을 지우면 그 시점부터 재발급이 막히고 세션이 끊긴다.
 *
 * <p>회원당 하나만 보관한다. 재발급할 때마다 새 토큰으로 덮어쓰므로,
 * 이전 리프레시 토큰은 자동으로 무효가 된다(토큰 회전).
 *
 * <p>구현을 인터페이스 뒤에 두는 이유는 저장 수단이 테스트 편의에 따라 달라지기 때문이다.
 * 운영은 Redis 를 쓰고, 저장소 자체가 관심사가 아닌 테스트에서는 인메모리 구현으로 대체한다.
 */
public interface RefreshTokenStore {

    /**
     * 리프레시 토큰을 저장한다. 같은 회원의 기존 토큰은 덮어쓴다.
     *
     * @param ttl 보관 기간. 토큰 유효기간과 맞춰 두면 만료된 값이 저장소에 남지 않는다.
     */
    void save(Long memberId, String refreshToken, Duration ttl);

    /**
     * 저장된 토큰과 일치하는지 확인한다.
     *
     * <p>토큰 자체의 서명·만료 검증과는 별개다. 서명이 유효하더라도 저장소에 없으면
     * 이미 로그아웃했거나 회전으로 폐기된 토큰이므로 거부해야 한다.
     */
    boolean matches(Long memberId, String refreshToken);

    /**
     * 저장된 토큰을 삭제한다. 로그아웃 시 호출한다.
     */
    void delete(Long memberId);
}
