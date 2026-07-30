package com.example.ocare.auth.token;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 테스트용 인메모리 리프레시 토큰 저장소.
 *
 * <p>저장소 자체가 검증 대상이 아닌 테스트(로그인 흐름, 인증 필터 등)에서 Redis 없이
 * 실행할 수 있도록 둔다. 운영 구현은 Redis 를 사용한다.
 *
 * <p>TTL 을 실제로 흉내 내는 이유는, 만료된 토큰이 통과하지 않는지까지 테스트에서
 * 확인할 수 있어야 하기 때문이다.
 */
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<Long, StoredToken> store = new ConcurrentHashMap<>();

    @Override
    public void save(Long memberId, String refreshToken, Duration ttl) {
        store.put(memberId, new StoredToken(refreshToken, Instant.now().plus(ttl)));
    }

    @Override
    public boolean matches(Long memberId, String refreshToken) {
        StoredToken stored = store.get(memberId);
        if (stored == null) {
            return false;
        }
        if (stored.isExpired()) {
            store.remove(memberId);
            return false;
        }
        return stored.value().equals(refreshToken);
    }

    @Override
    public void delete(Long memberId) {
        store.remove(memberId);
    }

    /**
     * 테스트에서 저장소를 초기화할 때 사용한다.
     */
    public void clear() {
        store.clear();
    }

    private record StoredToken(String value, Instant expiresAt) {

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
