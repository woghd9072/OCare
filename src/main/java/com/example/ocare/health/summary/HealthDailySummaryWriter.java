package com.example.ocare.health.summary;

import com.example.ocare.health.HealthSource;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 일별 집계를 다시 계산해 저장한다.
 *
 * <p>원본을 매번 합산하지 않고 집계 테이블을 두는 이유는, 조회는 자주 일어나는 반면
 * 원본은 한 사람당 한 달에 수천 건씩 쌓이기 때문이다.
 */
@Repository
@RequiredArgsConstructor
public class HealthDailySummaryWriter {

    /**
     * 대상 날짜의 원본을 다시 합산해 집계 행을 만들거나 갱신한다.
     *
     * <p>{@code steps} 는 합산을 모두 끝낸 뒤 한 번만 반올림한다.
     * 구간별로 반올림한 값을 더하면 원본 합계와 어긋난다.
     *
     * <p>{@code calories_supported} 는 출처로 판단한다. 애플 HealthKit 은 칼로리를 제공하지 않아
     * 항상 0 이 들어오는데, 합계가 0 인지로 판단하면 "온종일 움직이지 않은 날" 과 구분되지 않는다.
     */
    private static final String UPSERT_SQL = """
            INSERT INTO health_daily_summaries
                (record_key, summary_date, steps, calories, distance_km, entry_count,
                 calories_supported, last_aggregated_at, created_at, updated_at)
            SELECT s.record_key, s.summary_date, s.steps, s.calories, s.distance_km, s.entry_count,
                   s.calories_supported, NOW(6), NOW(6), NOW(6)
            FROM (SELECT r.record_key                                          AS record_key,
                         r.measured_date                                       AS summary_date,
                         ROUND(SUM(r.steps))                                   AS steps,
                         SUM(r.calories)                                       AS calories,
                         SUM(r.distance_km)                                    AS distance_km,
                         COUNT(*)                                              AS entry_count,
                         MIN(CASE WHEN r.source_name = ? THEN 0 ELSE 1 END)    AS calories_supported
                  FROM health_records r
                  WHERE r.record_key = ?
                    AND r.measured_date IN (%s)
                  GROUP BY r.record_key, r.measured_date) AS s
            ON DUPLICATE KEY UPDATE
                steps = s.steps,
                calories = s.calories,
                distance_km = s.distance_km,
                entry_count = s.entry_count,
                calories_supported = s.calories_supported,
                last_aggregated_at = NOW(6),
                updated_at = NOW(6)
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param dates 이번 수집으로 값이 달라진 날짜들. 전체를 다시 계산하지 않고 이 날짜만 갱신한다
     * @return 새로 만들어지거나 갱신된 집계 행 수
     */
    public int upsert(String recordKey, Collection<LocalDate> dates) {
        if (dates.isEmpty()) {
            return 0;
        }
        List<LocalDate> targets = List.copyOf(dates);
        String placeholders = String.join(", ", java.util.Collections.nCopies(targets.size(), "?"));

        List<Object> params = new ArrayList<>();
        params.add(HealthSource.APPLE_HEALTH.payloadName());
        params.add(recordKey);
        params.addAll(targets);

        return jdbcTemplate.update(UPSERT_SQL.formatted(placeholders), params.toArray());
    }
}
