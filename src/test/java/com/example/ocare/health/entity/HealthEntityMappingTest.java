package com.example.ocare.health.entity;

import com.example.ocare.health.HealthSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 엔티티가 스키마에 의도대로 매핑되는지 검증한다.
 *
 * <p>특히 시각 컬럼을 중점적으로 본다. {@code start_at_utc} 라는 이름과 달리 KST 가 저장되면
 * DB 를 직접 조회하는 사람이 오해하고, 집계 기준일과 대조할 때도 어긋나 보인다.
 */
@SpringBootTest
@ActiveProfiles("test")
class HealthEntityMappingTest {

    private static final String RECORD_KEY = "mapping-test-key";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HealthUploadRepository uploadRepository;

    @Autowired
    private HealthRecordRepository recordRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM health_records");
        jdbcTemplate.update("DELETE FROM health_uploads");
        jdbcTemplate.update("DELETE FROM record_keys");
        jdbcTemplate.update("DELETE FROM members");
        jdbcTemplate.update("INSERT INTO members (id, name, nickname, email, password, created_at, updated_at) "
                + "VALUES (1, '테스트', 'mapper', 'mapper@example.com', 'x', NOW(6), NOW(6))");
        jdbcTemplate.update("INSERT INTO record_keys (member_id, record_key, source_name, status, created_at, updated_at) "
                + "VALUES (1, ?, 'SamsungHealth', 'ACTIVE', NOW(6), NOW(6))", RECORD_KEY);
    }

    @Test
    @DisplayName("측정 시각은 UTC 벽시계로 저장된다")
    void storesInstantAsUtcWallClock() {
        Instant start = Instant.parse("2024-11-14T21:20:00Z");
        HealthUpload upload = uploadRepository.save(HealthUpload.start(
                RECORD_KEY, "steps", HealthSource.APPLE_HEALTH, 10, "iPhone", "Apple inc.", "",
                Instant.parse("2024-12-15T12:40:00Z"), 1, "hash-1"));

        recordRepository.save(HealthRecord.create(
                upload.getId(), RECORD_KEY, "steps", HealthSource.APPLE_HEALTH,
                start, start.plusSeconds(600), "+0000", LocalDate.of(2024, 11, 15),
                new BigDecimal("688.550985"), new BigDecimal("0"), new BigDecimal("0.550841"), "dedup-1"));

        String stored = jdbcTemplate.queryForObject(
                "SELECT DATE_FORMAT(start_at_utc, '%Y-%m-%d %H:%i:%s') FROM health_records", String.class);

        // 세션 타임존이 +09:00 이지만 UTC 값이 그대로 들어가야 한다
        assertThat(stored).isEqualTo("2024-11-14 21:20:00");
    }

    @Test
    @DisplayName("저장한 시각을 다시 읽으면 원래 Instant 와 같다")
    void readsBackSameInstant() {
        Instant start = Instant.parse("2024-11-14T21:20:00Z");
        HealthUpload upload = uploadRepository.save(HealthUpload.start(
                RECORD_KEY, "steps", HealthSource.SAMSUNG_HEALTH, 9, "Android", "Samsung", null,
                null, 1, "hash-2"));

        HealthRecord saved = recordRepository.save(HealthRecord.create(
                upload.getId(), RECORD_KEY, "steps", HealthSource.SAMSUNG_HEALTH,
                start, start.plusSeconds(600), null, LocalDate.of(2024, 11, 15),
                new BigDecimal("54"), new BigDecimal("2.03"), new BigDecimal("0.04223"), "dedup-2"));

        HealthRecord found = recordRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getStartAtUtc()).isEqualTo(start);
        assertThat(found.getMeasuredDate()).isEqualTo(LocalDate.of(2024, 11, 15));
        assertThat(found.getSourceOffset()).isNull();
        assertThat(found.getSteps()).isEqualByComparingTo("54");
    }

    @Test
    @DisplayName("출처는 단말이 보낸 표기 그대로 저장된다")
    void storesSourceAsPayloadName() {
        uploadRepository.save(HealthUpload.start(
                RECORD_KEY, "steps", HealthSource.APPLE_HEALTH, 10, "iPhone", "Apple inc.", "",
                null, 0, "hash-3"));

        String stored = jdbcTemplate.queryForObject(
                "SELECT source_name FROM health_uploads WHERE payload_hash = 'hash-3'", String.class);

        assertThat(stored).isEqualTo("Health Kit");
    }

    @Test
    @DisplayName("수집 결과 건수를 기록할 수 있다")
    void recordsUploadResult() {
        HealthUpload upload = uploadRepository.save(HealthUpload.start(
                RECORD_KEY, "steps", HealthSource.SAMSUNG_HEALTH, 9, "Android", "Samsung", null,
                null, 1066, "hash-4"));

        upload.complete(1000, 60, 6);
        uploadRepository.saveAndFlush(upload);

        HealthUpload found = uploadRepository.findById(upload.getId()).orElseThrow();
        assertThat(found.getEntryCount()).isEqualTo(1066);
        assertThat(found.getSavedCount()).isEqualTo(1000);
        assertThat(found.getDuplicatedCount()).isEqualTo(60);
        assertThat(found.getFailedCount()).isEqualTo(6);
    }
}
