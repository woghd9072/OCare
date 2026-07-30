package com.example.ocare.config;

import com.example.ocare.auth.security.JwtAccessDeniedHandler;
import com.example.ocare.auth.security.JwtAuthenticationEntryPoint;
import com.example.ocare.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 보안 필터 체인 설정.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // 토큰 기반 무상태 API 라 세션 쿠키를 쓰지 않으므로 CSRF 방어가 필요 없다.
            .httpBasic(AbstractHttpConfigurer::disable) // 브라우저 기본 인증창이나 로그인 폼을 띄우지 않는다. 단말 앱이 호출하는 API 이기 때문이다.
            .formLogin(AbstractHttpConfigurer::disable)
            // 인증 상태를 서버 세션에 보관하지 않는다.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 필터 단계의 실패는 @RestControllerAdvice 가 잡지 못하므로 여기서 응답 형식을 맞춘다.
            .exceptionHandling(handling -> handling
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                    // 인증 이전 단계이거나(가입/로그인), 액세스 토큰이 만료된 상태에서
                    // 호출해야 하는(재발급) 경로만 열어 둔다.
                    // 로그아웃은 누구의 세션을 끊을지 알아야 하므로 인증이 필요하다.
                    .requestMatchers(
                            "/api/v1/auth/signup",
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh"
                    ).permitAll()
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
