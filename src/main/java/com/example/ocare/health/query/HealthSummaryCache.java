package com.example.ocare.health.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * 조회 결과 캐시.
 *
 * <p>집계 테이블 조회는 인덱스를 타지만, 같은 사용자가 앱을 열 때마다 같은 구간을 반복해서 묻는다.
 * 사용자와 단말이 늘어나면 이 반복이 그대로 DB 부하가 되므로 결과를 캐시한다.
 *
 * <h3>무효화를 삭제가 아니라 버전으로 하는 이유</h3>
 * 캐시 키에는 조회 구간이 들어가므로 한 레코드키에 대해 수많은 키가 생긴다
 * 수집이 일어났을 때 이 키들을 모두 지우려면 패턴 검색이 필요한데,
 * {@code KEYS} 는 전체를 훑어 Redis 를 멈추게 하고 {@code SCAN} 은 여러 번 왕복해야 하며
 * 그 사이 새로 만들어진 키를 놓친다. 하나라도 놓치면 사용자는 옛 걸음수를 계속 보게 된다.
 *
 * <p>대신 레코드키마다 버전을 두고 캐시 키에 포함시킨다. 수집 시 버전을 1 올리면
 * 이전 키들은 더 이상 조회되지 않고 TTL 이 지나 저절로 사라진다.
 * 무효화가 명령 한 번으로 끝나고, 지우다 놓치는 경우가 구조적으로 없다.
 *
 * <p>버전 키에는 만료를 두지 않는다. 버전이 사라져 0 으로 돌아가면 아직 남아 있던
 * 옛 버전의 캐시가 다시 보이게 되기 때문이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthSummaryCache {

    private static final String VERSION_KEY_PREFIX = "health:summary:ver:";
    private static final String DAILY_KEY_PREFIX = "health:summary:daily:";
    private static final String MONTHLY_KEY_PREFIX = "health:summary:monthly:";

    /**
     * 캐시 보관 기간.
     */
    private static final Duration TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<DailySummaryResult> getDaily(String recordKey, String from, String to) {
        return get(dailyKey(recordKey, from, to), DailySummaryResult.class);
    }

    public void putDaily(String recordKey, String from, String to, DailySummaryResult result) {
        put(dailyKey(recordKey, from, to), result);
    }

    public Optional<MonthlySummaryResult> getMonthly(String recordKey, String from, String to) {
        return get(monthlyKey(recordKey, from, to), MonthlySummaryResult.class);
    }

    public void putMonthly(String recordKey, String from, String to, MonthlySummaryResult result) {
        put(monthlyKey(recordKey, from, to), result);
    }

    /**
     * 해당 레코드키의 캐시를 모두 무효화한다. 수집으로 집계가 바뀐 뒤 호출한다.
     */
    public void invalidate(String recordKey) {
        Long version = redisTemplate.opsForValue().increment(VERSION_KEY_PREFIX + recordKey);
        log.debug("조회 캐시 무효화: recordKey={}, version={}", recordKey, version);
    }

    private <T> Optional<T> get(String key, Class<T> type) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            return cached == null ? Optional.empty() : Optional.of(objectMapper.readValue(cached, type));
        } catch (RuntimeException e) {
            // 캐시는 없어도 동작해야 한다. 역직렬화 실패나 Redis 장애로 조회 자체가 실패하면 안 된다.
            log.warn("조회 캐시 읽기 실패, DB 로 조회한다: key={}, message={}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private void put(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), TTL);
        } catch (RuntimeException e) {
            log.warn("조회 캐시 쓰기 실패: key={}, message={}", key, e.getMessage());
        }
    }

    private String dailyKey(String recordKey, String from, String to) {
        return DAILY_KEY_PREFIX + recordKey + ":v" + currentVersion(recordKey) + ":" + from + ":" + to;
    }

    private String monthlyKey(String recordKey, String from, String to) {
        return MONTHLY_KEY_PREFIX + recordKey + ":v" + currentVersion(recordKey) + ":" + from + ":" + to;
    }

    /**
     * 레코드키의 현재 캐시 버전. 한 번도 수집되지 않았으면 0 이다.
     */
    private long currentVersion(String recordKey) {
        String version = redisTemplate.opsForValue().get(VERSION_KEY_PREFIX + recordKey);
        return version == null ? 0L : Long.parseLong(version);
    }
}
