package com.example.ocare.health.entity;

import com.example.ocare.common.entity.UtcInstantConverter;
import com.example.ocare.health.HealthSource;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 단말에서 수집한 측정 구간 원본.
 *
 * <p>한 번 저장하면 수정하지 않는다(append-only). 그래서 {@code BaseTimeEntity} 를 상속하지 않고 생성 시각만 둔다.
 *
 * <p>같은 구간이 다시 들어오면 저장하지 않는다. 판별 기준은 {@code dedupKey} 이며,
 * 이 값에 UNIQUE 제약이 걸려 있어 동시 요청이 겹쳐도 한 건만 남는다.
 */
@Getter
@Entity
@Table(name = "health_records")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_id", nullable = false)
    private Long uploadId;

    /**
     * 조회 시 조인을 피하기 위해 비정규화해 둔 값.
     */
    @Column(name = "record_key", nullable = false, length = 64)
    private String recordKey;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @Column(name = "source_name", nullable = false, length = 30)
    private HealthSource source;

    /**
     * 측정 시작 시각. UTC 벽시계로 저장된다.
     */
    @Convert(converter = UtcInstantConverter.class)
    @Column(name = "start_at_utc", nullable = false)
    private Instant startAtUtc;

    @Convert(converter = UtcInstantConverter.class)
    @Column(name = "end_at_utc", nullable = false)
    private Instant endAtUtc;

    /**
     * 원본에 표기되어 있던 오프셋. null 이면 서버가 KST 로 추정했다는 뜻이다.
     */
    @Column(name = "source_offset", length = 6)
    private String sourceOffset;

    /**
     * 집계 기준일(KST). 일별/월별 집계는 모두 이 값을 기준으로 한다.
     */
    @Column(name = "measured_date", nullable = false)
    private LocalDate measuredDate;

    /**
     * 걸음수. 애플 데이터는 소수로 들어오므로 반올림하지 않고 원본을 보존한다.
     */
    @Column(name = "steps", nullable = false, precision = 12, scale = 6)
    private BigDecimal steps;

    @Column(name = "calories", nullable = false, precision = 10, scale = 4)
    private BigDecimal calories;

    @Column(name = "distance_km", nullable = false, precision = 10, scale = 6)
    private BigDecimal distanceKm;

    @Column(name = "dedup_key", nullable = false, length = 64)
    private String dedupKey;

    /**
     * 서버가 저장한 시각. 단말이 측정한 시각과 다르다.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private HealthRecord(Long uploadId, String recordKey, String dataType, HealthSource source,
                         Instant startAtUtc, Instant endAtUtc, String sourceOffset, LocalDate measuredDate,
                         BigDecimal steps, BigDecimal calories, BigDecimal distanceKm, String dedupKey) {
        this.uploadId = uploadId;
        this.recordKey = recordKey;
        this.dataType = dataType;
        this.source = source;
        this.startAtUtc = startAtUtc;
        this.endAtUtc = endAtUtc;
        this.sourceOffset = sourceOffset;
        this.measuredDate = measuredDate;
        this.steps = steps;
        this.calories = calories;
        this.distanceKm = distanceKm;
        this.dedupKey = dedupKey;
    }

    public static HealthRecord create(Long uploadId, String recordKey, String dataType, HealthSource source,
                                      Instant startAtUtc, Instant endAtUtc, String sourceOffset,
                                      LocalDate measuredDate, BigDecimal steps, BigDecimal calories,
                                      BigDecimal distanceKm, String dedupKey) {
        return new HealthRecord(uploadId, recordKey, dataType, source, startAtUtc, endAtUtc,
                sourceOffset, measuredDate, steps, calories, distanceKm, dedupKey);
    }
}
