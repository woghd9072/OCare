package com.example.ocare.health.query;

import com.example.ocare.common.exception.BusinessException;
import com.example.ocare.common.exception.ErrorCode;
import com.example.ocare.health.summary.entity.HealthDailySummaryRepository;
import com.example.ocare.recordkey.service.RecordKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 수집된 활동 데이터를 레코드키 기준으로 조회한다.
 *
 * <p>원본이 아니라 집계 테이블을 읽는다. 한 달치 원본이 수천 건인 반면 집계는 날짜당 한 행이라,
 * 조회할 때마다 합산하면 같은 계산을 반복하게 된다.
 */
@Service
@RequiredArgsConstructor
public class HealthQueryService {

    /**
     * 한 번에 조회할 수 있는 최대 기간.
     *
     * <p>제한이 없으면 수 년치를 한 번에 요청해 응답이 지나치게 커질 수 있다.
     * 1년으로 두면 연간 조회까지는 한 번에 가능하다.
     */
    private static final long MAX_QUERY_DAYS = 366;

    private final RecordKeyService recordKeyService;
    private final HealthDailySummaryRepository dailySummaryRepository;

    /**
     * 일별 활동 집계를 조회한다.
     *
     * <p>레코드키 소유권을 먼저 확인한다. 조회 조건으로 받은 값을 그대로 믿으면
     * 남의 건강 데이터를 열람할 수 있기 때문이다.
     */
    @Transactional(readOnly = true)
    public DailySummaryResult findDaily(Long memberId, String recordKey, LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        recordKeyService.getOwnedRecordKey(memberId, recordKey);

        List<DailySummaryResponse> summaries = dailySummaryRepository
                .findAllByRecordKeyAndSummaryDateBetweenOrderBySummaryDateAsc(recordKey, from, to)
                .stream()
                .map(DailySummaryResponse::from)
                .toList();

        return DailySummaryResult.of(recordKey, from, to, summaries);
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "조회 시작일이 종료일보다 늦습니다.");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_QUERY_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "조회 기간은 최대 %d일까지 가능합니다. 요청: %d일".formatted(MAX_QUERY_DAYS, days));
        }
    }
}
