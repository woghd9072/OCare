package com.example.ocare.health.payload;

import com.example.ocare.health.HealthSource;

import java.time.Instant;
import java.util.List;

/**
 * 정규화된 수집 페이로드.
 *
 * @param recordKey    사용자 구분 키
 * @param dataType     측정 항목 (steps)
 * @param source       판별된 출처
 * @param sourceMode   원본 source.mode 값. 판별에는 쓰지 않고 수집 이력에 기록만 한다
 * @param productName  단말 제품명
 * @param productVendor 단말 제조사
 * @param memo         단말이 남긴 메모
 * @param lastUpdateAt 단말의 마지막 갱신 시각(UTC). 원본에 없으면 null
 * @param entries      정규화된 측정 구간 목록
 */
public record NormalizedHealthPayload(
        String recordKey,
        String dataType,
        HealthSource source,
        Integer sourceMode,
        String productName,
        String productVendor,
        String memo,
        Instant lastUpdateAt,
        List<NormalizedHealthEntry> entries
) {

    public int entryCount() {
        return entries.size();
    }
}
