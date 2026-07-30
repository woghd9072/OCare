-- ============================================================================
-- V1. 회원 및 레코드키 매핑 테이블
--
-- recordkey 는 단말이 전송하는 "사용자 구분 키"다. 이를 members 의 컬럼으로 두지 않고
-- 별도 테이블로 분리한 이유는 다음과 같다.
--   1. 한 회원이 삼성헬스(Android)와 애플 건강(iPhone)을 동시에 사용할 수 있다.
--   2. 단말을 교체하면 새 recordkey 가 발급되는데, 과거 데이터는 이전 키에 남아 있어야 한다.
-- 실제 입력 데이터에서도 서로 다른 recordkey 4개가 각각 다른 출처(삼성 2 / 애플 2)로 들어온다.
-- ============================================================================

CREATE TABLE members
(
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원 식별자',
    name       VARCHAR(50)  NOT NULL COMMENT '회원 이름',
    nickname   VARCHAR(50)  NOT NULL COMMENT '닉네임. 서비스 내 표시 이름이며 중복될 수 없다',
    email      VARCHAR(255) NOT NULL COMMENT '로그인 ID 로 사용하는 이메일',
    password   VARCHAR(100) NOT NULL COMMENT '비밀번호 해시. 평문은 저장하지 않는다. BCrypt 결과 60자에 더해 {bcrypt} 같은 알고리즘 식별자 접두어가 붙을 수 있어 여유를 둔다',
    created_at DATETIME(6)  NOT NULL COMMENT '레코드 생성 시각(서버 저장 시점)',
    updated_at DATETIME(6)  NOT NULL COMMENT '레코드 수정 시각(서버 저장 시점)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_members_email (email),
    UNIQUE KEY uk_members_nickname (nickname)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='서비스 회원';


CREATE TABLE record_keys
(
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '레코드키 매핑 식별자',
    member_id      BIGINT      NOT NULL COMMENT '이 레코드키를 소유한 회원 ID',
    record_key     VARCHAR(64) NOT NULL COMMENT '단말이 전송하는 사용자 구분 키. 입력 데이터에서는 UUID 형식이다',
    source_name    VARCHAR(30) NOT NULL COMMENT '데이터 출처. 입력 데이터 기준 SamsungHealth 또는 Health Kit',
    product_name   VARCHAR(50) NULL COMMENT '단말 제품명. Android 또는 iPhone',
    product_vendor VARCHAR(50) NULL COMMENT '단말 제조사. Samsung 또는 Apple inc.',
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '수집 상태. ACTIVE=수집 대상, INACTIVE=단말 교체 등으로 수집 중단',
    created_at     DATETIME(6) NOT NULL COMMENT '레코드 생성 시각(서버 저장 시점)',
    updated_at     DATETIME(6) NOT NULL COMMENT '레코드 수정 시각(서버 저장 시점)',
    PRIMARY KEY (id),
    -- 레코드키는 전역에서 유일해야 한다. 같은 키가 두 회원에 매핑되면 수집 데이터의 귀속이 모호해진다.
    UNIQUE KEY uk_record_keys_record_key (record_key),
    KEY idx_record_keys_member_id (member_id),
    CONSTRAINT fk_record_keys_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='회원과 단말 사용자 구분 키(recordkey)의 매핑';
