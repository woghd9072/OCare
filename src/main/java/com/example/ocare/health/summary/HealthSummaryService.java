package com.example.ocare.health.summary;

import com.example.ocare.health.query.HealthSummaryCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final HealthSummaryCache summaryCache;

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

        invalidateCacheAfterCommit(recordKey);

        log.info("집계 갱신: recordKey={}, 일자={}건, 월={}건 (영향 행 {}·{})",
                recordKey, affectedDates.size(), affectedMonths.size(), dailyRows, monthlyRows);
    }

    /**
     * 캐시 무효화를 트랜잭션 커밋 이후로 미룬다.
     *
     * <p>커밋 전에 무효화하면 다음 순서로 옛 값이 새 버전에 눌러앉는다.
     * <ol>
     *   <li>수집 트랜잭션이 캐시 버전을 올린다 (아직 커밋 전)</li>
     *   <li>다른 요청이 새 버전으로 조회한다. 아직 커밋되지 않았으므로 DB 에서 <b>옛 값</b>을 읽는다</li>
     *   <li>그 옛 값이 새 버전 키로 캐시된다</li>
     *   <li>수집이 커밋된다. 하지만 캐시에는 이미 옛 값이 새 버전으로 들어가 있다</li>
     * </ol>
     * 이렇게 되면 다음 수집이 일어날 때까지 사용자는 갱신 전 걸음수를 보게 된다.
     * 커밋 이후에 무효화하면 새 버전으로 처음 조회하는 시점에 이미 새 값이 보인다.
     *
     * <p>트랜잭션 밖에서 호출된 경우에는 미룰 곳이 없으므로 즉시 무효화한다.
     */
    private void invalidateCacheAfterCommit(String recordKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            summaryCache.invalidate(recordKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                summaryCache.invalidate(recordKey);
            }
        });
    }
}
