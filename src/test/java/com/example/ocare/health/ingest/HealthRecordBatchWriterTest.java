package com.example.ocare.health.ingest;

import com.example.ocare.health.HealthSource;
import com.example.ocare.health.entity.HealthRecord;
import com.example.ocare.health.entity.HealthUpload;
import com.example.ocare.health.entity.HealthUploadRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HealthRecordBatchWriterTest {

    private static final String RECORD_KEY = "batch-test-key";
    private static final Instant BASE = Instant.parse("2024-11-14T15:00:00Z");

    @Autowired
    private HealthRecordBatchWriter batchWriter;

    @Autowired
    private HealthUploadRepository uploadRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long uploadId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM health_records");
        jdbcTemplate.update("DELETE FROM health_uploads");
        jdbcTemplate.update("DELETE FROM record_keys");
        jdbcTemplate.update("DELETE FROM members");
        jdbcTemplate.update("INSERT INTO members (id, name, nickname, email, password, created_at, updated_at) "
                + "VALUES (1, '배치', 'batch', 'batch@example.com', 'x', NOW(6), NOW(6))");
        jdbcTemplate.update("INSERT INTO record_keys (member_id, record_key, source_name, status, created_at, updated_at) "
                + "VALUES (1, ?, 'SamsungHealth', 'ACTIVE', NOW(6), NOW(6))", RECORD_KEY);

        uploadId = uploadRepository.save(HealthUpload.start(
                RECORD_KEY, "steps", HealthSource.SAMSUNG_HEALTH, 9, "Android", "Samsung", null,
                null, 0, "batch-hash")).getId();
    }

    @Test
    @DisplayName("실제 수집 규모를 청크로 나눠 저장한다")
    void insertsRealisticBatch() {
        // INPUT_DATA1 과 같은 1,066건. 청크 크기(500)를 넘겨 여러 번에 나눠 나가는 경로를 태운다.
        List<HealthRecord> records = records(1066);

        int saved = batchWriter.insertAll(uploadId, records);

        assertThat(saved).isEqualTo(1066);
        assertThat(count()).isEqualTo(1066);
    }

    @Test
    @DisplayName("청크 경계를 넘지 않는 소량도 저장된다")
    void insertsSmallBatch() {
        assertThat(batchWriter.insertAll(uploadId, records(10))).isEqualTo(10);
    }

    @Test
    @DisplayName("빈 목록은 아무것도 저장하지 않는다")
    void insertsNothingForEmptyList() {
        assertThat(batchWriter.insertAll(uploadId, List.of())).isZero();
        assertThat(count()).isZero();
    }

    @Test
    @DisplayName("dedup_key 가 겹치면 건너뛰고 나머지는 저장한다")
    void skipsDuplicatesWithoutFailingBatch() {
        batchWriter.insertAll(uploadId, records(10));

        // 앞 5건은 이미 저장된 것과 같은 dedup_key 를 가진다.
        // 동시 요청이 겹쳐 Redis 검사를 함께 통과한 상황에 해당한다.
        List<HealthRecord> overlapping = new ArrayList<>(records(15));

        int saved = batchWriter.insertAll(uploadId, overlapping);

        // 중복 5건 때문에 전체가 실패하지 않고, 새 5건이 더해져 15건이 된다
        assertThat(saved).isEqualTo(15);
        assertThat(count()).isEqualTo(15);
    }

    @Test
    @DisplayName("측정 시각은 UTC 벽시계로 저장된다")
    void storesUtcWallClock() {
        batchWriter.insertAll(uploadId, records(1));

        String stored = jdbcTemplate.queryForObject(
                "SELECT DATE_FORMAT(start_at_utc, '%Y-%m-%d %H:%i:%s') FROM health_records", String.class);

        assertThat(stored).isEqualTo("2024-11-14 15:00:00");
    }

    @Test
    @DisplayName("저장 시각과 소수 걸음수가 그대로 기록된다")
    void storesCreatedAtAndFractionalSteps() {
        batchWriter.insertAll(uploadId, List.of(record(0, new BigDecimal("688.550985"))));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT created_at IS NOT NULL FROM health_records", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT steps FROM health_records", BigDecimal.class)).isEqualByComparingTo("688.550985");
    }

    private List<HealthRecord> records(int count) {
        List<HealthRecord> records = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            records.add(record(i, new BigDecimal("54")));
        }
        return records;
    }

    private HealthRecord record(int index, BigDecimal steps) {
        Instant start = BASE.plusSeconds(600L * index);
        return HealthRecord.create(uploadId, RECORD_KEY, "steps", HealthSource.SAMSUNG_HEALTH,
                start, start.plusSeconds(600), null, LocalDate.of(2024, 11, 15),
                steps, new BigDecimal("2.03"), new BigDecimal("0.04223"), "dedup-" + index);
    }

    private Integer count() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM health_records", Integer.class);
    }
}
