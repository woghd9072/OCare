package com.example.ocare.member.service;

import com.example.ocare.common.exception.BusinessException;
import com.example.ocare.common.exception.ErrorCode;
import com.example.ocare.member.dto.SignupRequest;
import com.example.ocare.member.entity.Member;
import com.example.ocare.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_members_email";
    private static final String NICKNAME_UNIQUE_CONSTRAINT = "uk_members_nickname";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원을 가입시킨다.
     *
     * <p>중복 검사를 두 겹으로 둔다.
     * <ul>
     *   <li>선행 조회: 어떤 항목이 중복인지 사용자에게 정확히 알려주기 위함</li>
     *   <li>DB UNIQUE 제약: 동시에 들어온 두 요청이 선행 조회를 함께 통과하는 경우의 최종 방어선</li>
     * </ul>
     * 선행 조회만으로는 동시성 문제를 막을 수 없고, 제약 위반만으로는 어떤 항목이 문제인지
     * 안내하기 어렵기 때문에 둘 다 필요하다.
     */
    @Transactional
    public void signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        if (memberRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        }

        Member member = Member.create(
                request.name(),
                request.nickname(),
                request.email(),
                passwordEncoder.encode(request.password())
        );

        try {
            // save() 는 트랜잭션 커밋 시점에 INSERT 될 수 있어, 이 블록에서 처리하려면 즉시 flush 해야 한다.
            memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            throw toDuplicationException(e);
        }
    }

    /**
     * 제약 위반이 어느 컬럼에서 났는지 예외 메시지의 제약 이름으로 판별한다.
     *
     * <p>판별할 수 없는 위반은 우리가 예상한 상황이 아니므로 그대로 던져
     * 전역 예외 처리기가 서버 오류로 다루게 한다. 임의로 중복으로 단정하면
     * 실제 원인이 로그에서 사라진다.
     */
    private RuntimeException toDuplicationException(DataIntegrityViolationException e) {
        String cause = e.getMostSpecificCause().getMessage();
        if (cause == null) {
            return e;
        }
        if (cause.contains(EMAIL_UNIQUE_CONSTRAINT)) {
            return new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        if (cause.contains(NICKNAME_UNIQUE_CONSTRAINT)) {
            return new BusinessException(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        }
        return e;
    }
}
