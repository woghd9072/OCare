package com.example.ocare.health.query;

import com.example.ocare.health.summary.entity.HealthMonthlySummary;

/**
 * 월별 조회 응답의 한 행.
 *
 * @param month             집계 기준월(KST), {@code YYYY-MM}
 * @param steps             총 걸음수. 일별 값을 그대로 더한 값이라 두 화면이 항상 맞아떨어진다
 * @param calories          총 소모 칼로리(kcal)
 * @param distance          총 이동거리(km)
 * @param activeDays        측정 데이터가 존재한 일수. 월 전체 일수와 다르다
 * @param entryCount        집계에 사용된 측정 구간 수
 * @param caloriesSupported 출처가 칼로리를 제공하는지 여부
 */
public record MonthlySummaryResponse(
        String month,
        long steps,
        double calories,
        double distance,
        int activeDays,
        int entryCount,
        boolean caloriesSupported
) {

    public static MonthlySummaryResponse from(HealthMonthlySummary summary) {
        return new MonthlySummaryResponse(
                summary.getSummaryMonth(),
                summary.getSteps(),
                summary.getCalories().doubleValue(),
                summary.getDistanceKm().doubleValue(),
                summary.getActiveDays(),
                summary.getEntryCount(),
                summary.isCaloriesSupported()
        );
    }
}
