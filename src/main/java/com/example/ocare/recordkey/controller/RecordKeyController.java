package com.example.ocare.recordkey.controller;

import com.example.ocare.auth.security.AuthenticatedMember;
import com.example.ocare.common.response.ApiResponse;
import com.example.ocare.recordkey.dto.RecordKeyRegisterRequest;
import com.example.ocare.recordkey.dto.RecordKeyResponse;
import com.example.ocare.recordkey.service.RecordKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 레코드키 등록·조회.
 *
 * <p>모든 엔드포인트가 인증을 요구한다. 레코드키는 회원에 귀속되는 정보이고,
 * 누구의 것인지는 요청 값이 아니라 인증 정보로만 판단한다.
 */
@RestController
@RequestMapping("/api/v1/record-keys")
@RequiredArgsConstructor
public class RecordKeyController {

    private final RecordKeyService recordKeyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecordKeyResponse> register(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody RecordKeyRegisterRequest request) {
        return ApiResponse.ok(recordKeyService.register(member.id(), request));
    }

    /**
     * 내가 등록한 레코드키 목록.
     *
     * <p>단말을 여러 대 연동한 경우를 위해 List로 반환한다.
     */
    @GetMapping
    public ApiResponse<List<RecordKeyResponse>> findMine(
            @AuthenticationPrincipal AuthenticatedMember member) {
        return ApiResponse.ok(recordKeyService.findMine(member.id()));
    }
}
