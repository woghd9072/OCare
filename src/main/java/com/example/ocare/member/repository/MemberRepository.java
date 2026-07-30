package com.example.ocare.member.repository;

import com.example.ocare.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 이메일 중복 확인용.
     */
    boolean existsByEmail(String email);

    /**
     * 닉네임 중복 확인용.
     */
    boolean existsByNickname(String nickname);

    /**
     * 로그인 시 이메일로 회원을 조회한다.
     */
    Optional<Member> findByEmail(String email);
}
