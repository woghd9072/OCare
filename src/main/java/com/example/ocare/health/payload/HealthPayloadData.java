package com.example.ocare.health.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 페이로드 본문.
 *
 * @param memo    단말이 남긴 메모. 애플 데이터에만 존재하며 거의 빈 문자열이다
 * @param source  데이터 출처와 단말 정보
 * @param entries 측정 구간 목록.
 */
public record HealthPayloadData(

        String memo,

        @Valid
        HealthPayloadSource source,

        @NotEmpty(message = "측정 데이터가 비어 있습니다.")
        @Valid
        List<HealthPayloadEntry> entries
) {
}
