package com.example.ocare.health.payload;

/**
 * 측정값의 단위.
 *
 * <p>실제 입력 데이터에서는 거리 {@code km}, 칼로리 {@code kcal} 만 확인된다.
 */
public enum HealthUnit {

    KILOMETER("km"),
    KILOCALORIE("kcal");

    private final String payloadUnit;

    HealthUnit(String payloadUnit) {
        this.payloadUnit = payloadUnit;
    }

    public String payloadUnit() {
        return payloadUnit;
    }

    /**
     * 단말이 보낸 단위 표기가 이 단위와 같은지 확인한다.
     */
    public boolean matches(String value) {
        return value != null && payloadUnit.equalsIgnoreCase(value.trim());
    }
}
