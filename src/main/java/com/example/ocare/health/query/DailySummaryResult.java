package com.example.ocare.health.query;

import java.time.LocalDate;
import java.util.List;

/**
 * 일별 조회 결과.
 *
 * @param recordKey 조회한 사용자 구분 키
 * @param from      조회 시작일
 * @param to        조회 종료일
 * @param days      데이터가 있는 날의 수
 * @param summaries 일자별 집계. 측정 데이터가 없는 날은 포함되지 않는다
 */
public record DailySummaryResult(
        String recordKey,
        LocalDate from,
        LocalDate to,
        int days,
        List<DailySummaryResponse> summaries
) {

    public static DailySummaryResult of(String recordKey, LocalDate from, LocalDate to,
                                        List<DailySummaryResponse> summaries) {
        return new DailySummaryResult(recordKey, from, to, summaries.size(), summaries);
    }
}
