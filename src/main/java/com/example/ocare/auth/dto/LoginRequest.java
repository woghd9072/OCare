package com.example.ocare.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청. 과제 요구사항대로 이메일과 패스워드로 인증한다.
 *
 * <p>가입 때와 달리 형식 제약(@Email, 길이, 패턴)을 두지 않는다.
 * 로그인은 저장된 값과 일치하는지만 확인하면 되고, 형식 오류를 별도로 알려주면
 * "이 이메일은 존재하지 않는다"는 정보를 흘리는 셈이 되기 때문이다.
 */
public record LoginRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
