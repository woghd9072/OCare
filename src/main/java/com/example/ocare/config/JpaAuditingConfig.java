package com.example.ocare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화.
 *
 * <p>{@code @EnableJpaAuditing} 을 메인 애플리케이션 클래스에 붙이지 않고 별도 설정 클래스로
 * 분리한 이유는, 이 기능이 필요 없는 테스트(예: 순수 파서 단위 테스트)에서
 * 설정을 골라 로딩할 수 있게 하기 위함이다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
