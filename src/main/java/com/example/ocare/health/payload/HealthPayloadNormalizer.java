package com.example.ocare.health.payload;

import com.example.ocare.common.exception.BusinessException;
import com.example.ocare.common.exception.ErrorCode;
import com.example.ocare.health.HealthSource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 단말이 보낸 페이로드를 저장 가능한 형태로 정규화한다.
 *
 * <p>출처별 표기 차이를 흡수하는 지점이 여기 한 곳으로 모여 있다.
 * 이후 단계(저장, 집계, 조회)는 삼성인지 애플인지 구분하지 않는다.
 * 새로운 출처가 추가되면 이 클래스와 {@link HealthSource} 만 손보면 된다.
 *
 * <p>정규화 항목은 네 가지다.
 * <ul>
 *   <li>출처 판별 — source.name 표기로 삼성/애플 구분</li>
 *   <li>시각 — 두 가지 표기를 UTC 로 통일하고 KST 기준일 산출</li>
 *   <li>단위 — km / kcal 확인</li>
 *   <li>수치 — 숫자와 문자열 표기를 모두 BigDecimal 로 수용 (역직렬화 단계에서 처리됨)</li>
 * </ul>
 */
@Component
public class HealthPayloadNormalizer {

    public NormalizedHealthPayload normalize(HealthPayload payload) {
        HealthSource source = resolveSource(payload);

        List<NormalizedHealthEntry> entries = payload.data().entries().stream()
                .map(this::normalizeEntry)
                .toList();

        return new NormalizedHealthPayload(
                payload.recordkey(),
                payload.type(),
                source,
                payload.data().source() == null ? null : payload.data().source().mode(),
                payload.data().source() == null ? null : payload.data().source().productName(),
                payload.data().source() == null ? null : payload.data().source().productVendor(),
                payload.data().memo(),
                parseLastUpdate(payload.lastUpdate()),
                entries
        );
    }

    private NormalizedHealthEntry normalizeEntry(HealthPayloadEntry entry) {
        ParsedTime start = HealthTimeParser.parse(entry.period().from());
        ParsedTime end = HealthTimeParser.parse(entry.period().to());

        if (end.instant().isBefore(start.instant())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "측정 종료 시각이 시작 시각보다 빠릅니다: " + entry.period().from() + " ~ " + entry.period().to());
        }

        return new NormalizedHealthEntry(
                start.instant(),
                end.instant(),
                start.sourceOffset(),
                // 기준일은 시작 시각으로 정한다. 자정을 걸치는 구간이 생기더라도
                // 활동이 시작된 날에 귀속되어 한 구간이 두 날짜로 쪼개지지 않는다.
                start.measuredDate(),
                entry.steps(),
                HealthMeasures.caloriesInKilocalories(entry.calories()),
                HealthMeasures.distanceInKilometers(entry.distance())
        );
    }

    /**
     * 출처를 판별한다.
     *
     * <p>{@code source.mode} 값(삼성 9, 애플 10)이 아니라 {@code source.name} 표기로 판별한다.
     * mode 는 문서화된 값이 아니라 단말 버전에 따라 달라질 수 있고,
     * name 은 데이터 형식과 직접 대응하기 때문이다.
     */
    private HealthSource resolveSource(HealthPayload payload) {
        HealthPayloadSource source = payload.data().source();
        if (source == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "데이터 출처 정보가 없습니다.");
        }
        try {
            return HealthSource.fromPayloadName(source.name());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * 단말의 마지막 갱신 시각. 수집 이력에만 기록하는 부가 정보라, 없거나 해석할 수 없어도
     * 수집 자체를 실패시키지 않는다. 측정 데이터의 정확성과는 무관하기 때문이다.
     */
    private java.time.Instant parseLastUpdate(String lastUpdate) {
        if (lastUpdate == null || lastUpdate.isBlank()) {
            return null;
        }
        try {
            return HealthTimeParser.parse(lastUpdate).instant();
        } catch (BusinessException e) {
            return null;
        }
    }
}
