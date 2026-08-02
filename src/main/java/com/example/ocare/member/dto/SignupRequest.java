package com.example.ocare.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청.
 *
 * <p>길이 제한은 스키마와 맞춰 두었다. 검증을 통과했는데 저장 단계에서
 * 길이 초과로 실패하면 사용자에게 원인을 설명할 수 없기 때문이다.
 */
public record SignupRequest(

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다.")
        String nickname,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        /*
         * 비밀번호는 해시로 저장되므로 컬럼 길이와 무관하지만, 지나치게 긴 입력은
         * BCrypt 연산 비용만 키우므로 상한을 둔다.
         * BCrypt 는 72바이트를 넘는 입력을 잘라내므로 그 이전에서 제한하는 편이 안전하다.
         */
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "비밀번호는 영문자와 숫자를 모두 포함해야 합니다."
        )
        String password
) {
}
