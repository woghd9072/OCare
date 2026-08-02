package com.example.ocare.auth.security;

import com.example.ocare.common.exception.ErrorCode;
import com.example.ocare.common.response.ApiError;
import com.example.ocare.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 보안 필터 단계에서 발생한 실패를 응답으로 쓴다.
 *
 * <p>인증 실패는 {@code DispatcherServlet} 에 도달하기 전 필터에서 발생하므로
 * {@code @RestControllerAdvice} 가 잡지 못한다. 그대로 두면 이 경로만 Spring 기본 응답이 나가
 * 클라이언트가 두 가지 오류 형식을 다뤄야 한다. 그래서 여기서 직접 같은 포맷으로 쓴다.
 */
@Component
@RequiredArgsConstructor
public class SecurityResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<Void> body = ApiResponse.fail(
                ApiError.of(errorCode.code(), errorCode.getMessage())
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
