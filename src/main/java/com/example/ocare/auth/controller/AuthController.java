package com.example.ocare.auth.controller;

import com.example.ocare.auth.dto.LoginRequest;
import com.example.ocare.auth.dto.RefreshTokenRequest;
import com.example.ocare.auth.dto.TokenResponse;
import com.example.ocare.auth.security.AuthenticatedMember;
import com.example.ocare.auth.service.AuthService;
import com.example.ocare.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인. 이메일과 비밀번호가 일치하면 액세스/리프레시 토큰을 발급한다.
     */
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * 토큰 재발급. 액세스 토큰이 만료되었을 때 리프레시 토큰으로 새 토큰을 받는다.
     */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    /**
     * 로그아웃. 저장된 리프레시 토큰을 지워 더 이상 재발급되지 않게 한다.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal AuthenticatedMember member) {
        authService.logout(member.id());
        return ApiResponse.ok();
    }
}
