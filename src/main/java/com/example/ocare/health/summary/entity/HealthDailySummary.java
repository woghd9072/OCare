package com.example.ocare.health.summary.entity;

import com.example.ocare.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 레코드키별 일간 활동 집계.
 *
 * <p>조회 전용 엔티티다. 값을 만드는 것은 원본을 다시 합산하는 UPSERT 구문이고,
 * 이 클래스는 그 결과를 읽기만 한다. 두 경로가 같은 데이터를 서로 다르게 계산하면
 * 어느 쪽이 맞는지 알 수 없게 되므로, 쓰기 책임을 여기에 두지 않는다.
 */
@Getter
@Entity
@Table(name = "health_daily_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthDailySummary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_key", nullable = false, length = 64)
    private String recordKey;

    /**
     * 집계 기준일(KST).
     */
    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    /**
     * 총 걸음수. 소수 걸음을 모두 더한 뒤 한 번만 반올림한 값이다.
     */
    @Column(name = "steps", nullable = false)
    private int steps;

    @Column(name = "calories", nullable = false, precision = 12, scale = 4)
    private BigDecimal calories;

    @Column(name = "distance_km", nullable = false, precision = 12, scale = 6)
    private BigDecimal distanceKm;

    /**
     * 집계에 사용된 측정 구간 수. 값이 비어 보일 때 데이터가 없는 것인지 판단하는 근거가 된다.
     */
    @Column(name = "entry_count", nullable = false)
    private int entryCount;

    /**
     * 출처가 칼로리를 제공하는지 여부.
     * 애플 HealthKit 은 항상 0 을 보내므로, 활동이 없어서 0 인 것과 구분하기 위해 둔다.
     */
    @Column(name = "calories_supported", nullable = false)
    private boolean caloriesSupported;

    @Column(name = "last_aggregated_at", nullable = false)
    private LocalDateTime lastAggregatedAt;
}
