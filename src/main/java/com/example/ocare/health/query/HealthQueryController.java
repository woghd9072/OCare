package com.example.ocare.health.query;

import com.example.ocare.auth.security.AuthenticatedMember;
import com.example.ocare.common.response.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 활동 데이터 조회.
 */
@Validated
@RestController
@RequestMapping("/api/v1/health-data")
@RequiredArgsConstructor
public class HealthQueryController {

    private final HealthQueryService healthQueryService;

    /**
     * 일별 활동 집계 조회.
     */
    @GetMapping("/daily")
    public ApiResponse<DailySummaryResult> findDaily(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam @NotBlank(message = "레코드키는 필수입니다.") String recordKey,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(healthQueryService.findDaily(member.id(), recordKey, from, to));
    }

    /**
     * 월별 활동 집계 조회.
     *
     * <p>{@code YearMonth} 는 Spring 이 기본으로 변환하지 못해 형식을 명시한다.
     * 명시하지 않으면 {@code 2024-11} 이 파싱되지 않아 400 이 난다.
     */
    @GetMapping("/monthly")
    public ApiResponse<MonthlySummaryResult> findMonthly(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam @NotBlank(message = "레코드키는 필수입니다.") String recordKey,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return ApiResponse.ok(healthQueryService.findMonthly(member.id(), recordKey, from, to));
    }
}
