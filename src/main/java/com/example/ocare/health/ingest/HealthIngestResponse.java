package com.example.ocare.health.ingest;

import java.time.LocalDate;
import java.util.List;

/**
 * 수집 결과.
 *
 * <p>건수를 나눠 알려주는 이유는, 단말이 "보낸 만큼 저장됐는지" 를 스스로 확인할 수 있어야 하기 때문이다.
 * 저장 건수만 주면 재전송으로 걸러진 것인지 오류로 빠진 것인지 구분할 수 없다.
 *
 * @param uploadId      수집 이력 식별자
 * @param received      단말이 보낸 측정 구간 수
 * @param saved         새로 저장된 수
 * @param duplicated    이미 저장되어 있어 건너뛴 수
 * @param failed        형식 오류 등으로 저장하지 못한 수
 * @param affectedDates 이번 수집으로 값이 달라진 집계 기준일 목록
 */
public record HealthIngestResponse(
        Long uploadId,
        int received,
        int saved,
        int duplicated,
        int failed,
        List<LocalDate> affectedDates
) {
}
