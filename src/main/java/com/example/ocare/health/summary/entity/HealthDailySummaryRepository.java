package com.example.ocare.health.summary.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HealthDailySummaryRepository extends JpaRepository<HealthDailySummary, Long> {

    /**
     * 레코드키와 기간으로 일별 집계를 조회한다.
     *
     * <p>{@code (record_key, summary_date)} 유니크 인덱스를 그대로 타므로
     * 원본을 훑지 않고 필요한 행만 읽는다.
     */
    List<HealthDailySummary> findAllByRecordKeyAndSummaryDateBetweenOrderBySummaryDateAsc(
            String recordKey, LocalDate from, LocalDate to);
}
