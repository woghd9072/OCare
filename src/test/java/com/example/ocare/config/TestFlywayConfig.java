package com.example.ocare.config;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 테스트 전용 Flyway 실행 전략.
 *
 * <p>기본 동작은 "아직 적용되지 않은 마이그레이션만 실행"이라, 이전 테스트가 남긴 데이터가
 * 그대로 남는다. 집계 정확도 테스트는 실제 입력 데이터의 합계(골든값)와 비교하므로
 * 잔여 데이터가 있으면 틀린 결과로 실패한다.
 *
 * <p>그래서 테스트에서는 스키마를 비우고(clean) 처음부터 다시 쌓는다(migrate).
 * {@code @Profile("test")} 로 묶여 있어 개발/운영 실행에는 적용되지 않으며,
 * 대상 스키마도 {@code ocare_test} 로 분리되어 있다.
 */
@Profile("test")
@Configuration
public class TestFlywayConfig {

    @Bean
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
