package com.example.ocare.health.payload;

import com.example.ocare.common.exception.BusinessException;
import com.example.ocare.health.payload.HealthPayloadEntry.HealthPayloadMeasure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HealthMeasuresTest {

    @Test
    @DisplayName("km 단위 이동거리는 값을 그대로 사용한다")
    void distanceInKilometers() {
        BigDecimal value = HealthMeasures.distanceInKilometers(measure("0.550840787688434", "km"));

        assertThat(value).isEqualByComparingTo("0.550840787688434");
    }

    @Test
    @DisplayName("kcal 단위 칼로리는 값을 그대로 사용한다")
    void caloriesInKilocalories() {
        assertThat(HealthMeasures.caloriesInKilocalories(measure("2.03", "kcal")))
                .isEqualByComparingTo("2.03");
    }

    @Test
    @DisplayName("애플이 보내는 0 칼로리도 유효한 값으로 받아들인다")
    void zeroCaloriesIsValid() {
        assertThat(HealthMeasures.caloriesInKilocalories(measure("0", "kcal")))
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("단위 표기의 대소문자와 공백은 무시한다")
    void unitNotationIsLenient() {
        assertThat(HealthMeasures.distanceInKilometers(measure("1.5", "KM")))
                .isEqualByComparingTo("1.5");
        assertThat(HealthMeasures.distanceInKilometers(measure("1.5", " km ")))
                .isEqualByComparingTo("1.5");
    }

    @Test
    @DisplayName("다른 단위로 온 거리는 변환하지 않고 거부한다")
    void rejectUnexpectedDistanceUnit() {
        // m 로 온 값을 km 로 간주하면 1000배 차이가 나는데, 걸음수와 함께 보면
        // 이상해 보이지 않아 한참 뒤에나 발견된다. 그래서 그 자리에서 거부한다.
        assertThatThrownBy(() -> HealthMeasures.distanceInKilometers(measure("550.84", "m")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이동거리 단위가 올바르지 않습니다")
                .hasMessageContaining("기대: km")
                .hasMessageContaining("실제: m");
    }

    @Test
    @DisplayName("다른 단위로 온 칼로리는 거부한다")
    void rejectUnexpectedCalorieUnit() {
        assertThatThrownBy(() -> HealthMeasures.caloriesInKilocalories(measure("8500", "cal")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("소모 칼로리 단위가 올바르지 않습니다");
    }

    @Test
    @DisplayName("단위가 없으면 거부한다")
    void rejectMissingUnit() {
        assertThatThrownBy(() -> HealthMeasures.distanceInKilometers(measure("1.5", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("단위가 올바르지 않습니다");
    }

    @Test
    @DisplayName("값이 없으면 거부한다")
    void rejectMissingValue() {
        assertThatThrownBy(() -> HealthMeasures.distanceInKilometers(new HealthPayloadMeasure(null, "km")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이동거리 값이 비어 있습니다");

        assertThatThrownBy(() -> HealthMeasures.caloriesInKilocalories(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("소모 칼로리 값이 비어 있습니다");
    }

    private HealthPayloadMeasure measure(String value, String unit) {
        return new HealthPayloadMeasure(new BigDecimal(value), unit);
    }
}
