package com.example.ocare.health.entity;

import com.example.ocare.common.entity.BaseTimeEntity;
import com.example.ocare.common.entity.UtcInstantConverter;
import com.example.ocare.health.HealthSource;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * payload 단위 수집 이력.
 */
@Getter
@Entity
@Table(name = "health_uploads")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthUpload extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_key", nullable = false, length = 64)
    private String recordKey;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @Column(name = "source_name", nullable = false, length = 30)
    private HealthSource source;

    @Column(name = "source_mode")
    private Integer sourceMode;

    @Column(name = "product_name", length = 50)
    private String productName;

    @Column(name = "product_vendor", length = 50)
    private String productVendor;

    @Column(name = "memo", length = 255)
    private String memo;

    @Convert(converter = UtcInstantConverter.class)
    @Column(name = "last_update_at")
    private Instant lastUpdateAt;

    @Column(name = "entry_count", nullable = false)
    private int entryCount;

    @Column(name = "saved_count", nullable = false)
    private int savedCount;

    @Column(name = "duplicated_count", nullable = false)
    private int duplicatedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    /**
     * payload 전체의 SHA-256. 같은 내용이 다시 오면 이 값으로 판별한다.
     */
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    private HealthUpload(String recordKey, String dataType, HealthSource source, Integer sourceMode,
                         String productName, String productVendor, String memo,
                         Instant lastUpdateAt, int entryCount, String payloadHash) {
        this.recordKey = recordKey;
        this.dataType = dataType;
        this.source = source;
        this.sourceMode = sourceMode;
        this.productName = productName;
        this.productVendor = productVendor;
        this.memo = memo;
        this.lastUpdateAt = lastUpdateAt;
        this.entryCount = entryCount;
        this.payloadHash = payloadHash;
    }

    public static HealthUpload start(String recordKey, String dataType, HealthSource source, Integer sourceMode,
                                     String productName, String productVendor, String memo,
                                     Instant lastUpdateAt, int entryCount, String payloadHash) {
        return new HealthUpload(recordKey, dataType, source, sourceMode, productName, productVendor,
                memo, lastUpdateAt, entryCount, payloadHash);
    }

    /**
     * 수집 결과를 기록한다. 저장이 끝난 뒤 한 번만 호출한다.
     */
    public void complete(int savedCount, int duplicatedCount, int failedCount) {
        this.savedCount = savedCount;
        this.duplicatedCount = duplicatedCount;
        this.failedCount = failedCount;
    }
}
