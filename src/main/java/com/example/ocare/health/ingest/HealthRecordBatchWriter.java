package com.example.ocare.health.ingest;

import com.example.ocare.health.entity.HealthRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 측정 원본을 배치로 저장한다.
 *
 * <p>JPA 대신 JDBC 를 쓰는 이유는 식별자 생성 전략 때문이다.
 * 이 테이블은 {@code AUTO_INCREMENT} 를 쓰는데, Hibernate 는 IDENTITY 전략에서
 * 생성된 키를 즉시 받아와야 하므로 INSERT 를 묶지 못한다.
 * 설정에 {@code batch_size} 를 넣어도 1,500건이면 1,500번의 INSERT 가 나간다.
 * JDBC 배치로 보내면 드라이버의 {@code rewriteBatchedStatements} 옵션과 맞물려
 * 여러 행이 하나의 구문으로 합쳐진다.
 *
 * <p>한 번에 다 보내지 않고 나눠 보내는 이유는 두 가지다.
 * 구문 하나가 지나치게 커지면 {@code max_allowed_packet} 에 걸릴 수 있고,
 * 트랜잭션이 잠그는 시간이 길어져 같은 테이블을 쓰는 다른 요청이 밀린다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class HealthRecordBatchWriter {

    /**
     * 한 번에 보낼 행 수. 실제 수집 규모(최대 1,497건)를 서너 번에 나눠 보내는 크기다.
     */
    static final int CHUNK_SIZE = 500;

    private static final String INSERT_SQL = """
            INSERT INTO health_records
                (upload_id, record_key, data_type, source_name, start_at_utc, end_at_utc,
                 source_offset, measured_date, steps, calories, distance_km, dedup_key, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE id = id
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 원본을 저장하고 실제로 저장된 건수를 반환한다.
     *
     * <p>{@code ON DUPLICATE KEY UPDATE id = id} 는 dedup_key 가 겹칠 때 아무것도 바꾸지 않고 넘어간다.
     * 앞단의 Redis 검사를 통과한 뒤에도 동시 요청이 겹치면 여기서 충돌할 수 있는데,
     * 그때 전체 배치가 실패하면 정상 데이터까지 함께 버려지기 때문이다.
     * 다른 제약 위반(잘못된 upload_id 등)은 그대로 예외로 드러난다.
     *
     * <p>저장 건수는 배치 반환값 대신 저장 후 조회로 센다.
     * {@code rewriteBatchedStatements=true} 를 켜면 드라이버가 여러 행을 하나의 구문으로 합치면서
     * 행별 결과를 알려주지 않기 때문이다.
     */
    public int insertAll(Long uploadId, List<HealthRecord> records) {
        if (records.isEmpty()) {
            return 0;
        }

        for (int start = 0; start < records.size(); start += CHUNK_SIZE) {
            List<HealthRecord> chunk = records.subList(start, Math.min(start + CHUNK_SIZE, records.size()));
            insertChunk(chunk);
        }

        Integer saved = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM health_records WHERE upload_id = ?", Integer.class, uploadId);
        return saved == null ? 0 : saved;
    }

    private void insertChunk(List<HealthRecord> chunk) {
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.batchUpdate(INSERT_SQL, chunk, chunk.size(), (ps, record) -> bind(ps, record, now));
    }

    private void bind(PreparedStatement ps, HealthRecord record, LocalDateTime now) throws SQLException {
        ps.setLong(1, record.getUploadId());
        ps.setString(2, record.getRecordKey());
        ps.setString(3, record.getDataType());
        ps.setString(4, record.getSource().payloadName());
        // 컬럼 이름대로 UTC 벽시계를 넣는다. 드라이버에 맡기면 세션 타임존(+09:00)이 적용되어
        // KST 값이 들어간다.
        ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.ofInstant(record.getStartAtUtc(), ZoneOffset.UTC)));
        ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.ofInstant(record.getEndAtUtc(), ZoneOffset.UTC)));
        ps.setString(7, record.getSourceOffset());
        ps.setObject(8, record.getMeasuredDate());
        ps.setBigDecimal(9, record.getSteps());
        ps.setBigDecimal(10, record.getCalories());
        ps.setBigDecimal(11, record.getDistanceKm());
        ps.setString(12, record.getDedupKey());
        // JPA 를 거치지 않으므로 Auditing 이 동작하지 않는다. 저장 시각을 직접 넣는다.
        ps.setTimestamp(13, Timestamp.valueOf(now));
    }
}
