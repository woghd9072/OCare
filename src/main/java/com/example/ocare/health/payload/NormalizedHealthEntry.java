package com.example.ocare.health.payload;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 저장할 수 있는 형태로 정규화된 측정 구간.
 *
 * <p>이 시점부터는 출처가 삼성인지 애플인지 신경 쓸 필요가 없다.
 * 표기 차이는 모두 흡수되었고, 값은 UTC 시각 / KST 기준일 / km / kcal 로 통일되어 있다.
 *
 * @param startAtUtc   측정 시작 시각(UTC)
 * @param endAtUtc     측정 종료 시각(UTC). 삼성 데이터에는 시작과 같은 0초 구간이 존재한다
 * @param sourceOffset 원본에 표기된 오프셋. 없던 경우 null 이며 KST 로 추정했음을 뜻한다
 * @param measuredDate 집계 기준일(KST)
 * @param steps        걸음수. 애플은 소수가 들어오므로 반올림하지 않고 그대로 보존한다
 * @param caloriesKcal 소모 칼로리. 애플은 항상 0 이다
 * @param distanceKm   이동거리
 */
public record NormalizedHealthEntry(
        Instant startAtUtc,
        Instant endAtUtc,
        String sourceOffset,
        LocalDate measuredDate,
        BigDecimal steps,
        BigDecimal caloriesKcal,
        BigDecimal distanceKm
) {
}
