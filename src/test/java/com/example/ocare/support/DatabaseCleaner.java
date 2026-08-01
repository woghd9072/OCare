package com.example.ocare.support;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 통합 테스트 사이에 데이터를 비운다.
 *
 * <p>테스트마다 각자 정리하게 두면, 뒤에 실행되는 테스트가 앞 테스트의 잔여 데이터 때문에
 * 실패한다. 실제로 수집 테스트가 남긴 레코드키 때문에 회원 테스트의 삭제가
 * 외래키 제약에 걸리는 문제가 있었다. 실행 순서에 따라 결과가 달라지면
 * 테스트를 믿을 수 없게 되므로, 정리 책임을 한 곳으로 모은다.
 *
 * <p>삭제 순서는 참조 관계의 역순이다. 자식 테이블을 먼저 지우지 않으면 제약에 걸린다.
 */
@Profile("test")
@Component
@RequiredArgsConstructor
public class DatabaseCleaner {

    /**
     * 참조하는 쪽부터 지운다. members 가 마지막이다.
     */
    private static final List<String> TABLES_IN_DELETE_ORDER = List.of(
            "health_records",
            "health_daily_summaries",
            "health_monthly_summaries",
            "health_uploads",
            "record_keys",
            "members"
    );

    private final JdbcTemplate jdbcTemplate;

    public void clean() {
        TABLES_IN_DELETE_ORDER.forEach(table -> jdbcTemplate.update("DELETE FROM " + table));
    }
}
