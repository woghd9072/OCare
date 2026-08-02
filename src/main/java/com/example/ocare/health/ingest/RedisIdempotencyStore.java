package com.example.ocare.health.ingest;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.SetCondition;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Redis 기반 멱등성 저장소.
 *
 * <p>한 번에 최대 1,500건을 처리하므로 파이프라인으로 묶어 보낸다.
 * 건별로 왕복하면 네트워크 지연이 건수만큼 곱해진다.
 */
@Repository
@RequiredArgsConstructor
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String KEY_PREFIX = "health:dedup:";
    private static final byte[] MARKER = "1".getBytes(StandardCharsets.UTF_8);

    private final StringRedisTemplate redisTemplate;

    @Override
    public Set<String> markIfAbsent(Collection<String> keys, Duration ttl) {
        if (keys.isEmpty()) {
            return Set.of();
        }
        List<String> ordered = List.copyOf(keys);

        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : ordered) {
                // SET key value NX EX ttl : "없을 때만 쓰기" 를 한 번의 명령으로 처리한다.
                connection.stringCommands().set(
                        redisKey(key), MARKER, SetCondition.ifAbsent(), Expiration.from(ttl));
            }
            return null;
        });

        Set<String> newlyMarked = new LinkedHashSet<>();
        for (int i = 0; i < ordered.size(); i++) {
            // 표시에 성공한 키만 이번에 처음 본 것이다.
            if (Boolean.TRUE.equals(results.get(i))) {
                newlyMarked.add(ordered.get(i));
            }
        }
        return newlyMarked;
    }

    @Override
    public void release(Collection<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        redisTemplate.delete(keys.stream().map(key -> KEY_PREFIX + key).toList());
    }

    private byte[] redisKey(String key) {
        return (KEY_PREFIX + key).getBytes(StandardCharsets.UTF_8);
    }
}
