package com.example.ocare.health.ingest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Redis 에 붙어 멱등성 저장소를 검증한다.
 *
 * <p>원자적 표시와 TTL 만료는 Redis 의 동작에 의존하므로 흉내 내면 검증의 의미가 없다.
 */
@SpringBootTest
@ActiveProfiles("test")
class RedisIdempotencyStoreTest {

    private static final Duration TTL = Duration.ofMinutes(10);

    @Autowired
    private RedisIdempotencyStore store;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        Set<String> keys = redisTemplate.keys("health:dedup:*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("처음 보는 키만 표시되어 반환된다")
    void marksOnlyNewKeys() {
        Set<String> first = store.markIfAbsent(List.of("a", "b", "c"), TTL);
        assertThat(first).containsExactlyInAnyOrder("a", "b", "c");

        // 같은 요청이 다시 오면 아무것도 새로 표시되지 않는다
        Set<String> second = store.markIfAbsent(List.of("a", "b", "c"), TTL);
        assertThat(second).isEmpty();
    }

    @Test
    @DisplayName("이미 처리한 키와 새 키가 섞여 오면 새 키만 걸러낸다")
    void separatesNewKeysFromSeen() {
        store.markIfAbsent(List.of("a", "b"), TTL);

        Set<String> result = store.markIfAbsent(List.of("a", "b", "c", "d"), TTL);

        // 단말이 이전 구간까지 포함해 다시 보내는 상황이다. 새 구간만 저장하면 된다.
        assertThat(result).containsExactlyInAnyOrder("c", "d");
    }

    @Test
    @DisplayName("한 요청 안에 같은 키가 두 번 있으면 한 번만 새 키로 잡힌다")
    void duplicatedKeyWithinSingleRequest() {
        Set<String> result = store.markIfAbsent(List.of("a", "a", "b"), TTL);

        assertThat(result).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    @DisplayName("표시를 되돌리면 다시 새 키가 된다")
    void releaseAllowsRetry() {
        store.markIfAbsent(List.of("a", "b"), TTL);

        // 저장에 실패했을 때 표시를 되돌리지 않으면, 단말이 다시 보내도 건너뛰어 데이터가 유실된다.
        store.release(List.of("a", "b"));

        assertThat(store.markIfAbsent(List.of("a", "b"), TTL)).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    @DisplayName("TTL 이 지나면 표시가 사라진다")
    void expiresAfterTtl() throws InterruptedException {
        store.markIfAbsent(List.of("a"), Duration.ofMillis(300));

        Thread.sleep(600);

        assertThat(store.markIfAbsent(List.of("a"), TTL)).containsExactly("a");
    }

    @Test
    @DisplayName("실제 수집 규모(1,500건)를 한 번에 처리한다")
    void handlesRealisticBatchSize() {
        List<String> keys = IntStream.range(0, 1500).mapToObj(i -> "key-" + i).toList();

        Set<String> first = store.markIfAbsent(keys, TTL);
        Set<String> second = store.markIfAbsent(keys, TTL);

        assertThat(first).hasSize(1500);
        assertThat(second).isEmpty();
    }

    @Test
    @DisplayName("빈 목록은 Redis 를 호출하지 않고 빈 결과를 준다")
    void emptyInput() {
        assertThat(store.markIfAbsent(List.of(), TTL)).isEmpty();
        store.release(List.of());
    }
}
