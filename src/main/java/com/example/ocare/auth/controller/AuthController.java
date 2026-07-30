package com.example.ocare.auth.controller;

import com.example.ocare.auth.dto.LoginRequest;
import com.example.ocare.auth.dto.TokenResponse;
import com.example.ocare.auth.service.AuthService;
import com.example.ocare.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
