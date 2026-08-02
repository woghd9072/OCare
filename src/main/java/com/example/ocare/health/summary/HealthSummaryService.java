package com.example.ocare.health.summary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * 수집으로 값이 달라진 구간의 집계를 다시 계산한다.
 *
 * <p>전체를 다시 계산하지 않고 영향받은 날짜만 처리한다. 한 번의 수집이 한 달치 데이터를
 * 담고 있어도 실제로 값이 바뀌는 날은 그중 일부이고, 다른 레코드키의 집계는 전혀 영향받지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthSummaryService {

    private final HealthDailySummaryWriter dailyWriter;
    private final HealthMonthlySummaryWriter monthlyWriter;

    /**
     * 일별 집계를 먼저 갱신하고, 그 결과로 월별 집계를 갱신한다.
     *
     * <p>순서가 중요하다. 월별 집계는 일별 집계를 더해 만들기 때문에,
     * 반대로 실행하면 갱신 전 값으로 월 합계가 계산된다.
     *
     * <p>수집과 같은 트랜잭션에서 동작한다. 원본은 저장됐는데 집계만 옛 값으로 남으면
     * 조회 결과가 실제와 어긋나고, 그 상태를 밖에서 알아챌 방법이 없기 때문이다.
     */
    @Transactional
    public void refresh(String recordKey, Collection<LocalDate> affectedDates) {
        if (affectedDates.isEmpty()) {
            return;
        }

        int dailyRows = dailyWriter.upsert(recordKey, affectedDates);

        Set<YearMonth> affectedMonths = new TreeSet<>();
        affectedDates.forEach(date -> affectedMonths.add(YearMonth.from(date)));
        int monthlyRows = monthlyWriter.upsert(recordKey, affectedMonths);

        log.info("집계 갱신: recordKey={}, 일자={}건, 월={}건 (영향 행 {}·{})",
                recordKey, affectedDates.size(), affectedMonths.size(), dailyRows, monthlyRows);
    }
}
