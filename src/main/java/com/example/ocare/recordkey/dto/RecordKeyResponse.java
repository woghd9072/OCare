package com.example.ocare.recordkey.dto;

import com.example.ocare.recordkey.entity.RecordKey;

import java.time.LocalDateTime;

/**
 * 레코드키 조회 응답.
 *
 * <p>출처는 열거형 상수명이 아니라 단말이 보낸 표기 그대로 내려준다.
 * 클라이언트가 보낸 값과 응답 값이 달라 혼란스러워지는 것을 막기 위함이다.
 */
public record RecordKeyResponse(
        Long id,
        String recordKey,
        String source,
        String productName,
        String productVendor,
        String status,
        LocalDateTime createdAt
) {

    public static RecordKeyResponse from(RecordKey recordKey) {
        return new RecordKeyResponse(
                recordKey.getId(),
                recordKey.getRecordKey(),
                recordKey.getSource().payloadName(),
                recordKey.getProductName(),
                recordKey.getProductVendor(),
                recordKey.getStatus().name(),
                recordKey.getCreatedAt()
        );
    }
}
