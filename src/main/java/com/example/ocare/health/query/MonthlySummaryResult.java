package com.example.ocare.health.query;

import java.util.List;

/**
 * 월별 조회 결과.
 *
 * @param recordKey 조회한 사용자 구분 키
 * @param from      조회 시작월 {@code YYYY-MM}
 * @param to        조회 종료월 {@code YYYY-MM}
 * @param months    데이터가 있는 달의 수
 * @param summaries 월별 집계. 측정 데이터가 없는 달은 포함되지 않는다
 */
public record MonthlySummaryResult(
        String recordKey,
        String from,
        String to,
        int months,
        List<MonthlySummaryResponse> summaries
) {

    public static MonthlySummaryResult of(String recordKey, String from, String to,
                                          List<MonthlySummaryResponse> summaries) {
        return new MonthlySummaryResult(recordKey, from, to, summaries.size(), summaries);
    }
}
