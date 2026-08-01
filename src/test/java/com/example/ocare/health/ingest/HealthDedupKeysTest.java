package com.example.ocare.health.ingest;

import com.example.ocare.health.HealthSource;
import com.example.ocare.health.payload.NormalizedHealthEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HealthDedupKeysTest {

    private static final String RECORD_KEY = "7836887b-b12a-440f-af0f-851546504b13";
    private static final Instant START = Instant.parse("2024-11-14T15:00:00Z");
    private static final Instant END = Instant.parse("2024-11-14T15:10:00Z");

    @Test
    @DisplayName("같은 구간은 같은 키가 된다")
    void sameEntryProducesSameKey() {
        String first = key(entry(START, END, "54"));
        String second = key(entry(START, END, "54"));

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }

    @Test
    @DisplayName("측정값이 보정되어도 같은 구간이면 같은 키다")
    void correctedValueDoesNotChangeKey() {
        // 단말이 같은 구간의 값을 나중에 보정해 다시 보낼 수 있다.
        // 값을 키에 넣으면 보정 전후가 다른 측정으로 취급되어 중복 저장된다.
        assertThat(key(entry(START, END, "54"))).isEqualTo(key(entry(START, END, "60")));
    }

    @Test
    @DisplayName("구간이 다르면 다른 키가 된다")
    void differentPeriodProducesDifferentKey() {
        assertThat(key(entry(START, END, "54")))
                .isNotEqualTo(key(entry(START.plusSeconds(600), END.plusSeconds(600), "54")));
    }

    @Test
    @DisplayName("0초 구간은 같은 시각의 10분 구간과 다른 키가 된다")
    void zeroLengthPeriodIsDistinct() {
        // 삼성 데이터에는 from 과 to 가 같은 구간이 존재하며, 같은 시각에 시작하는
        // 10분 구간과 함께 들어온다. 둘은 서로 다른 측정이므로 키가 달라야 한다.
        String zeroLength = key(entry(START, START, "105"));
        String tenMinutes = key(entry(START, END, "54"));

        assertThat(zeroLength).isNotEqualTo(tenMinutes);
    }

    @Test
    @DisplayName("출처가 다르면 같은 구간이라도 다른 키가 된다")
    void differentSourceProducesDifferentKey() {
        // 한 사람이 두 단말을 함께 쓰면 같은 시간대에 각각의 측정이 존재할 수 있다.
        String samsung = HealthDedupKeys.entryKey(RECORD_KEY, HealthSource.SAMSUNG_HEALTH, "steps",
                entry(START, END, "54"));
        String apple = HealthDedupKeys.entryKey(RECORD_KEY, HealthSource.APPLE_HEALTH, "steps",
                entry(START, END, "54"));

        assertThat(samsung).isNotEqualTo(apple);
    }

    @Test
    @DisplayName("레코드키가 다르면 다른 키가 된다")
    void differentRecordKeyProducesDifferentKey() {
        assertThat(key(entry(START, END, "54")))
                .isNotEqualTo(HealthDedupKeys.entryKey("other-record-key", HealthSource.SAMSUNG_HEALTH,
                        "steps", entry(START, END, "54")));
    }

    private String key(NormalizedHealthEntry entry) {
        return HealthDedupKeys.entryKey(RECORD_KEY, HealthSource.SAMSUNG_HEALTH, "steps", entry);
    }

    private NormalizedHealthEntry entry(Instant start, Instant end, String steps) {
        return new NormalizedHealthEntry(start, end, null, LocalDate.of(2024, 11, 15),
                new BigDecimal(steps), BigDecimal.ZERO, BigDecimal.ONE);
    }
}
