package com.example.ocare.health.query;

import com.example.ocare.health.summary.entity.HealthDailySummary;

import java.time.LocalDate;

/**
 * 일별 조회 응답의 한 행.
 *
 * @param date              집계 기준일(KST)
 * @param steps             총 걸음수
 * @param calories          총 소모 칼로리(kcal). {@code caloriesSupported} 가 false 면 의미가 없다
 * @param distance          총 이동거리(km)
 * @param entryCount        집계에 사용된 측정 구간 수
 * @param caloriesSupported 출처가 칼로리를 제공하는지 여부.
 */
public record DailySummaryResponse(
        LocalDate date,
        int steps,
        double calories,
        double distance,
        int entryCount,
        boolean caloriesSupported
) {

    /**
     * 저장은 DECIMAL 로, 응답은 실수로 내려준다.
     * 과제의 필드 정의(calories float, distance float)에 맞추기 위함이며,
     * 누적 오차가 문제되는 구간은 이미 지나온 뒤라 여기서 변환해도 값이 달라지지 않는다.
     */
    public static DailySummaryResponse from(HealthDailySummary summary) {
        return new DailySummaryResponse(
                summary.getSummaryDate(),
                summary.getSteps(),
                summary.getCalories().doubleValue(),
                summary.getDistanceKm().doubleValue(),
                summary.getEntryCount(),
                summary.isCaloriesSupported()
        );
    }
}
