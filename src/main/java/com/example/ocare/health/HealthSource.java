package com.example.ocare.health;

import java.util.Arrays;

/**
 * 건강활동 데이터의 출처.
 *
 * <p>입력 데이터의 {@code data.source.name} 값과 대응한다. 단말이 보내는 문자열이
 * {@code "SamsungHealth"}, {@code "Health Kit"} 처럼 표기가 제각각이라
 * 코드에서는 열거형으로 다루고, 원본 문자열은 {@link #payloadName} 으로 보관한다.
 *
 * <p>출처를 구분해야 하는 이유는 데이터 형식이 서로 다르기 때문이다.
 * 삼성헬스는 오프셋 없는 로컬 시각과 정수 걸음수를 보내고,
 * 애플 HealthKit 은 UTC 시각과 소수를 담은 문자열 걸음수를 보내며 칼로리를 제공하지 않는다.
 */
public enum HealthSource {

    /**
     * 삼성헬스(Android). 입력 데이터의 source.mode 값은 9 로 관측된다.
     */
    SAMSUNG_HEALTH("SamsungHealth"),

    /**
     * 애플 건강(HealthKit, iPhone). 입력 데이터의 source.mode 값은 10 으로 관측된다.
     */
    APPLE_HEALTH("Health Kit");

    private final String payloadName;

    HealthSource(String payloadName) {
        this.payloadName = payloadName;
    }

    public String payloadName() {
        return payloadName;
    }

    /**
     * 단말이 보낸 출처 문자열을 열거형으로 변환한다.
     *
     * <p>표기 흔들림(대소문자, 공백)을 흡수한다. 실제 데이터에서 {@code "Health Kit"} 처럼
     * 공백이 들어간 값이 확인되어, 공백을 제거한 뒤 비교한다.
     *
     * @throws IllegalArgumentException 알 수 없는 출처인 경우
     */
    public static HealthSource fromPayloadName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("데이터 출처가 지정되지 않았습니다.");
        }
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(source -> normalize(source.payloadName).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 데이터 출처입니다: " + value));
    }

    private static String normalize(String value) {
        return value.replace(" ", "").toLowerCase();
    }
}
