package com.example.ocare.health.payload;

import com.example.ocare.health.HealthSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 과제로 제공된 실제 입력 데이터 4개 파일로 정규화 전체 과정을 검증한다.
 *
 * <p>단위 테스트로 만든 예시가 아니라 실제 파일을 그대로 쓰는 이유는,
 * 표기 흔들림이나 예외적인 값이 실데이터에만 존재하기 때문이다.
 * 삼성 파일의 0초 구간, 애플 파일의 소수 걸음수가 그런 예다.
 */
class RealPayloadNormalizationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HealthPayloadNormalizer normalizer = new HealthPayloadNormalizer();

    @Test
    @DisplayName("삼성 파일 1 - 구조와 일별 합계가 원본과 일치한다")
    void samsungFile1() throws IOException {
        NormalizedHealthPayload payload = normalize("INPUT_DATA1.json");

        assertThat(payload.recordKey()).isEqualTo("7836887b-b12a-440f-af0f-851546504b13");
        assertThat(payload.source()).isEqualTo(HealthSource.SAMSUNG_HEALTH);
        assertThat(payload.dataType()).isEqualTo("steps");
        assertThat(payload.sourceMode()).isEqualTo(9);
        assertThat(payload.productVendor()).isEqualTo("Samsung");
        assertThat(payload.entryCount()).isEqualTo(1066);

        assertDailyTotal(payload, "2024-11-15", "7243", "289.20995225", "5.4194896718", 38);
        assertDailyTotal(payload, "2024-11-16", "10717", "425.52994815", "8.0204797034", 44);
        assertDateRange(payload, "2024-11-15", "2024-12-16", 32);
    }

    @Test
    @DisplayName("삼성 파일 2 - 구조와 일별 합계가 원본과 일치한다")
    void samsungFile2() throws IOException {
        NormalizedHealthPayload payload = normalize("INPUT_DATA2.json");

        assertThat(payload.recordKey()).isEqualTo("3b87c9a4-f983-4168-8f27-85436447bb57");
        assertThat(payload.source()).isEqualTo(HealthSource.SAMSUNG_HEALTH);
        assertThat(payload.entryCount()).isEqualTo(1497);

        assertDailyTotal(payload, "2024-11-15", "9589", "345.35993438", "7.2432301600", 65);
        assertDateRange(payload, "2024-11-15", "2024-12-16", 32);
    }

    @Test
    @DisplayName("애플 파일 3 - 소수 걸음수를 손실 없이 합산한다")
    void appleFile3() throws IOException {
        NormalizedHealthPayload payload = normalize("INPUT_DATA3.json");

        assertThat(payload.recordKey()).isEqualTo("7b012e6e-ba2b-49c7-bc2e-473b7b58e72e");
        assertThat(payload.source()).isEqualTo(HealthSource.APPLE_HEALTH);
        assertThat(payload.sourceMode()).isEqualTo(10);
        assertThat(payload.productVendor()).isEqualTo("Apple inc.");
        assertThat(payload.entryCount()).isEqualTo(1459);

        assertDailyTotal(payload, "2024-11-15", "7542.462901604615283", "0", "6.033970321283692447", 49);
        assertDateRange(payload, "2024-11-15", "2024-12-15", 31);
    }

    @Test
    @DisplayName("애플 파일 4 - 소수 걸음수를 손실 없이 합산한다")
    void appleFile4() throws IOException {
        NormalizedHealthPayload payload = normalize("INPUT_DATA4.json");

        assertThat(payload.recordKey()).isEqualTo("e27ba7ef-8bb2-424c-af1d-877e826b7487");
        assertThat(payload.source()).isEqualTo(HealthSource.APPLE_HEALTH);
        assertThat(payload.entryCount()).isEqualTo(688);

        assertDailyTotal(payload, "2024-11-16", "12449.99999999999997", "0", "9.96000000000000047", 28);
        assertDateRange(payload, "2024-11-15", "2024-12-15", 31);
    }

    @Test
    @DisplayName("애플 파일의 첫 구간은 UTC 로 11월 14일이지만 11월 15일로 집계된다")
    void appleFirstEntryIsAssignedToKstDate() throws IOException {
        NormalizedHealthPayload payload = normalize("INPUT_DATA3.json");

        NormalizedHealthEntry first = payload.entries().stream()
                .min(Comparator.comparing(NormalizedHealthEntry::startAtUtc))
                .orElseThrow();

        assertThat(first.startAtUtc().toString()).isEqualTo("2024-11-14T21:20:00Z");
        assertThat(first.measuredDate()).isEqualTo(LocalDate.of(2024, 11, 15));
        assertThat(first.sourceOffset()).isEqualTo("+0000");
    }

    @Test
    @DisplayName("삼성 데이터는 오프셋이 없어 KST 로 추정했음이 기록된다")
    void samsungEntriesHaveNoExplicitOffset() throws IOException {
        NormalizedHealthPayload payload = normalize("INPUT_DATA1.json");

        assertThat(payload.entries()).allSatisfy(entry ->
                assertThat(entry.sourceOffset()).isNull());
    }

    @Test
    @DisplayName("삼성 데이터의 0초 구간을 버리지 않는다")
    void keepsZeroLengthPeriods() throws IOException {
        // from 과 to 가 같은 구간이 실제로 존재하며 걸음수도 정상값이다.
        // 잘못된 데이터로 보고 걸러내면 그만큼 걸음수가 사라진다.
        long zeroLength = normalize("INPUT_DATA1.json").entries().stream()
                .filter(entry -> entry.startAtUtc().equals(entry.endAtUtc()))
                .count();

        assertThat(zeroLength).isEqualTo(9);
        assertThat(normalize("INPUT_DATA2.json").entries().stream()
                .filter(entry -> entry.startAtUtc().equals(entry.endAtUtc()))
                .count()).isEqualTo(6);
    }

    @Test
    @DisplayName("애플 데이터의 소수 걸음수가 정수로 잘리지 않는다")
    void preservesFractionalSteps() throws IOException {
        long fractional = normalize("INPUT_DATA3.json").entries().stream()
                .filter(entry -> entry.steps().stripTrailingZeros().scale() > 0)
                .count();

        assertThat(fractional).isEqualTo(963);
        assertThat(normalize("INPUT_DATA4.json").entries().stream()
                .filter(entry -> entry.steps().stripTrailingZeros().scale() > 0)
                .count()).isEqualTo(535);
    }

    @Test
    @DisplayName("합산 후 반올림과 구간별 반올림의 결과가 다르다")
    void roundingStrategyChangesResult() throws IOException {
        NormalizedHealthPayload payload = normalize("INPUT_DATA4.json");

        Map<LocalDate, BigDecimal> exactSums = payload.entries().stream()
                .collect(Collectors.groupingBy(NormalizedHealthEntry::measuredDate,
                        Collectors.reducing(BigDecimal.ZERO, NormalizedHealthEntry::steps, BigDecimal::add)));

        // 합산 후 1회 반올림
        BigDecimal sumThenRound = exactSums.values().stream()
                .map(value -> value.setScale(0, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 구간별로 반올림한 뒤 합산
        BigDecimal roundEachThenSum = payload.entries().stream()
                .map(entry -> entry.steps().setScale(0, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(sumThenRound).isEqualByComparingTo("273096");
        assertThat(roundEachThenSum).isEqualByComparingTo("273101");

        // 차이는 크지 않지만 원본과 정확히 일치해야 하는 값이므로 합산 후 반올림을 택한다.
        // 특히 11월 16일은 정확한 합이 12449.99999999999997 이라, 구간별로 반올림하면 12449 가 되어
        // 사용자가 실제로 걸은 12450 걸음과 어긋난다.
        assertThat(exactSums.get(LocalDate.of(2024, 11, 16)).setScale(0, RoundingMode.HALF_UP))
                .isEqualByComparingTo("12450");
    }

    private void assertDailyTotal(NormalizedHealthPayload payload, String date,
                                  String steps, String calories, String distanceKm, int entryCount) {
        Map<LocalDate, java.util.List<NormalizedHealthEntry>> byDate = payload.entries().stream()
                .collect(Collectors.groupingBy(NormalizedHealthEntry::measuredDate));

        java.util.List<NormalizedHealthEntry> entries = byDate.get(LocalDate.parse(date));
        assertThat(entries).as("%s 의 측정 구간", date).hasSize(entryCount);

        assertThat(sum(entries, NormalizedHealthEntry::steps)).as("%s 걸음수", date)
                .isEqualByComparingTo(steps);
        assertThat(sum(entries, NormalizedHealthEntry::caloriesKcal)).as("%s 칼로리", date)
                .isEqualByComparingTo(calories);
        assertThat(sum(entries, NormalizedHealthEntry::distanceKm)).as("%s 이동거리", date)
                .isEqualByComparingTo(distanceKm);
    }

    private void assertDateRange(NormalizedHealthPayload payload, String first, String last, int dayCount) {
        var dates = payload.entries().stream()
                .map(NormalizedHealthEntry::measuredDate)
                .collect(Collectors.toCollection(java.util.TreeSet::new));

        assertThat(dates.first()).isEqualTo(LocalDate.parse(first));
        assertThat(dates.last()).isEqualTo(LocalDate.parse(last));
        assertThat(dates).hasSize(dayCount);
    }

    private BigDecimal sum(java.util.List<NormalizedHealthEntry> entries,
                           Function<NormalizedHealthEntry, BigDecimal> extractor) {
        return entries.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private NormalizedHealthPayload normalize(String fileName) throws IOException {
        try (InputStream in = new ClassPathResource("health/" + fileName).getInputStream()) {
            return normalizer.normalize(objectMapper.readValue(in, HealthPayload.class));
        }
    }
}
