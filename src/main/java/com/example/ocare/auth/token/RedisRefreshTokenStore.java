package com.example.ocare.auth.token;

import com.example.ocare.common.util.Sha256;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

/**
 * Redis 기반 리프레시 토큰 저장소.
 *
 * <p>Redis 를 통해 TTL 을 지정하면 만료된 토큰이 저절로 사라져 별도 정리 작업이 필요 없고,
 * 서버 인스턴스가 늘어나도 토큰 상태를 공유할 수 있다.
 * 반면 RDB 에 두면 만료 데이터를 주기적으로 지우는 배치가 따로 필요하다.
 *
 * <p>토큰 원문이 아니라 해시를 저장한다. 저장소가 노출되더라도 그 값만으로는
 * 재발급 요청을 만들 수 없게 하기 위함이다. 비교는 해시끼리 수행하므로 동작에는 차이가 없다.
 */
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(Long memberId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(key(memberId), Sha256.hex(refreshToken), ttl);
    }

    @Override
    public boolean matches(Long memberId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(key(memberId));
        if (stored == null) {
            return false;
        }
        // 문자열 비교 시간이 값에 따라 달라지지 않도록 상수 시간 비교를 사용한다.
        return MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                Sha256.hex(refreshToken).getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public void delete(Long memberId) {
        redisTemplate.delete(key(memberId));
    }

    private String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }
}
