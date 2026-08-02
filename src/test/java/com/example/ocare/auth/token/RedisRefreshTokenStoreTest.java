package com.example.ocare.auth.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 리프레시 토큰 저장소 테스트.
 *
 * <p>실제 Redis(테스트용 DB index 1)에 붙어 동작한다. TTL 만료나 키 삭제는 Redis 의
 * 동작에 의존하므로 흉내 내면 검증의 의미가 없다.
 */
@SpringBootTest
@ActiveProfiles("test")
class RedisRefreshTokenStoreTest {

    private static final Long MEMBER_ID = 1L;
    private static final Duration TTL = Duration.ofMinutes(10);

    @Autowired
    private RedisRefreshTokenStore store;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        store.delete(MEMBER_ID);
    }

    @Test
    @DisplayName("저장한 토큰은 같은 값으로만 일치 판정된다")
    void saveAndMatch() {
        store.save(MEMBER_ID, "refresh-token-value", TTL);

        assertThat(store.matches(MEMBER_ID, "refresh-token-value")).isTrue();
        assertThat(store.matches(MEMBER_ID, "other-token-value")).isFalse();
    }

    @Test
    @DisplayName("저장된 값이 없으면 일치하지 않는다")
    void matchWithoutSavedToken() {
        assertThat(store.matches(MEMBER_ID, "refresh-token-value")).isFalse();
    }

    @Test
    @DisplayName("재발급으로 새 토큰을 저장하면 이전 토큰은 무효가 된다")
    void rotation() {
        store.save(MEMBER_ID, "old-token", TTL);
        store.save(MEMBER_ID, "new-token", TTL);

        assertThat(store.matches(MEMBER_ID, "old-token")).isFalse();
        assertThat(store.matches(MEMBER_ID, "new-token")).isTrue();
    }

    @Test
    @DisplayName("삭제하면 더 이상 일치하지 않는다")
    void delete() {
        store.save(MEMBER_ID, "refresh-token-value", TTL);

        store.delete(MEMBER_ID);

        assertThat(store.matches(MEMBER_ID, "refresh-token-value")).isFalse();
    }

    @Test
    @DisplayName("TTL 이 지나면 저장된 토큰이 사라진다")
    void expiresAfterTtl() throws InterruptedException {
        store.save(MEMBER_ID, "refresh-token-value", Duration.ofMillis(300));

        Thread.sleep(600);

        assertThat(store.matches(MEMBER_ID, "refresh-token-value")).isFalse();
    }

    @Test
    @DisplayName("토큰 원문이 아니라 해시가 저장된다")
    void storesHashNotRawToken() {
        String rawToken = "refresh-token-value";
        store.save(MEMBER_ID, rawToken, TTL);

        String stored = redisTemplate.opsForValue().get("auth:refresh:" + MEMBER_ID);

        assertThat(stored).isNotNull();
        assertThat(stored).isNotEqualTo(rawToken);
        // SHA-256 을 16진수로 표현한 길이
        assertThat(stored).hasSize(64);
    }
}
