-- ============================================================================
-- V2. 수집 이력 및 건강활동 원본 데이터 테이블
--
-- 입력 데이터는 하나의 payload 안에 recordkey 1개와 10분 단위 측정 구간 수백~수천 건이 담긴다.
-- 이를 두 단계로 나눠 저장한다.
--   health_uploads : payload 단위의 수집 이력 (출처/단말/집계 결과)
--   health_records : 개별 측정 구간 (append-only)
--
-- 두 출처의 데이터 형식이 다르다는 점이 스키마 설계의 핵심 제약이다.
--   - 삼성헬스 : period 가 오프셋 없는 로컬 시각, steps 가 정수, calories 존재
--   - 애플 HealthKit : period 가 ISO8601 UTC, steps 가 소수를 담은 문자열, calories 가 항상 0
-- 서버는 이를 모두 UTC 로 정규화해 저장하고, 집계는 KST 기준 measured_date 로 수행한다.
-- ============================================================================

CREATE TABLE health_uploads
(
    id                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '수집 이력 식별자',
    record_key        VARCHAR(64) NOT NULL COMMENT '수집 대상 사용자 구분 키',
    data_type         VARCHAR(20) NOT NULL COMMENT 'payload 의 type 값. 현재는 steps 만 사용하며 향후 심박/수면 등으로 확장 가능',
    source_name       VARCHAR(30) NOT NULL COMMENT '데이터 출처. SamsungHealth 또는 Health Kit',
    source_mode       INT         NULL COMMENT 'payload 의 source.mode 값. 삼성 9, 애플 10 으로 관측됨',
    product_name      VARCHAR(50) NULL COMMENT '단말 제품명. Android 또는 iPhone',
    product_vendor    VARCHAR(50) NULL COMMENT '단말 제조사. Samsung 또는 Apple inc.',
    memo              VARCHAR(255) NULL COMMENT 'payload 의 memo 값. 애플 데이터에만 존재하며 대개 빈 문자열',
    last_update_at    DATETIME(3) NULL COMMENT 'payload 의 lastUpdate 를 UTC 로 정규화한 값. 단말이 마지막으로 데이터를 갱신한 시각',
    entry_count       INT         NOT NULL DEFAULT 0 COMMENT '수신한 측정 구간 건수',
    saved_count       INT         NOT NULL DEFAULT 0 COMMENT '실제로 저장된 건수',
    duplicated_count  INT         NOT NULL DEFAULT 0 COMMENT '이미 저장되어 있어 건너뛴 건수(재전송)',
    failed_count      INT         NOT NULL DEFAULT 0 COMMENT '형식 오류 등으로 저장하지 못한 건수',
    payload_hash      CHAR(64)    NOT NULL COMMENT 'payload 전체의 SHA-256. 동일 payload 재전송을 이력 단위에서 식별한다',
    created_at        DATETIME(6) NOT NULL COMMENT '레코드 생성 시각(서버 저장 시점)',
    updated_at        DATETIME(6) NOT NULL COMMENT '레코드 수정 시각(서버 저장 시점)',
    PRIMARY KEY (id),
    -- 같은 payload 가 다시 들어오면 이력을 새로 만들지 않고 기존 이력을 반환한다.
    UNIQUE KEY uk_health_uploads_payload_hash (payload_hash),
    KEY idx_health_uploads_record_key (record_key),
    CONSTRAINT fk_health_uploads_record_key FOREIGN KEY (record_key) REFERENCES record_keys (record_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='payload 단위 수집 이력';


CREATE TABLE health_records
(
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '측정 구간 식별자',
    upload_id     BIGINT        NOT NULL COMMENT '이 데이터가 들어온 수집 이력 ID',
    record_key    VARCHAR(64)   NOT NULL COMMENT '사용자 구분 키. 조회 시 조인을 피하기 위해 비정규화해 둔다',
    data_type     VARCHAR(20)   NOT NULL COMMENT '측정 항목. 현재는 steps',
    source_name   VARCHAR(30)   NOT NULL COMMENT '데이터 출처. 같은 구간이라도 출처가 다르면 별개 데이터로 취급한다',

    start_at_utc  DATETIME(3)   NOT NULL COMMENT '측정 구간 시작 시각(UTC 정규화). 삼성은 오프셋이 없어 기기 로컬(KST)로 간주해 변환한다',
    end_at_utc    DATETIME(3)   NOT NULL COMMENT '측정 구간 종료 시각(UTC 정규화). 삼성 데이터에는 start 와 동일한 0초 구간이 존재하며 이 또한 유효한 값이다',
    source_offset VARCHAR(6)    NULL COMMENT '원본에 표기된 UTC 오프셋(예: +0000). 오프셋이 없던 데이터는 NULL 로 두어 추정 여부를 구분한다',
    measured_date DATE          NOT NULL COMMENT '집계 기준일(KST). 일별/월별 집계는 항상 이 컬럼을 기준으로 한다',

    steps         DECIMAL(12, 6) NOT NULL COMMENT '걸음수. 애플 데이터는 구간을 안분한 소수값이 들어오므로 원본을 손실 없이 보존하고 반올림은 집계 시 1회만 수행한다',
    calories      DECIMAL(10, 4) NOT NULL COMMENT '소모 칼로리(kcal). 애플 HealthKit 은 값을 제공하지 않아 0 으로 들어온다',
    distance_km   DECIMAL(10, 6) NOT NULL COMMENT '이동거리(km). 원본 단위를 검증한 뒤 km 로 정규화해 저장한다',

    dedup_key     CHAR(64)      NOT NULL COMMENT 'record_key + source + type + 측정 구간의 SHA-256. 동일 구간 재전송을 차단하는 최종 방어선',
    created_at    DATETIME(6)   NOT NULL COMMENT '서버 저장 시각. 측정 시각과 구분된다',
    PRIMARY KEY (id),
    -- 애플리케이션이 Redis 로 1차 차단하더라도, 동시 요청이나 캐시 유실에 대비한 최종 방어선이 필요하다.
    UNIQUE KEY uk_health_records_dedup_key (dedup_key),
    -- 일별/월별 조회와 집계 갱신이 모두 이 조합으로 조회한다.
    KEY idx_health_records_key_date (record_key, measured_date),
    KEY idx_health_records_upload_id (upload_id),
    CONSTRAINT fk_health_records_upload FOREIGN KEY (upload_id) REFERENCES health_uploads (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='단말에서 수집한 10분 단위 측정 원본. 수정하지 않는 append-only 테이블';
