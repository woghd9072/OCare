package com.example.ocare.health.payload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 서로 다른 두 출처의 표기를 같은 DTO 로 받을 수 있는지 검증한다.
 *
 * <p>삼성헬스와 애플 HealthKit 은 같은 항목을 다른 JSON 타입으로 보낸다.
 * 이 차이를 수집 입구에서 흡수하지 못하면 한쪽 데이터가 통째로 거부된다.
 */
class HealthPayloadDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("삼성헬스의 숫자 표기 steps 를 받아들인다")
    void samsungEntry() {
        String json = """
                {"period":{"from":"2024-11-15 00:00:00","to":"2024-11-15 00:10:00"},
                 "distance":{"unit":"km","value":0.04223},
                 "calories":{"unit":"kcal","value":2.03},
                 "steps":54}
                """;

        HealthPayloadEntry entry = objectMapper.readValue(json, HealthPayloadEntry.class);

        assertThat(entry.steps()).isEqualByComparingTo("54");
        assertThat(entry.distance().value()).isEqualByComparingTo("0.04223");
        assertThat(entry.calories().value()).isEqualByComparingTo("2.03");
        assertThat(entry.period().from()).isEqualTo("2024-11-15 00:00:00");
    }

    @Test
    @DisplayName("애플 HealthKit 의 문자열 표기 steps 를 받아들인다")
    void appleEntry() {
        String json = """
                {"steps":"688.5509846105425",
                 "period":{"to":"2024-11-14T21:50:00+0000","from":"2024-11-14T21:40:00+0000"},
                 "distance":{"value":0.550840787688434,"unit":"km"},
                 "calories":{"value":0,"unit":"kcal"}}
                """;

        HealthPayloadEntry entry = objectMapper.readValue(json, HealthPayloadEntry.class);

        assertThat(entry.steps()).isEqualByComparingTo("688.5509846105425");
        assertThat(entry.calories().value()).isEqualByComparingTo("0");
        assertThat(entry.period().from()).isEqualTo("2024-11-14T21:40:00+0000");
    }

    @Test
    @DisplayName("JSON 에 적힌 10진수가 그대로 보존된다")
    void preservesDecimalLiteral() {
        String json = """
                {"steps":1,"period":{"from":"a","to":"b"},
                 "distance":{"value":0.550840787688434,"unit":"km"},
                 "calories":{"value":0,"unit":"kcal"}}
                """;

        HealthPayloadEntry entry = objectMapper.readValue(json, HealthPayloadEntry.class);

        // double 을 거치면 2진 부동소수로 근사되어 원문과 달라진다.
        // BigDecimal 로 직접 받으면 원문 그대로 유지된다.
        assertThat(entry.distance().value().toPlainString()).isEqualTo("0.550840787688434");
    }

    @Test
    @DisplayName("빈 문자열 steps 는 null 이 되므로 검증에서 걸러야 한다")
    void emptyStepsBecomesNull() {
        String json = """
                {"steps":"","period":{"from":"a","to":"b"},
                 "distance":{"value":0,"unit":"km"},
                 "calories":{"value":0,"unit":"kcal"}}
                """;

        HealthPayloadEntry entry = objectMapper.readValue(json, HealthPayloadEntry.class);

        // Jackson 은 빈 문자열을 예외 없이 null 로 바꾼다.
        // 이 동작 때문에 steps 에 @NotNull 이 필요하다. 없으면 걸음수 누락이 조용히 통과한다.
        assertThat(entry.steps()).isNull();
    }

    @Test
    @DisplayName("전체 페이로드 구조를 받아들인다")
    void fullPayload() {
        String json = """
                {
                  "recordkey": "7836887b-b12a-440f-af0f-851546504b13",
                  "type": "steps",
                  "lastUpdate": "2024-12-16 14:40:00 +0000",
                  "data": {
                    "source": {"mode": 9, "product": {"name": "Android", "vender": "Samsung"},
                               "name": "SamsungHealth", "type": ""},
                    "entries": [
                      {"period":{"from":"2024-11-15 00:00:00","to":"2024-11-15 00:10:00"},
                       "distance":{"unit":"km","value":0.04223},
                       "calories":{"unit":"kcal","value":2.03},"steps":54}
                    ]
                  }
                }
                """;

        HealthPayload payload = objectMapper.readValue(json, HealthPayload.class);

        assertThat(payload.recordkey()).isEqualTo("7836887b-b12a-440f-af0f-851546504b13");
        assertThat(payload.type()).isEqualTo("steps");
        assertThat(payload.data().source().name()).isEqualTo("SamsungHealth");
        assertThat(payload.data().source().productVendor()).isEqualTo("Samsung");
        assertThat(payload.data().entries()).hasSize(1);
        assertThat(payload.data().entries().get(0).steps()).isEqualByComparingTo(BigDecimal.valueOf(54));
    }
}
