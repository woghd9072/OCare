package com.example.ocare.health.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * 측정 구간 하나.
 */
public record HealthPayloadEntry(

        @NotNull(message = "측정 구간은 필수입니다.")
        @Valid
        HealthPayloadPeriod period,

        @NotNull(message = "걸음수는 필수입니다.")
        @PositiveOrZero(message = "걸음수는 0 이상이어야 합니다.")
        BigDecimal steps,

        @NotNull(message = "이동거리는 필수입니다.")
        @Valid
        HealthPayloadMeasure distance,

        @NotNull(message = "소모 칼로리는 필수입니다.")
        @Valid
        HealthPayloadMeasure calories
) {

    /**
     * 측정 구간.
     *
     * <p>포맷이 출처마다 다르다.
     * 삼성은 {@code "2024-11-15 00:00:00"} 처럼 오프셋이 없고, 애플은
     * {@code "2024-11-14T21:20:00+0000"} 처럼 ISO8601 UTC 로 보낸다.
     *
     * <p>삼성 데이터에는 {@code from} 과 {@code to} 가 같은 0초 구간이 존재하며
     * 값도 정상이므로 버리면 안 된다.
     */
    public record HealthPayloadPeriod(
            @NotNull(message = "측정 시작 시각은 필수입니다.") String from,
            @NotNull(message = "측정 종료 시각은 필수입니다.") String to
    ) {
    }

    /**
     * 값과 단위를 함께 담는 측정치.
     *
     * @param value 측정값. 애플의 칼로리는 항상 0 이다
     * @param unit  단위. 실제 데이터에서는 거리 km, 칼로리 kcal 만 확인된다.
     *              단위를 무시하고 값만 쓰면 m 로 보내는 단말이 생겼을 때 1000배 오차가 발생하므로 함께 받는다
     */
    public record HealthPayloadMeasure(
            @NotNull(message = "측정값은 필수입니다.")
            @PositiveOrZero(message = "측정값은 0 이상이어야 합니다.")
            BigDecimal value,

            @NotNull(message = "측정 단위는 필수입니다.")
            String unit
    ) {
    }
}
