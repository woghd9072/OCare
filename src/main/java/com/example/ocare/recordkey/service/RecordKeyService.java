package com.example.ocare.recordkey.service;

import com.example.ocare.common.exception.BusinessException;
import com.example.ocare.common.exception.ErrorCode;
import com.example.ocare.health.HealthSource;
import com.example.ocare.member.entity.Member;
import com.example.ocare.member.repository.MemberRepository;
import com.example.ocare.recordkey.dto.RecordKeyRegisterRequest;
import com.example.ocare.recordkey.dto.RecordKeyResponse;
import com.example.ocare.recordkey.entity.RecordKey;
import com.example.ocare.recordkey.repository.RecordKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordKeyService {

    private final RecordKeyRepository recordKeyRepository;
    private final MemberRepository memberRepository;

    /**
     * 레코드키를 등록한다.
     *
     * <p>레코드키는 전역에서 유일하다. 같은 키가 두 회원에 매핑되면 수집 데이터의 주인이
     * 모호해지기 때문이다. 이미 등록된 키라면 그것이 본인 것이든 타인 것이든 같은 오류를 준다.
     */
    @Transactional
    public RecordKeyResponse register(Long memberId, RecordKeyRegisterRequest request) {
        HealthSource source = toHealthSource(request.source());

        if (recordKeyRepository.existsByRecordKey(request.recordKey())) {
            throw new BusinessException(ErrorCode.RECORD_KEY_DUPLICATED);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        RecordKey recordKey = RecordKey.register(
                member,
                request.recordKey(),
                source,
                request.productName(),
                request.productVendor()
        );

        try {
            return RecordKeyResponse.from(recordKeyRepository.saveAndFlush(recordKey));
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 키를 등록하는 요청이 선행 검사를 함께 통과한 경우 UNIQUE 제약이 막는다.
            throw new BusinessException(ErrorCode.RECORD_KEY_DUPLICATED);
        }
    }

    /**
     * 회원이 등록한 레코드키 목록.
     */
    @Transactional(readOnly = true)
    public List<RecordKeyResponse> findMine(Long memberId) {
        return recordKeyRepository.findAllByMemberId(memberId).stream()
                .map(RecordKeyResponse::from)
                .toList();
    }

    /**
     * 요청자가 소유한 레코드키를 조회한다. 소유자가 아니면 거부한다.
     *
     * <p>수집과 조회 API 가 공통으로 사용된다. 두 API 모두 레코드키를 요청 값으로 받는데,
     * 이 검사가 없으면 남의 레코드키를 넣어 타인의 건강 데이터를 저장하거나 열람할 수 있다.
     * 인증만으로는 막을 수 없고, "인증된 사람이 그 데이터의 주인인지"까지 확인해야 한다.
     *
     * <p>엔티티를 그대로 반환해 호출부가 출처나 활성 여부 같은 부가 정보를 함께 쓸 수 있게 한다.
     */
    @Transactional(readOnly = true)
    public RecordKey getOwnedRecordKey(Long memberId, String recordKey) {
        RecordKey found = recordKeyRepository.findByRecordKey(recordKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECORD_KEY_NOT_FOUND));

        if (!found.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.RECORD_KEY_FORBIDDEN);
        }
        return found;
    }

    /**
     * 단말이 보낸 출처 표기를 열거형으로 바꾼다.
     *
     * <p>Bean Validation 으로는 "알려진 출처인지"를 판단할 수 없어 여기서 확인한다.
     * 변환 실패를 그대로 두면 500 이 나가므로 400 으로 바꿔 준다.
     */
    private HealthSource toHealthSource(String source) {
        try {
            return HealthSource.fromPayloadName(source);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, e.getMessage(), e);
        }
    }
}
