package com.example.ocare.auth.security;

import com.example.ocare.auth.jwt.InvalidTokenException;
import com.example.ocare.auth.jwt.JwtTokenProvider;
import com.example.ocare.auth.jwt.TokenType;
import com.example.ocare.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 요청 헤더의 액세스 토큰을 검증해 인증 정보를 채운다.
 *
 * <p>토큰이 아예 없는 요청은 그대로 통과시킨다. 회원가입처럼 인증이 필요 없는 경로가 있고,
 * 인증이 필요한 경로인지 판단하는 것은 {@code authorizeHttpRequests} 의 역할이기 때문이다.
 * 토큰이 없어 인증 정보가 비어 있으면 이후 단계에서 진입점이 401 을 내려준다.
 * 반대로 토큰이 있는데 유효하지 않으면 즉시 401 로 끊는다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityResponseWriter responseWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long memberId = jwtTokenProvider.getMemberId(token, TokenType.ACCESS);
            SecurityContextHolder.getContext().setAuthentication(authentication(memberId));
        } catch (InvalidTokenException e) {
            // 인증 정보가 남지 않도록 비운 뒤 응답한다.
            SecurityContextHolder.clearContext();
            responseWriter.write(response, ErrorCode.UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private UsernamePasswordAuthenticationToken authentication(Long memberId) {
        AuthenticatedMember principal = new AuthenticatedMember(memberId);
        // 이 서비스에는 회원 등급 구분이 없어 권한 목록을 비워 둔다.
        // 인증 여부만으로 접근을 통제한다.
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
