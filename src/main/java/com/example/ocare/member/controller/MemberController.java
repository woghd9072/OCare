package com.example.ocare.member.controller;

import com.example.ocare.common.response.ApiResponse;
import com.example.ocare.member.dto.SignupRequest;
import com.example.ocare.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 회원가입.
     *
     * <p>{@code @Valid} 를 붙여 컨트롤러 진입 전에 요청 값 검증이 끝나게 한다.
     * 검증 실패는 전역 예외 처리기가 400 과 필드별 오류 목록으로 변환한다.
     */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
        memberService.signup(request);
        return ApiResponse.ok();
    }
}
