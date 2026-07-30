package com.example.oncare;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 테스트는 항상 test 프로파일로 실행한다.
 * 개발용 스키마(oncare)가 아니라 테스트 전용 스키마(oncare_test)와 Redis DB 1 을 바라보게 하기 위함이다.
 */
@SpringBootTest
@ActiveProfiles("test")
class OncareApplicationTests {

	@Test
	void contextLoads() {
	}

}
