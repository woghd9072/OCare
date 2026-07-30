-- ============================================================================
-- V3. 일별 / 월별 집계 테이블
--
-- 과제 제출물의 "Daily/Monthly 레코드키 기준 조회"를 위한 테이블이다.
-- 원본(health_records)을 매번 GROUP BY 로 합산하지 않고 별도 테이블로 유지하는 이유는,
-- 조회가 recordkey + 기간 조합으로 반복 발생하는 반면 원본은 한 달에 수천 건씩 쌓이기 때문이다.
-- 수집 시점에 영향받은 날짜만 UPSERT 로 갱신한다.
--
-- 합계 컬럼을 DECIMAL 로 두는 이유:
-- 원본 데이터에는 0.29999998, 9.759994 처럼 float32 로 계산된 값이 그대로 들어 있다.
-- 이를 부동소수로 1,500건 누적하면 오차가 눈에 보이는 수준으로 커진다.
-- 저장과 집계는 DECIMAL 로 수행하고, 응답에서만 실수로 변환한다.
-- ============================================================================

CREATE TABLE health_daily_summaries
(
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '일별 집계 식별자',
    record_key         VARCHAR(64)    NOT NULL COMMENT '사용자 구분 키',
    summary_date       DATE           NOT NULL COMMENT '집계 기준일(KST). health_records.measured_date 와 같은 기준이다',

    steps              INT            NOT NULL COMMENT '해당 일자의 총 걸음수. 소수 걸음을 모두 더한 뒤 마지막에 한 번만 반올림한다. 구간별로 반올림하면 하루 수천 걸음의 오차가 발생한다',
    calories           DECIMAL(12, 4) NOT NULL COMMENT '해당 일자의 총 소모 칼로리(kcal)',
    distance_km        DECIMAL(12, 6) NOT NULL COMMENT '해당 일자의 총 이동거리(km)',

    entry_count        INT            NOT NULL COMMENT '집계에 사용된 측정 구간 건수. 데이터 누락 여부를 판단하는 근거가 된다',
    calories_supported TINYINT(1)     NOT NULL DEFAULT 1 COMMENT '출처가 칼로리를 제공하는지 여부. 애플 HealthKit 은 항상 0 을 보내므로, 활동이 없어서 0 인 것과 값을 제공하지 않아 0 인 것을 구분한다',
    last_aggregated_at DATETIME(6)    NOT NULL COMMENT '마지막으로 집계를 갱신한 시각',

    created_at         DATETIME(6)    NOT NULL COMMENT '레코드 생성 시각(서버 저장 시점)',
    updated_at         DATETIME(6)    NOT NULL COMMENT '레코드 수정 시각(서버 저장 시점)',
    PRIMARY KEY (id),
    -- UPSERT(INSERT ... ON DUPLICATE KEY UPDATE)의 충돌 판정 기준이 되는 제약이다.
    UNIQUE KEY uk_daily_summaries_key_date (record_key, summary_date),
    CONSTRAINT fk_daily_summaries_record_key FOREIGN KEY (record_key) REFERENCES record_keys (record_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='레코드키별 일간 활동 집계';


CREATE TABLE health_monthly_summaries
(
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '월별 집계 식별자',
    record_key         VARCHAR(64)    NOT NULL COMMENT '사용자 구분 키',
    summary_month      CHAR(7)        NOT NULL COMMENT '집계 기준월(KST), YYYY-MM 형식. 문자열 비교만으로 기간 범위 조회가 가능하다',

    steps              BIGINT         NOT NULL COMMENT '해당 월의 총 걸음수. 일별 합계를 누적하므로 INT 범위를 넘을 수 있어 BIGINT 로 둔다',
    calories           DECIMAL(14, 4) NOT NULL COMMENT '해당 월의 총 소모 칼로리(kcal)',
    distance_km        DECIMAL(14, 6) NOT NULL COMMENT '해당 월의 총 이동거리(km)',

    active_days        INT            NOT NULL COMMENT '측정 데이터가 존재한 일수. 월 전체 일수와 달라 활동 밀도를 판단하는 근거가 된다',
    entry_count        INT            NOT NULL COMMENT '집계에 사용된 측정 구간 건수',
    calories_supported TINYINT(1)     NOT NULL DEFAULT 1 COMMENT '출처가 칼로리를 제공하는지 여부',
    last_aggregated_at DATETIME(6)    NOT NULL COMMENT '마지막으로 집계를 갱신한 시각',

    created_at         DATETIME(6)    NOT NULL COMMENT '레코드 생성 시각(서버 저장 시점)',
    updated_at         DATETIME(6)    NOT NULL COMMENT '레코드 수정 시각(서버 저장 시점)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_summaries_key_month (record_key, summary_month),
    CONSTRAINT fk_monthly_summaries_record_key FOREIGN KEY (record_key) REFERENCES record_keys (record_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='레코드키별 월간 활동 집계';
