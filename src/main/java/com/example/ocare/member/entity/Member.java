package com.example.ocare.member.entity;

import com.example.ocare.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서비스 회원.
 *
 * <p>과제 요구사항의 회원가입 항목(이름, 닉네임, 이메일, 패스워드)을 담는다.
 */
@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "nickname", nullable = false, length = 50, unique = true)
    private String nickname;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    /**
     * 비밀번호 해시. 평문은 어떤 경우에도 저장하지 않는다.
     */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    private Member(String name, String nickname, String email, String encodedPassword) {
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.password = encodedPassword;
    }

    /**
     * 회원을 생성한다.
     *
     * @param encodedPassword 이미 암호화된 비밀번호 해시. 평문을 넘기면 안 된다.
     */
    public static Member create(String name, String nickname, String email, String encodedPassword) {
        return new Member(name, nickname, email, encodedPassword);
    }
}
