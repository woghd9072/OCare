package com.example.ocare.health.entity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    long countByRecordKey(String recordKey);
}
