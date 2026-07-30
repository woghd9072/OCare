-- MySQL 컨테이너 최초 기동 시 1회 실행된다.
-- 개발용 스키마(ocare)는 compose 의 MYSQL_DATABASE 환경변수로 생성되므로,
-- 여기서는 테스트 전용 스키마만 추가로 생성한다.
-- 테스트는 매 실행마다 Flyway clean + migrate 로 이 스키마를 초기화하기 때문에
-- 개발용 스키마와 반드시 분리되어야 한다.
CREATE DATABASE IF NOT EXISTS ocare_test
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
