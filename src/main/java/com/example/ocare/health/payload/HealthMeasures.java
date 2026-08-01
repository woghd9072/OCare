package com.example.ocare.health.payload;

import com.example.ocare.common.exception.BusinessException;
import com.example.ocare.common.exception.ErrorCode;
import com.example.ocare.health.payload.HealthPayloadEntry.HealthPayloadMeasure;

import java.math.BigDecimal;

/**
 * 측정값의 단위를 확인하고 저장 단위로 맞춘다.
 */
public final class HealthMeasures {

    private HealthMeasures() {
    }

    /**
     * 이동거리를 km 로 반환한다.
     */
    public static BigDecimal distanceInKilometers(HealthPayloadMeasure distance) {
        return requireUnit(distance, HealthUnit.KILOMETER, "이동거리");
    }

    /**
     * 소모 칼로리를 kcal 로 반환한다.
     *
     * <p>애플 HealthKit 은 칼로리를 제공하지 않아 항상 0 이 들어온다. 0 도 유효한 값이므로
     * 그대로 저장하고, 값을 제공하지 않는 출처인지 여부는 집계 단계에서 별도로 표시한다.
     */
    public static BigDecimal caloriesInKilocalories(HealthPayloadMeasure calories) {
        return requireUnit(calories, HealthUnit.KILOCALORIE, "소모 칼로리");
    }

    private static BigDecimal requireUnit(HealthPayloadMeasure measure, HealthUnit expected, String fieldName) {
        if (measure == null || measure.value() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, fieldName + " 값이 비어 있습니다.");
        }
        if (!expected.matches(measure.unit())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "%s 단위가 올바르지 않습니다. 기대: %s, 실제: %s"
                            .formatted(fieldName, expected.payloadUnit(), measure.unit()));
        }
        return measure.value();
    }
}
