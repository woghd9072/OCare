package com.example.ocare.health.payload;

import com.example.ocare.common.exception.BusinessException;
import com.example.ocare.common.exception.ErrorCode;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 단말이 보낸 시각 문자열을 UTC 로 정규화한다.
 *
 * <p>출처마다 표기가 다르다는 점이 이 클래스가 존재하는 이유다.
 * <pre>
 * 삼성 period     : "2024-11-15 00:00:00"        (오프셋 없음)
 * 애플 period     : "2024-11-14T21:20:00+0000"   (ISO8601, UTC)
 * 공통 lastUpdate : "2024-12-16 14:40:00 +0000"  (공백 + 오프셋)
 * </pre>
 */
public final class HealthTimeParser {

    /**
     * 오프셋이 없는 시각에 적용할 기기 타임존.
     */
    public static final ZoneId DEVICE_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 오프셋이 포함된 표기.
     *
     * <p>실제 입력 데이터에서 확인된 것은 {@code +0000} 형태(XX)뿐이지만,
     * {@code +00:00} 과 {@code Z}(XXX)도 함께 받는다. 셋은 ISO8601 이 허용하는 같은 의미의 표기라
     * 단말 SDK 가 바뀌면 언제든 다른 형태로 올 수 있고, 지원하지 않으면 데이터가 통째로 거부된다.
     */
    private static final List<DateTimeFormatter> OFFSET_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX")
    );

    /**
     * 오프셋이 없는 표기.
     */
    private static final List<DateTimeFormatter> LOCAL_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    );

    private HealthTimeParser() {
    }

    /**
     * 시각 문자열을 해석한다.
     *
     * @throws BusinessException 어떤 표기로도 해석할 수 없는 경우
     */
    public static ParsedTime parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "측정 시각이 비어 있습니다.");
        }
        String trimmed = value.trim();

        for (DateTimeFormatter formatter : OFFSET_FORMATS) {
            try {
                OffsetDateTime parsed = OffsetDateTime.parse(trimmed, formatter);
                return new ParsedTime(parsed.toInstant(), formatOffset(parsed));
            } catch (DateTimeParseException ignored) {
                // 다음 표기로 넘어간다
            }
        }

        for (DateTimeFormatter formatter : LOCAL_FORMATS) {
            try {
                LocalDateTime parsed = LocalDateTime.parse(trimmed, formatter);
                return new ParsedTime(parsed.atZone(DEVICE_ZONE).toInstant(), null);
            } catch (DateTimeParseException ignored) {
                // 다음 표기로 넘어간다
            }
        }

        throw new BusinessException(ErrorCode.INVALID_REQUEST, "해석할 수 없는 시각 표기입니다: " + value);
    }

    /**
     * 오프셋을 원본과 같은 {@code +0000} 형태로 되돌린다.
     */
    private static String formatOffset(OffsetDateTime dateTime) {
        int totalSeconds = dateTime.getOffset().getTotalSeconds();
        String sign = totalSeconds < 0 ? "-" : "+";
        int absoluteSeconds = Math.abs(totalSeconds);
        return String.format("%s%02d%02d", sign, absoluteSeconds / 3600, (absoluteSeconds % 3600) / 60);
    }
}
