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
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 레코드키별 월간 활동 집계. 일별과 마찬가지로 조회 전용이다.
 */
@Getter
@Entity
@Table(name = "health_monthly_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthMonthlySummary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_key", nullable = false, length = 64)
    private String recordKey;

    /**
     * 집계 기준월(KST), {@code YYYY-MM} 형식.
     *
     * <p>문자열로 두면 사전순 비교가 곧 시간순 비교가 되어 기간 조회 조건이 단순해진다.
     */
    @Column(name = "summary_month", nullable = false, length = 7)
    private String summaryMonth;

    @Column(name = "steps", nullable = false)
    private long steps;

    @Column(name = "calories", nullable = false, precision = 14, scale = 4)
    private BigDecimal calories;

    @Column(name = "distance_km", nullable = false, precision = 14, scale = 6)
    private BigDecimal distanceKm;

    /**
     * 측정 데이터가 존재한 일수. 월 전체 일수와 달라 활동 밀도를 판단하는 근거가 된다.
     */
    @Column(name = "active_days", nullable = false)
    private int activeDays;

    @Column(name = "entry_count", nullable = false)
    private int entryCount;

    @Column(name = "calories_supported", nullable = false)
    private boolean caloriesSupported;

    @Column(name = "last_aggregated_at", nullable = false)
    private LocalDateTime lastAggregatedAt;

    public YearMonth month() {
        return YearMonth.parse(summaryMonth);
    }
}
