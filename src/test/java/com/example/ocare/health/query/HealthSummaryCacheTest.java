package com.example.ocare.health.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 조회 캐시를 실제 Redis 에 붙여 검증한다.
 *
 * <p>버전 기반 무효화가 의도대로 동작하는지가 핵심이다.
 * 무효화가 새는 순간 사용자는 옛 걸음수를 계속 보게 된다.
 */
@SpringBootTest
@ActiveProfiles("test")
class HealthSummaryCacheTest {

    private static final String RECORD_KEY = "cache-test-key";
    private static final String OTHER_KEY = "cache-other-key";

    @Autowired
    private HealthSummaryCache cache;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        Set<String> keys = redisTemplate.keys("health:summary:*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("저장한 일별 결과를 그대로 읽는다")
    void putAndGetDaily() {
        cache.putDaily(RECORD_KEY, "2024-11-15", "2024-11-17", dailyResult(7243));

        DailySummaryResult cached = cache.getDaily(RECORD_KEY, "2024-11-15", "2024-11-17").orElseThrow();

        assertThat(cached.recordKey()).isEqualTo(RECORD_KEY);
        assertThat(cached.days()).isEqualTo(1);
        assertThat(cached.summaries().get(0).steps()).isEqualTo(7243);
        assertThat(cached.summaries().get(0).date()).isEqualTo(LocalDate.of(2024, 11, 15));
        assertThat(cached.summaries().get(0).caloriesSupported()).isTrue();
    }

    @Test
    @DisplayName("저장한 적 없는 구간은 비어 있다")
    void getMissing() {
        assertThat(cache.getDaily(RECORD_KEY, "2024-11-15", "2024-11-17")).isEmpty();
    }

    @Test
    @DisplayName("조회 구간이 다르면 다른 항목으로 저장된다")
    void differentPeriodsAreSeparateEntries() {
        cache.putDaily(RECORD_KEY, "2024-11-15", "2024-11-17", dailyResult(7243));

        assertThat(cache.getDaily(RECORD_KEY, "2024-11-15", "2024-11-16")).isEmpty();
        assertThat(cache.getDaily(RECORD_KEY, "2024-11-15", "2024-11-17")).isPresent();
    }

    @Test
    @DisplayName("무효화하면 이전에 저장한 결과가 더 이상 조회되지 않는다")
    void invalidateHidesPreviousEntries() {
        cache.putDaily(RECORD_KEY, "2024-11-15", "2024-11-17", dailyResult(7243));
        cache.putMonthly(RECORD_KEY, "2024-11", "2024-12", monthlyResult(124783));

        cache.invalidate(RECORD_KEY);

        // 구간별로 지우지 않았는데도 일별·월별 모두 사라져야 한다
        assertThat(cache.getDaily(RECORD_KEY, "2024-11-15", "2024-11-17")).isEmpty();
        assertThat(cache.getMonthly(RECORD_KEY, "2024-11", "2024-12")).isEmpty();
    }

    @Test
    @DisplayName("무효화 후 저장한 결과는 정상적으로 조회된다")
    void writesAfterInvalidationAreVisible() {
        cache.putDaily(RECORD_KEY, "2024-11-15", "2024-11-17", dailyResult(7243));
        cache.invalidate(RECORD_KEY);

        cache.putDaily(RECORD_KEY, "2024-11-15", "2024-11-17", dailyResult(9999));

        assertThat(cache.getDaily(RECORD_KEY, "2024-11-15", "2024-11-17").orElseThrow()
                .summaries().get(0).steps()).isEqualTo(9999);
    }

    @Test
    @DisplayName("한 레코드키를 무효화해도 다른 레코드키는 영향받지 않는다")
    void invalidationIsScopedToRecordKey() {
        cache.putDaily(RECORD_KEY, "2024-11-15", "2024-11-17", dailyResult(7243));
        cache.putDaily(OTHER_KEY, "2024-11-15", "2024-11-17", dailyResult(12450));

        cache.invalidate(RECORD_KEY);

        assertThat(cache.getDaily(RECORD_KEY, "2024-11-15", "2024-11-17")).isEmpty();
        assertThat(cache.getDaily(OTHER_KEY, "2024-11-15", "2024-11-17")).isPresent();
    }

    @Test
    @DisplayName("무효화를 여러 번 해도 문제없다")
    void repeatedInvalidation() {
        cache.putDaily(RECORD_KEY, "2024-11-15", "2024-11-17", dailyResult(7243));

        cache.invalidate(RECORD_KEY);
        cache.invalidate(RECORD_KEY);
        cache.invalidate(RECORD_KEY);

        assertThat(cache.getDaily(RECORD_KEY, "2024-11-15", "2024-11-17")).isEmpty();
        assertThat(redisTemplate.opsForValue().get("health:summary:ver:" + RECORD_KEY)).isEqualTo("3");
    }

    @Test
    @DisplayName("버전 키에는 만료를 두지 않는다")
    void versionKeyHasNoExpiry() {
        cache.invalidate(RECORD_KEY);

        Long ttl = redisTemplate.getExpire("health:summary:ver:" + RECORD_KEY);

        // 버전이 사라져 0 으로 돌아가면, 아직 남아 있는 옛 버전 캐시가 되살아난다
        assertThat(ttl).isEqualTo(-1L);
    }

    @Test
    @DisplayName("깨진 값이 저장되어 있어도 조회가 실패하지 않는다")
    void corruptedValueFallsBackToEmpty() {
        cache.putDaily(RECORD_KEY, "2024-11-15", "2024-11-17", dailyResult(7243));
        Set<String> keys = redisTemplate.keys("health:summary:daily:*");
        keys.forEach(key -> redisTemplate.opsForValue().set(key, "{not-json"));

        // 캐시는 없어도 동작해야 한다. 조회 자체가 실패하면 캐시가 장애 지점이 된다.
        assertThat(cache.getDaily(RECORD_KEY, "2024-11-15", "2024-11-17")).isEmpty();
    }

    @Test
    @DisplayName("월별 결과도 그대로 읽는다")
    void putAndGetMonthly() {
        cache.putMonthly(RECORD_KEY, "2024-11", "2024-12", monthlyResult(124783));

        MonthlySummaryResult cached = cache.getMonthly(RECORD_KEY, "2024-11", "2024-12").orElseThrow();

        assertThat(cached.summaries().get(0).month()).isEqualTo("2024-11");
        assertThat(cached.summaries().get(0).steps()).isEqualTo(124783L);
        assertThat(cached.summaries().get(0).activeDays()).isEqualTo(16);
    }

    private DailySummaryResult dailyResult(int steps) {
        return DailySummaryResult.of(RECORD_KEY, LocalDate.of(2024, 11, 15), LocalDate.of(2024, 11, 17),
                List.of(new DailySummaryResponse(LocalDate.of(2024, 11, 15), steps, 289.21, 5.41949, 38, true)));
    }

    private MonthlySummaryResult monthlyResult(long steps) {
        return MonthlySummaryResult.of(RECORD_KEY, "2024-11", "2024-12",
                List.of(new MonthlySummaryResponse("2024-11", steps, 5002.5, 94.3421, 16, 551, true)));
    }
}
