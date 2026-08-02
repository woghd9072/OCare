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
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HealthMonthlySummaryWriterTest {

    private static final String RECORD_KEY = "monthly-test-key";
    private static final YearMonth NOVEMBER = YearMonth.of(2024, 11);
    private static final YearMonth DECEMBER = YearMonth.of(2024, 12);

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private HealthMonthlySummaryWriter writer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        jdbcTemplate.update("INSERT INTO members (id, name, nickname, email, password, created_at, updated_at) "
                + "VALUES (1, '월집계', 'monthly', 'monthly@example.com', 'x', NOW(6), NOW(6))");
        jdbcTemplate.update("INSERT INTO record_keys (member_id, record_key, source_name, status, created_at, updated_at) "
                + "VALUES (1, ?, 'SamsungHealth', 'ACTIVE', NOW(6), NOW(6))", RECORD_KEY);
    }

    @Test
    @DisplayName("일별 집계를 더해 월별 집계를 만든다")
    void aggregatesDailySummaries() {
        insertDaily(LocalDate.of(2024, 11, 15), 7243, "289.21", "5.419490", 38, true);
        insertDaily(LocalDate.of(2024, 11, 16), 10717, "425.53", "8.020480", 44, true);

        writer.upsert(RECORD_KEY, List.of(NOVEMBER));

        assertThat(steps(NOVEMBER)).isEqualTo(17960L);
        assertThat(decimal(NOVEMBER, "calories")).isEqualByComparingTo("714.74");
        assertThat(decimal(NOVEMBER, "distance_km")).isEqualByComparingTo("13.439970");
        assertThat(intValue(NOVEMBER, "active_days")).isEqualTo(2);
        assertThat(intValue(NOVEMBER, "entry_count")).isEqualTo(82);
    }

    @Test
    @DisplayName("월 합계는 일별 값을 그대로 더한 값과 같다")
    void monthlyEqualsSumOfDailyValues() {
        // 사용자는 일별 화면과 월별 화면을 함께 본다. 두 값이 어긋나면 오류로 받아들이므로
        // 월 합계는 원본이 아니라 일별 집계를 더해 만든다.
        insertDaily(LocalDate.of(2024, 11, 15), 7543, "0", "6.033970", 49, false);
        insertDaily(LocalDate.of(2024, 11, 16), 6630, "0", "5.303651", 45, false);

        writer.upsert(RECORD_KEY, List.of(NOVEMBER));

        assertThat(steps(NOVEMBER)).isEqualTo(7543L + 6630L);
    }

    @Test
    @DisplayName("활동이 기록된 일수만 센다")
    void countsOnlyActiveDays() {
        // 11월은 30일이지만 측정 데이터가 있는 날은 3일뿐이다
        insertDaily(LocalDate.of(2024, 11, 15), 100, "1", "0.1", 1, true);
        insertDaily(LocalDate.of(2024, 11, 20), 200, "2", "0.2", 1, true);
        insertDaily(LocalDate.of(2024, 11, 30), 300, "3", "0.3", 1, true);

        writer.upsert(RECORD_KEY, List.of(NOVEMBER));

        assertThat(intValue(NOVEMBER, "active_days")).isEqualTo(3);
    }

    @Test
    @DisplayName("월이 다른 데이터는 섞이지 않는다")
    void separatesMonths() {
        insertDaily(LocalDate.of(2024, 11, 30), 100, "1", "0.1", 1, true);
        insertDaily(LocalDate.of(2024, 12, 1), 200, "2", "0.2", 1, true);

        writer.upsert(RECORD_KEY, List.of(NOVEMBER, DECEMBER));

        assertThat(steps(NOVEMBER)).isEqualTo(100L);
        assertThat(steps(DECEMBER)).isEqualTo(200L);
    }

    @Test
    @DisplayName("여러 번 실행해도 결과가 같다")
    void isIdempotent() {
        insertDaily(LocalDate.of(2024, 11, 15), 7243, "289.21", "5.419490", 38, true);

        writer.upsert(RECORD_KEY, List.of(NOVEMBER));
        writer.upsert(RECORD_KEY, List.of(NOVEMBER));

        assertThat(steps(NOVEMBER)).isEqualTo(7243L);
        assertThat(rowCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("일별 값이 바뀌면 월 합계도 다시 계산된다")
    void recalculatesWhenDailyChanges() {
        insertDaily(LocalDate.of(2024, 11, 15), 7243, "289.21", "5.419490", 38, true);
        writer.upsert(RECORD_KEY, List.of(NOVEMBER));

        insertDaily(LocalDate.of(2024, 11, 16), 10717, "425.53", "8.020480", 44, true);
        writer.upsert(RECORD_KEY, List.of(NOVEMBER));

        assertThat(steps(NOVEMBER)).isEqualTo(17960L);
        assertThat(intValue(NOVEMBER, "active_days")).isEqualTo(2);
    }

    @Test
    @DisplayName("칼로리 미제공 표시가 월 단위로 이어진다")
    void carriesCaloriesSupportedFlag() {
        insertDaily(LocalDate.of(2024, 11, 15), 100, "0", "0.1", 1, false);
        insertDaily(LocalDate.of(2024, 11, 16), 200, "0", "0.2", 1, false);

        writer.upsert(RECORD_KEY, List.of(NOVEMBER));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT calories_supported FROM health_monthly_summaries WHERE summary_month = '2024-11'",
                Boolean.class)).isFalse();
    }

    @Test
    @DisplayName("대상 월이 없으면 아무것도 하지 않는다")
    void doesNothingForEmptyMonths() {
        assertThat(writer.upsert(RECORD_KEY, List.of())).isZero();
        assertThat(rowCount()).isZero();
    }

    private void insertDaily(LocalDate date, int steps, String calories, String distanceKm,
                             int entryCount, boolean caloriesSupported) {
        jdbcTemplate.update("INSERT INTO health_daily_summaries (record_key, summary_date, steps, calories, "
                        + "distance_km, entry_count, calories_supported, last_aggregated_at, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(6), NOW(6), NOW(6))",
                RECORD_KEY, date, steps, new BigDecimal(calories), new BigDecimal(distanceKm),
                entryCount, caloriesSupported);
    }

    private Long steps(YearMonth month) {
        return jdbcTemplate.queryForObject(
                "SELECT steps FROM health_monthly_summaries WHERE record_key = ? AND summary_month = ?",
                Long.class, RECORD_KEY, month.toString());
    }

    private BigDecimal decimal(YearMonth month, String column) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM health_monthly_summaries WHERE record_key = ? AND summary_month = ?",
                BigDecimal.class, RECORD_KEY, month.toString());
    }

    private Integer intValue(YearMonth month, String column) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM health_monthly_summaries WHERE record_key = ? AND summary_month = ?",
                Integer.class, RECORD_KEY, month.toString());
    }

    private Integer rowCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM health_monthly_summaries", Integer.class);
    }
}
