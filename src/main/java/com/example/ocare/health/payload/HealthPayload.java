package com.example.ocare.health.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 단말이 App to App 으로 전달한 건강활동 데이터 페이로드.
 *
 * <p>실제 입력 데이터의 구조를 그대로 받는다.
 * <pre>
 * {
 *   "recordkey": "7836887b-...",
 *   "type": "steps",
 *   "lastUpdate": "2024-12-16 14:40:00 +0000",
 *   "data": { "memo": "", "source": {...}, "entries": [...] }
 * }
 * </pre>
 *
 * @param recordkey  사용자 구분 키
 * @param type       측정 항목. 입력 데이터에서는 모두 "steps" 다
 * @param lastUpdate 단말이 데이터를 마지막으로 갱신한 시각
 * @param data       측정 구간 목록과 출처 정보
 */
public record HealthPayload(

        @NotBlank(message = "레코드키는 필수입니다.")
        @Size(max = 64, message = "레코드키는 64자 이하여야 합니다.")
        String recordkey,

        @NotBlank(message = "데이터 종류는 필수입니다.")
        @Size(max = 20, message = "데이터 종류는 20자 이하여야 합니다.")
        String type,

        String lastUpdate,

        @NotNull(message = "데이터 본문은 필수입니다.")
        @Valid
        HealthPayloadData data
) {
}
