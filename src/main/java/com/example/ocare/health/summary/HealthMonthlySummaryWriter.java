package com.example.ocare.health.summary;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 월별 집계를 다시 계산해 저장한다.
 *
 * <p>원본이 아니라 일별 집계를 더한다. 원본에서 직접 합산하면 월 합계와
 * "일별 값을 눈으로 더한 값" 이 미세하게 달라질 수 있는데, 사용자는 두 화면을 함께 보므로
 * 그 차이를 오류로 받아들인다. 일별 값의 합으로 정의하면 두 화면이 항상 맞아떨어진다.
 */
@Repository
@RequiredArgsConstructor
public class HealthMonthlySummaryWriter {

    /**
     * {@code active_days} 는 집계 행이 존재하는 날의 수다.
     * 일별 집계는 측정 데이터가 있는 날에만 만들어지므로, 이 값이 곧 활동이 기록된 일수가 된다.
     * 월 전체 일수와 달라서 활동 밀도를 판단하는 근거가 된다.
     */
    private static final String UPSERT_SQL = """
            INSERT INTO health_monthly_summaries
                (record_key, summary_month, steps, calories, distance_km, active_days, entry_count,
                 calories_supported, last_aggregated_at, created_at, updated_at)
            SELECT s.record_key, s.summary_month, s.steps, s.calories, s.distance_km, s.active_days,
                   s.entry_count, s.calories_supported, NOW(6), NOW(6), NOW(6)
            FROM (SELECT d.record_key                             AS record_key,
                         DATE_FORMAT(d.summary_date, '%%Y-%%m')   AS summary_month,
                         SUM(d.steps)                             AS steps,
                         SUM(d.calories)                          AS calories,
                         SUM(d.distance_km)                       AS distance_km,
                         COUNT(*)                                 AS active_days,
                         SUM(d.entry_count)                       AS entry_count,
                         MIN(d.calories_supported)                AS calories_supported
                  FROM health_daily_summaries d
                  WHERE d.record_key = ?
                    AND DATE_FORMAT(d.summary_date, '%%Y-%%m') IN (%s)
                  GROUP BY d.record_key, DATE_FORMAT(d.summary_date, '%%Y-%%m')) AS s
            ON DUPLICATE KEY UPDATE
                steps = s.steps,
                calories = s.calories,
                distance_km = s.distance_km,
                active_days = s.active_days,
                entry_count = s.entry_count,
                calories_supported = s.calories_supported,
                last_aggregated_at = NOW(6),
                updated_at = NOW(6)
            """;

    private final JdbcTemplate jdbcTemplate;

    public int upsert(String recordKey, Collection<YearMonth> months) {
        if (months.isEmpty()) {
            return 0;
        }
        List<YearMonth> targets = List.copyOf(months);
        String placeholders = String.join(", ", Collections.nCopies(targets.size(), "?"));

        List<Object> params = new ArrayList<>();
        params.add(recordKey);
        targets.forEach(month -> params.add(month.toString()));

        return jdbcTemplate.update(UPSERT_SQL.formatted(placeholders), params.toArray());
    }
}
