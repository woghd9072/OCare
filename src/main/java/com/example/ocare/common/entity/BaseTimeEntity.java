package com.example.ocare.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 생성/수정 시각을 자동으로 관리하는 엔티티 공통 상위 클래스.
 *
 * <p>수집 데이터는 언제 들어왔는지가 추적의 기본이 되므로 모든 테이블이 이 두 컬럼을 갖는다.
 * 애플리케이션이 직접 시각을 넣지 않고 Auditing 에 맡기는 이유는, 여러 경로(수집 API / 집계 갱신)
 * 에서 저장이 일어날 때 누락되거나 서로 다른 기준의 시각이 섞이는 것을 막기 위함이다.
 *
 * <p>타입은 {@link LocalDateTime} 을 쓰지만, JDBC 타임존을 {@code Asia/Seoul} 로 고정해 두었으므로
 * 저장/조회 시 KST 기준으로 일관되게 다뤄진다.
 *
 * <p>측정 데이터 자체의 시각(측정 구간, 집계 기준일)은 이 컬럼과 별개다.
 * 이 값은 "서버가 언제 저장했는지", 측정 시각은 "단말이 언제 측정했는지"를 뜻한다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
