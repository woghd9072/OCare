package com.example.ocare.recordkey.repository;

import com.example.ocare.recordkey.entity.RecordKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecordKeyRepository extends JpaRepository<RecordKey, Long> {

    /**
     * 단말이 보낸 키로 매핑을 조회한다. 수집 요청이 들어올 때마다 사용한다.
     */
    Optional<RecordKey> findByRecordKey(String recordKey);

    boolean existsByRecordKey(String recordKey);

    /**
     * 회원이 등록한 레코드키 목록. 단말을 여러 대 쓰는 경우가 있어 목록으로 조회한다.
     */
    List<RecordKey> findAllByMemberId(Long memberId);
}
