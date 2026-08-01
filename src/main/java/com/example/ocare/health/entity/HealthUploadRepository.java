package com.example.ocare.health.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthUploadRepository extends JpaRepository<HealthUpload, Long> {

    /**
     * 같은 내용의 payload 가 이미 수집됐는지 확인한다.
     * 재전송이면 새 이력을 만들지 않고 기존 이력을 그대로 돌려준다.
     */
    Optional<HealthUpload> findByPayloadHash(String payloadHash);
}
