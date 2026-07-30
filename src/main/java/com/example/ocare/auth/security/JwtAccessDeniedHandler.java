package com.example.ocare.auth.security;

import com.example.ocare.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증은 되었으나 권한이 없는 요청에 대한 응답.
 *
 * <p>401 과 구분해 403 을 내려주는 이유는, 클라이언트가 "다시 로그인하면 되는 상황"과
 * "다시 로그인해도 접근할 수 없는 상황"을 다르게 처리해야 하기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityResponseWriter responseWriter;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        responseWriter.write(response, ErrorCode.FORBIDDEN);
    }
}
