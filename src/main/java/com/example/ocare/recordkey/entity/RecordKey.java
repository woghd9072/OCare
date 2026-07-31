package com.example.ocare.recordkey.entity;

import com.example.ocare.common.entity.BaseTimeEntity;
import com.example.ocare.health.HealthSource;
import com.example.ocare.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원과 단말 사용자 구분 키(recordkey)의 매핑.
 *
 * <p>단말이 수집 데이터를 보낼 때 회원 정보를 담지 않고 recordkey 만 보내므로,
 * 서버는 이 매핑을 통해 데이터의 주인을 판단한다.
 *
 * <p>회원 1명이 여러 개를 가질 수 있다. 삼성헬스와 애플 건강을 동시에 쓰거나,
 * 단말을 교체해 새 키를 발급받는 경우가 있기 때문이다.
 */
@Getter
@Entity
@Table(name = "record_keys")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordKey extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소유 회원.
     *
     * <p>지연 로딩으로 두는 이유는 수집·조회 흐름에서 회원 정보 자체가 필요한 경우가 드물기 때문이다.
     * 대부분은 "이 레코드키가 요청자의 것인가"만 확인하면 되고, 이는 식별자 비교로 끝난다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "record_key", nullable = false, length = 64, unique = true)
    private String recordKey;

    @Column(name = "source_name", nullable = false, length = 30)
    private HealthSource source;

    @Column(name = "product_name", length = 50)
    private String productName;

    @Column(name = "product_vendor", length = 50)
    private String productVendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecordKeyStatus status;

    private RecordKey(Member member, String recordKey, HealthSource source,
                      String productName, String productVendor) {
        this.member = member;
        this.recordKey = recordKey;
        this.source = source;
        this.productName = productName;
        this.productVendor = productVendor;
        this.status = RecordKeyStatus.ACTIVE;
    }

    public static RecordKey register(Member member, String recordKey, HealthSource source,
                                     String productName, String productVendor) {
        return new RecordKey(member, recordKey, source, productName, productVendor);
    }

    /**
     * 이 레코드키가 해당 회원의 것인지 확인한다.
     *
     * <p>회원 엔티티를 로딩하지 않도록 식별자만 비교한다.
     */
    public boolean isOwnedBy(Long memberId) {
        return member.getId().equals(memberId);
    }

    public boolean isActive() {
        return status == RecordKeyStatus.ACTIVE;
    }

    /**
     * 수집을 중단한다. 과거 데이터를 보존하기 위해 삭제하지 않고 상태만 바꾼다.
     */
    public void deactivate() {
        this.status = RecordKeyStatus.INACTIVE;
    }
}
