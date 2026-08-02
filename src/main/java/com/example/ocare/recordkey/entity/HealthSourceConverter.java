package com.example.ocare.recordkey.entity;

import com.example.ocare.health.HealthSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link HealthSource} 를 단말이 보낸 원본 문자열로 저장한다.
 *
 * <p>{@code @Enumerated(STRING)} 을 쓰면 {@code SAMSUNG_HEALTH} 같은 열거형 상수명이 저장된다.
 * 그러면 DB 값만 보고는 단말이 실제로 무엇을 보냈는지 알 수 없어, 수집 데이터를 추적할 때
 * 원본과 대조하기 어려워진다. 스키마 주석에도 {@code SamsungHealth / Health Kit} 로 명시해 두었다.
 *
 * <p>그래서 코드에서는 열거형으로 안전하게 다루고, 저장은 원본 표기를 유지한다.
 */
@Converter(autoApply = true)
public class HealthSourceConverter implements AttributeConverter<HealthSource, String> {

    @Override
    public String convertToDatabaseColumn(HealthSource attribute) {
        return attribute == null ? null : attribute.payloadName();
    }

    @Override
    public HealthSource convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HealthSource.fromPayloadName(dbData);
    }
}
