package com.example.ocare.health.ingest;

import com.example.ocare.common.util.Sha256;
import com.example.ocare.health.HealthSource;
import com.example.ocare.health.payload.NormalizedHealthEntry;
import com.example.ocare.health.payload.NormalizedHealthPayload;

import java.util.List;

/**
 * 재전송을 판별하기 위한 키를 만든다.
 *
 * <p>App to App 수집은 네트워크 실패나 앱 재시작으로 같은 데이터가 다시 올라오는 것이 정상 흐름이다.
 * 그대로 저장하면 걸음수가 두 배가 되므로, "같은 측정인지"를 판단할 기준이 필요하다.
 */
public final class HealthDedupKeys {

    private static final String DELIMITER = "|";

    private HealthDedupKeys() {
    }

    /**
     * 측정 구간 하나를 식별하는 키.
     *
     * <p>레코드키 + 출처 + 측정 항목 + 측정 구간으로 만든다. 측정값(걸음수 등)은 넣지 않는다.
     * 단말이 같은 구간의 값을 나중에 보정해 다시 보낼 수 있는데, 값을 키에 포함하면
     * 보정 전후가 서로 다른 측정으로 취급되어 중복 저장되기 때문이다.
     *
     * <p>출처를 포함하는 이유는, 한 사람이 두 단말을 함께 쓰면 같은 시간대에 각각의 측정이
     * 존재할 수 있고 이는 서로 다른 데이터이기 때문이다.
     *
     * <p>시각은 UTC 기준으로 넣는다. 표기가 다른 두 출처가 같은 시점을 보내면 같은 키가 되어야 한다.
     */
    public static String entryKey(String recordKey, HealthSource source, String dataType,
                                  NormalizedHealthEntry entry) {
        return Sha256.hex(String.join(DELIMITER,
                recordKey,
                source.name(),
                dataType,
                entry.startAtUtc().toString(),
                entry.endAtUtc().toString()
        ));
    }

    /**
     * payload 전체를 식별하는 키.
     *
     * <p>구간별 키를 이어 붙여 해싱한다. JSON 의 공백이나 필드 순서가 달라도 측정 내용이 같으면
     * 같은 값이 나오므로, 앱이 재전송할 때 직렬화 방식이 조금 달라져도 재전송으로 인식된다.
     */
    public static String payloadHash(NormalizedHealthPayload payload, List<String> entryKeys) {
        return Sha256.hex(String.join(DELIMITER,
                payload.recordKey(),
                payload.source().name(),
                payload.dataType(),
                String.join(DELIMITER, entryKeys)
        ));
    }
}
