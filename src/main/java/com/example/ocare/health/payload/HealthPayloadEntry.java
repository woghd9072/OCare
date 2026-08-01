package com.example.ocare.health.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 측정 구간 하나.
 *
 * <p>{@code steps} 를 {@link String} 으로 받는 이유는 출처마다 JSON 타입이 다르기 때문이다.
 * 삼성은 숫자 {@code 54}, 애플은 문자열 {@code "688.5509846105425"} 로 보낸다.
 * 둘 다 받으려면 별도 역직렬화가 필요하며, 이는 다음 단계에서 처리한다.
 */
public record HealthPayloadEntry(

        @NotNull(message = "측정 구간은 필수입니다.")
        @Valid
        HealthPayloadPeriod period,

        String steps,

        @Valid
        HealthPayloadMeasure distance,

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
     * @param unit  단위. 실제 데이터에서는 거리 km, 칼로리 kcal 만 확인된다
     */
    public record HealthPayloadMeasure(Double value, String unit) {
    }
}
