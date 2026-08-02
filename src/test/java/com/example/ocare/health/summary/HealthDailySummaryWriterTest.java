package com.example.ocare.health.summary;

import com.example.ocare.support.DatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HealthDailySummaryWriterTest {

    private static final String SAMSUNG_KEY = "summary-samsung-key";
    private static final String APPLE_KEY = "summary-apple-key";
    private static final LocalDate DATE = LocalDate.of(2024, 11, 15);

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private HealthDailySummaryWriter writer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        jdbcTemplate.update("INSERT INTO members (id, name, nickname, email, password, created_at, updated_at) "
                + "VALUES (1, '집계', 'summary', 'summary@example.com', 'x', NOW(6), NOW(6))");
        insertRecordKey(SAMSUNG_KEY, "SamsungHealth");
        insertRecordKey(APPLE_KEY, "Health Kit");
        insertUpload(1, SAMSUNG_KEY, "SamsungHealth");
        insertUpload(2, APPLE_KEY, "Health Kit");
    }

    @Test
    @DisplayName("원본을 합산해 일별 집계를 만든다")
    void createsSummary() {
        insertRecord(SAMSUNG_KEY, "SamsungHealth", 1, DATE, "54", "2.03", "0.04223", "d1");
        insertRecord(SAMSUNG_KEY, "SamsungHealth", 1, DATE, "7", "0.24", "0.00553", "d2");

        writer.upsert(SAMSUNG_KEY, List.of(DATE));

        assertThat(steps(SAMSUNG_KEY, DATE)).isEqualTo(61);
        assertThat(decimal(SAMSUNG_KEY, DATE, "calories")).isEqualByComparingTo("2.2700");
        assertThat(decimal(SAMSUNG_KEY, DATE, "distance_km")).isEqualByComparingTo("0.047760");
        assertThat(entryCount(SAMSUNG_KEY, DATE)).isEqualTo(2);
    }

    @Test
    @DisplayName("소수 걸음수는 모두 더한 뒤 한 번만 반올림한다")
    void roundsOnlyAfterSummation() {
        // 각각 반올림하면 6672 가 되지만, 합산 후 반올림하면 6673 이다.
        insertRecord(APPLE_KEY, "Health Kit", 2, DATE, "3336.4", "0", "1.5", "a1");
        insertRecord(APPLE_KEY, "Health Kit", 2, DATE, "3336.4", "0", "1.5", "a2");

        writer.upsert(APPLE_KEY, List.of(DATE));

        assertThat(steps(APPLE_KEY, DATE)).isEqualTo(6673);
    }

    @Test
    @DisplayName("여러 번 실행해도 결과가 같다")
    void isIdempotent() {
        insertRecord(SAMSUNG_KEY, "SamsungHealth", 1, DATE, "54", "2.03", "0.04223", "d1");

        writer.upsert(SAMSUNG_KEY, List.of(DATE));
        writer.upsert(SAMSUNG_KEY, List.of(DATE));
        writer.upsert(SAMSUNG_KEY, List.of(DATE));

        // 누적이 아니라 재계산이므로 몇 번을 실행해도 값이 늘지 않는다
        assertThat(steps(SAMSUNG_KEY, DATE)).isEqualTo(54);
        assertThat(rowCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("원본이 늘면 다시 계산한 값으로 갱신된다")
    void recalculatesAfterMoreRecords() {
        insertRecord(SAMSUNG_KEY, "SamsungHealth", 1, DATE, "54", "2.03", "0.04223", "d1");
        writer.upsert(SAMSUNG_KEY, List.of(DATE));

        insertRecord(SAMSUNG_KEY, "SamsungHealth", 1, DATE, "46", "1.97", "0.03777", "d2");
        writer.upsert(SAMSUNG_KEY, List.of(DATE));

        assertThat(steps(SAMSUNG_KEY, DATE)).isEqualTo(100);
        assertThat(entryCount(SAMSUNG_KEY, DATE)).isEqualTo(2);
    }

    @Test
    @DisplayName("애플 데이터는 칼로리 미제공으로 표시된다")
    void marksCaloriesUnsupportedForAppleSource() {
        insertRecord(APPLE_KEY, "Health Kit", 2, DATE, "100", "0", "0.08", "a1");
        insertRecord(SAMSUNG_KEY, "SamsungHealth", 1, DATE, "100", "0", "0.08", "d1");

        writer.upsert(APPLE_KEY, List.of(DATE));
        writer.upsert(SAMSUNG_KEY, List.of(DATE));

        // 칼로리 합계가 둘 다 0 이지만, 값을 제공하지 않는 출처와
        // 온종일 움직이지 않은 날은 구분되어야 한다
        assertThat(caloriesSupported(APPLE_KEY, DATE)).isFalse();
        assertThat(caloriesSupported(SAMSUNG_KEY, DATE)).isTrue();
    }

    @Test
    @DisplayName("여러 날짜를 한 번에 갱신한다")
    void upsertsMultipleDates() {
        insertRecord(SAMSUNG_KEY, "SamsungHealth", 1, DATE, "54", "2.03", "0.04", "d1");
        insertRecord(SAMSUNG_KEY, "SamsungHealth", 1, DATE.plusDays(1), "77", "3.01", "0.06", "d2");

        writer.upsert(SAMSUNG_KEY, List.of(DATE, DATE.plusDays(1)));

        assertThat(steps(SAMSUNG_KEY, DATE)).isEqualTo(54);
        assertThat(steps(SAMSUNG_KEY, DATE.plusDays(1))).isEqualTo(77);
    }

    @Test
    @DisplayName("다른 레코드키의 데이터는 섞이지 않는다")
    void doesNotMixRecordKeys() {
        insertRecord(SAMSUNG_KEY, "SamsungHealth", 1, DATE, "54", "2.03", "0.04", "d1");
        insertRecord(APPLE_KEY, "Health Kit", 2, DATE, "999", "0", "0.8", "a1");

        writer.upsert(SAMSUNG_KEY, List.of(DATE));

        assertThat(steps(SAMSUNG_KEY, DATE)).isEqualTo(54);
        assertThat(rowCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("대상 날짜가 없으면 아무것도 하지 않는다")
    void doesNothingForEmptyDates() {
        assertThat(writer.upsert(SAMSUNG_KEY, List.of())).isZero();
        assertThat(rowCount()).isZero();
    }

    private void insertRecordKey(String recordKey, String source) {
        jdbcTemplate.update("INSERT INTO record_keys (member_id, record_key, source_name, status, created_at, updated_at) "
                + "VALUES (1, ?, ?, 'ACTIVE', NOW(6), NOW(6))", recordKey, source);
    }

    private void insertUpload(long id, String recordKey, String source) {
        jdbcTemplate.update("INSERT INTO health_uploads (id, record_key, data_type, source_name, entry_count, "
                + "saved_count, duplicated_count, failed_count, payload_hash, created_at, updated_at) "
                + "VALUES (?, ?, 'steps', ?, 0, 0, 0, 0, ?, NOW(6), NOW(6))",
                id, recordKey, source, "hash-" + id);
    }

    private void insertRecord(String recordKey, String source, long uploadId, LocalDate date,
                              String steps, String calories, String distance, String dedupSuffix) {
        jdbcTemplate.update("INSERT INTO health_records (upload_id, record_key, data_type, source_name, "
                        + "start_at_utc, end_at_utc, measured_date, steps, calories, distance_km, dedup_key, created_at) "
                        + "VALUES (?, ?, 'steps', ?, ?, ?, ?, ?, ?, ?, ?, NOW(6))",
                uploadId, recordKey, source, date.atStartOfDay(), date.atStartOfDay().plusMinutes(10), date,
                new BigDecimal(steps), new BigDecimal(calories), new BigDecimal(distance),
                recordKey + "-" + date + "-" + dedupSuffix);
    }

    private Integer steps(String recordKey, LocalDate date) {
        return jdbcTemplate.queryForObject(
                "SELECT steps FROM health_daily_summaries WHERE record_key = ? AND summary_date = ?",
                Integer.class, recordKey, date);
    }

    private BigDecimal decimal(String recordKey, LocalDate date, String column) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM health_daily_summaries WHERE record_key = ? AND summary_date = ?",
                BigDecimal.class, recordKey, date);
    }

    private Integer entryCount(String recordKey, LocalDate date) {
        return jdbcTemplate.queryForObject(
                "SELECT entry_count FROM health_daily_summaries WHERE record_key = ? AND summary_date = ?",
                Integer.class, recordKey, date);
    }

    private Boolean caloriesSupported(String recordKey, LocalDate date) {
        return jdbcTemplate.queryForObject(
                "SELECT calories_supported FROM health_daily_summaries WHERE record_key = ? AND summary_date = ?",
                Boolean.class, recordKey, date);
    }

    private Integer rowCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM health_daily_summaries", Integer.class);
    }
}
