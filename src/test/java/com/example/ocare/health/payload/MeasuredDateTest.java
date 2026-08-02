package com.example.ocare.health.payload;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 집계 기준일 산출을 검증한다.
 *
 * <p>이 값이 하루라도 밀리면 일별/월별 집계가 전부 어긋나므로, 두 출처의 실제 표기가
 * 같은 날짜로 귀속되는지를 경계 시각 중심으로 확인한다.
 */
class MeasuredDateTest {

    @Test
    @DisplayName("삼성의 로컬 자정은 그 날짜에 귀속된다")
    void samsungMidnight() {
        LocalDate date = HealthTimeParser.parse("2024-11-15 00:00:00").measuredDate();

        assertThat(date).isEqualTo(LocalDate.of(2024, 11, 15));
    }

    @Test
    @DisplayName("삼성의 하루 마지막 구간도 같은 날짜에 귀속된다")
    void samsungLastEntryOfDay() {
        LocalDate date = HealthTimeParser.parse("2024-11-15 23:50:00").measuredDate();

        assertThat(date).isEqualTo(LocalDate.of(2024, 11, 15));
    }

    @Test
    @DisplayName("애플의 UTC 표기는 KST 날짜로 환산되어 귀속된다")
    void appleUtcConvertedToKstDate() {
        // INPUT_DATA3 의 첫 엔트리. UTC 로는 11월 14일이지만 KST 로는 15일 06:20 이다.
        LocalDate date = HealthTimeParser.parse("2024-11-14T21:20:00+0000").measuredDate();

        assertThat(date).isEqualTo(LocalDate.of(2024, 11, 15));
    }

    @Test
    @DisplayName("UTC 기준으로 집계하면 오전 활동이 전날로 밀린다")
    void utcBasedAggregationShiftsMorningActivity() {
        Instant instant = HealthTimeParser.parse("2024-11-14T21:20:00+0000").instant();

        // 올바른 기준(KST)
        assertThat(HealthTimeParser.measuredDate(instant)).isEqualTo(LocalDate.of(2024, 11, 15));
        // UTC 를 그대로 썼다면 하루 전으로 귀속된다
        assertThat(instant.atZone(ZoneOffset.UTC).toLocalDate())
                .isEqualTo(LocalDate.of(2024, 11, 14));
    }

    @Test
    @DisplayName("KST 자정 직전과 직후는 서로 다른 날짜로 나뉜다")
    void dayBoundary() {
        // KST 2024-11-15 23:50 (UTC 14:50)
        assertThat(HealthTimeParser.parse("2024-11-15T14:50:00+0000").measuredDate())
                .isEqualTo(LocalDate.of(2024, 11, 15));
        // KST 2024-11-16 00:00 (UTC 15:00)
        assertThat(HealthTimeParser.parse("2024-11-15T15:00:00+0000").measuredDate())
                .isEqualTo(LocalDate.of(2024, 11, 16));
    }

    @Test
    @DisplayName("표기가 달라도 같은 시점이면 같은 기준일이 된다")
    void sameInstantSameDateRegardlessOfNotation() {
        LocalDate fromApple = HealthTimeParser.parse("2024-11-14T21:20:00+0000").measuredDate();
        LocalDate fromSamsung = HealthTimeParser.parse("2024-11-15 06:20:00").measuredDate();

        assertThat(fromApple).isEqualTo(fromSamsung);
    }

    @Test
    @DisplayName("자정에 끝나는 구간은 시작 시각의 날짜에 귀속된다")
    void periodEndingAtMidnight() {
        // INPUT_DATA4 의 구간: 23:50 -> 00:00 (UTC). KST 로는 15일 08:50 -> 09:00 이다.
        ParsedTime from = HealthTimeParser.parse("2024-11-14T23:50:00+0000");
        ParsedTime to = HealthTimeParser.parse("2024-11-15T00:00:00+0000");

        // 시작 시각을 기준으로 삼으므로 두 값의 날짜가 같아야 한다
        assertThat(from.measuredDate()).isEqualTo(LocalDate.of(2024, 11, 15));
        assertThat(to.measuredDate()).isEqualTo(LocalDate.of(2024, 11, 15));
    }
}
