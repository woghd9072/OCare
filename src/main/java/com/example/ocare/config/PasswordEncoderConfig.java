package com.example.ocare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 암호화 설정.
 *
 * <p>기본 알고리즘은 BCrypt 이고, 접두어를 포함해 68자가 되므로
 * members.password 컬럼을 VARCHAR(100) 으로 잡아 두었다.
 *
 * <p>인코더만 별도 설정으로 분리한 이유는, 인증 필터 체인({@code SecurityFilterChain}) 구성은
 * 로그인 기능과 함께 다루는 것이 자연스럽기 때문이다. 회원가입 단계에서는 암호화만 필요하다.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
