package com.example.ocare.health.ingest;

import com.example.ocare.auth.security.AuthenticatedMember;
import com.example.ocare.common.response.ApiResponse;
import com.example.ocare.health.payload.HealthPayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 건강활동 데이터 수집.
 *
 * <p>단말이 App to App 으로 받은 데이터를 그대로 전달하는 창구다.
 * 요청 본문의 recordkey 가 요청자의 것인지 확인한 뒤에만 저장한다.
 */
@RestController
@RequestMapping("/api/v1/health-data")
@RequiredArgsConstructor
public class HealthIngestController {

    private final HealthIngestService healthIngestService;

    /**
     * 측정 데이터를 수집한다.
     *
     * <p>재전송은 오류가 아니라 정상 흐름이므로 200 으로 응답하고,
     * 실제로 저장된 건수와 건너뛴 건수를 결과에 담아 단말이 스스로 확인할 수 있게 한다.
     */
    @PostMapping
    public ApiResponse<HealthIngestResponse> ingest(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody HealthPayload payload) {
        return ApiResponse.ok(healthIngestService.ingest(member.id(), payload));
    }
}
