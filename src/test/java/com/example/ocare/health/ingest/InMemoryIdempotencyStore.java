package com.example.ocare.health.ingest;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 테스트용 인메모리 멱등성 저장소.
 *
 * <p>수집 로직 자체를 검증할 때 Redis 없이 실행하기 위한 구현이다.
 */
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Map<String, Instant> marked = new ConcurrentHashMap<>();

    @Override
    public Set<String> markIfAbsent(Collection<String> keys, Duration ttl) {
        Instant expiresAt = Instant.now().plus(ttl);
        Set<String> newlyMarked = new LinkedHashSet<>();

        for (String key : keys) {
            Instant existing = marked.get(key);
            if (existing != null && existing.isAfter(Instant.now())) {
                continue;
            }
            marked.put(key, expiresAt);
            newlyMarked.add(key);
        }
        return newlyMarked;
    }

    @Override
    public void release(Collection<String> keys) {
        keys.forEach(marked::remove);
    }

    public void clear() {
        marked.clear();
    }
}
