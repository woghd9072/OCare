package com.example.ocare.health.payload;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 단말이 보낸 시각을 해석한 결과.
 *
 * @param instant      UTC 로 정규화한 시각. 저장과 비교는 항상 이 값을 기준으로 한다
 * @param sourceOffset 원본에 표기되어 있던 UTC 오프셋(예: {@code +0000}).
 *                     오프셋이 없던 데이터는 {@code null} 이다.
 */
public record ParsedTime(Instant instant, String sourceOffset) {

    public boolean hasExplicitOffset() {
        return sourceOffset != null;
    }

    /**
     * 이 시각이 속하는 집계 기준일(KST).
     */
    public LocalDate measuredDate() {
        return HealthTimeParser.measuredDate(instant);
    }
}
