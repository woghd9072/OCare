package com.example.ocare.common.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * {@link Instant} 를 UTC 벽시계 값으로 저장한다.
 *
 * <p>MySQL 의 {@code DATETIME} 은 타임존을 담지 않는다. 그래서 어떤 기준의 시각인지는
 * 저장하는 쪽이 정해야 하는데, 드라이버에 맡기면 세션 타임존에 따라 값이 달라진다.
 *
 * <p>그래서 변환을 드라이버에 맡기지 않고 여기서 UTC 로 명시한다.
 * 설정이나 실행 환경이 바뀌어도 저장 값은 항상 UTC 다.
 */
@Converter
public class UtcInstantConverter implements AttributeConverter<Instant, LocalDateTime> {

    @Override
    public LocalDateTime convertToDatabaseColumn(Instant attribute) {
        return attribute == null ? null : LocalDateTime.ofInstant(attribute, ZoneOffset.UTC);
    }

    @Override
    public Instant convertToEntityAttribute(LocalDateTime dbData) {
        return dbData == null ? null : dbData.toInstant(ZoneOffset.UTC);
    }
}
