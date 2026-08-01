package com.example.ocare.health.payload;

import com.example.ocare.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 두 출처의 시각 표기를 하나의 기준(UTC)으로 모으는지 검증한다.
 *
 * <p>이 변환이 틀리면 일별 집계 경계가 통째로 어긋나므로, 실제 입력 데이터에 등장한
 * 표기를 그대로 사용해 검증한다.
 */
class HealthTimeParserTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("오프셋이 없는 삼성 표기는 기기 로컬(KST)로 해석한다")
    void parseSamsungLocalTime() {
        ParsedTime parsed = HealthTimeParser.parse("2024-11-15 00:00:00");

        // KST 자정은 UTC 로 전날 15:00 이다
        assertThat(parsed.instant()).isEqualTo(Instant.parse("2024-11-14T15:00:00Z"));
        assertThat(parsed.hasExplicitOffset()).isFalse();
        assertThat(parsed.sourceOffset()).isNull();
    }

    @Test
    @DisplayName("ISO8601 오프셋 표기인 애플 시각은 그대로 UTC 로 변환한다")
    void parseAppleOffsetTime() {
        ParsedTime parsed = HealthTimeParser.parse("2024-11-14T21:20:00+0000");

        assertThat(parsed.instant()).isEqualTo(Instant.parse("2024-11-14T21:20:00Z"));
        assertThat(parsed.sourceOffset()).isEqualTo("+0000");
    }

    @Test
    @DisplayName("공백과 오프셋이 섞인 lastUpdate 표기도 해석한다")
    void parseLastUpdateFormat() {
        ParsedTime parsed = HealthTimeParser.parse("2024-12-16 14:40:00 +0000");

        assertThat(parsed.instant()).isEqualTo(Instant.parse("2024-12-16T14:40:00Z"));
        assertThat(parsed.sourceOffset()).isEqualTo("+0000");
    }

    @Test
    @DisplayName("콜론이 있는 오프셋과 Z 표기도 해석한다")
    void parseAlternativeOffsetNotations() {
        assertThat(HealthTimeParser.parse("2024-11-14T21:20:00+00:00").instant())
                .isEqualTo(Instant.parse("2024-11-14T21:20:00Z"));
        assertThat(HealthTimeParser.parse("2024-11-14T21:20:00Z").instant())
                .isEqualTo(Instant.parse("2024-11-14T21:20:00Z"));
        assertThat(HealthTimeParser.parse("2024-11-15T09:00:00+09:00").instant())
                .isEqualTo(Instant.parse("2024-11-15T00:00:00Z"));
    }

    @Test
    @DisplayName("두 출처의 같은 시점이 동일한 UTC 값으로 모인다")
    void bothSourcesConvergeToSameInstant() {
        // 애플이 보낸 2024-11-14T21:20:00+0000 은 KST 로 2024-11-15 06:20 이다.
        // 삼성이 같은 시점을 로컬 시각으로 보냈다면 아래처럼 표기된다.
        ParsedTime apple = HealthTimeParser.parse("2024-11-14T21:20:00+0000");
        ParsedTime samsung = HealthTimeParser.parse("2024-11-15 06:20:00");

        assertThat(apple.instant()).isEqualTo(samsung.instant());
    }

    @Test
    @DisplayName("애플의 첫 측정 구간은 KST 기준 11월 15일에 속한다")
    void appleFirstEntryBelongsToKstDate() {
        // INPUT_DATA3 의 첫 엔트리. UTC 로는 11월 14일이지만 KST 로는 15일 06:20 이다.
        // 집계를 UTC 기준으로 하면 이 데이터가 14일로 잘못 귀속된다.
        ParsedTime parsed = HealthTimeParser.parse("2024-11-14T21:20:00+0000");

        assertThat(parsed.instant().atZone(KST).toLocalDate())
                .isEqualTo("2024-11-15");
    }

    @Test
    @DisplayName("오프셋 없는 값을 UTC 로 잘못 읽으면 날짜가 밀린다")
    void localTimeMustNotBeTreatedAsUtc() {
        ParsedTime parsed = HealthTimeParser.parse("2024-11-15 00:00:00");

        // 올바른 해석: KST 자정 -> KST 기준 11월 15일
        assertThat(parsed.instant().atZone(KST).toLocalDate()).isEqualTo("2024-11-15");
        // 만약 UTC 로 읽었다면 KST 로는 11월 15일 09:00 이 되어 새벽 활동이 15일에서 사라진다
        assertThat(Instant.parse("2024-11-15T00:00:00Z").atZone(KST).getHour()).isEqualTo(9);
    }

    @Test
    @DisplayName("0초 구간도 정상적으로 해석한다")
    void parseZeroLengthPeriod() {
        // 삼성 데이터에는 from 과 to 가 같은 구간이 존재하며 걸음수도 정상값이다
        ParsedTime from = HealthTimeParser.parse("2024-11-16 00:00:00");
        ParsedTime to = HealthTimeParser.parse("2024-11-16 00:00:00");

        assertThat(from.instant()).isEqualTo(to.instant());
    }

    @Test
    @DisplayName("해석할 수 없는 표기는 거부한다")
    void rejectUnknownFormat() {
        assertThatThrownBy(() -> HealthTimeParser.parse("2024/11/15 00:00"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("해석할 수 없는 시각 표기");

        assertThatThrownBy(() -> HealthTimeParser.parse(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("측정 시각이 비어 있습니다");

        assertThatThrownBy(() -> HealthTimeParser.parse(null))
                .isInstanceOf(BusinessException.class);
    }
}
