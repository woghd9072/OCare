package com.example.ocare.recordkey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 레코드키 등록 요청.
 *
 * @param recordKey     단말이 발급한 사용자 구분 키. 입력 데이터에서는 UUID 형식이다
 * @param source        데이터 출처. 단말이 보내는 표기 그대로 받는다 (SamsungHealth / Health Kit)
 * @param productName   단말 제품명 (Android / iPhone)
 * @param productVendor 단말 제조사 (Samsung / Apple inc.)
 */
public record RecordKeyRegisterRequest(

        @NotBlank(message = "레코드키는 필수입니다.")
        @Size(max = 64, message = "레코드키는 64자 이하여야 합니다.")
        String recordKey,

        @NotBlank(message = "데이터 출처는 필수입니다.")
        @Size(max = 30, message = "데이터 출처는 30자 이하여야 합니다.")
        String source,

        @Size(max = 50, message = "제품명은 50자 이하여야 합니다.")
        String productName,

        @Size(max = 50, message = "제조사는 50자 이하여야 합니다.")
        String productVendor
) {
}
