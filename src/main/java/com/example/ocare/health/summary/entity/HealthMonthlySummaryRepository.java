package com.example.ocare.health.summary.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthMonthlySummaryRepository extends JpaRepository<HealthMonthlySummary, Long> {

    /**
     * 레코드키와 기간으로 월별 집계를 조회한다.
     */
    List<HealthMonthlySummary> findAllByRecordKeyAndSummaryMonthBetweenOrderBySummaryMonthAsc(
            String recordKey, String from, String to);
}
