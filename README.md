# OCare — 건강활동 데이터 수집 백엔드

삼성헬스 / 애플 건강(HealthKit)이 App to App 으로 단말에 전달한 걸음수·칼로리·이동거리 데이터를
서버에서 수집·저장하고, 사용자 구분 키(`recordkey`) 기준으로 일별/월별 집계를 조회하는 백엔드입니다.

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| 언어 / 런타임 | Java 17 |
| 프레임워크 | Spring Boot 4.1.0, Spring Data JPA, Spring Security |
| 데이터베이스 | MySQL 8.0 |
| 캐시 / 인메모리 | Redis 7 |
| 마이그레이션 | Flyway |
| 빌드 | Gradle (Wrapper 포함) |

## 사전 준비

- JDK 17 이상 (Gradle toolchain 이 17 을 사용합니다)
- Docker / Docker Compose

## 로컬 실행

### 1. MySQL · Redis 기동

```bash
docker compose up -d
```

`docker-compose.yml` 이 아래 두 컨테이너를 띄웁니다.

| 컨테이너 | 포트 | 비고 |
|---|---|---|
| `ocare-mysql` | 3306 | 타임존 `Asia/Seoul`, charset `utf8mb4` |
| `ocare-redis` | 6379 | |

최초 기동 시 `docker/mysql/init.sql` 이 실행되어 스키마 두 개가 생성됩니다.

- `ocare` — 애플리케이션용
- `ocare_test` — 테스트 전용

기동 상태는 아래로 확인합니다.

```bash
docker compose ps
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 테스트 실행

```bash
./gradlew test
```

테스트는 `@ActiveProfiles("test")` 로 실행되어 `ocare_test` 스키마와 Redis DB 1 번을 사용합니다.
개발 중이던 데이터가 테스트에 오염되거나, 반대로 테스트가 개발 데이터를 지우는 일을 막기 위함입니다.

## 설정

접속 정보는 모두 환경변수로 덮어쓸 수 있습니다. 미지정 시 아래 기본값이 적용됩니다.

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/ocare?...` | JDBC URL |
| `MYSQL_USERNAME` | `root` | DB 계정 |
| `MYSQL_PASSWORD` | `root` | DB 비밀번호 |
| `REDIS_HOST` | `localhost` | Redis 호스트 |
| `REDIS_PORT` | `6379` | Redis 포트 |

테스트용으로는 `TEST_MYSQL_URL`, `TEST_MYSQL_USERNAME`, `TEST_MYSQL_PASSWORD`,
`TEST_REDIS_HOST`, `TEST_REDIS_PORT` 를 사용합니다.

### 타임존

수집 데이터의 집계 기준일(`measured_date`)이 KST 기준이므로, 타임존을 세 곳에서 일관되게 고정합니다.

1. MySQL 컨테이너 — `TZ=Asia/Seoul`, `--default-time-zone=+09:00`
2. JDBC URL — `serverTimezone=Asia/Seoul`
3. Hibernate — `spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Seoul`

한 곳이라도 어긋나면 일별 집계 경계가 하루씩 밀립니다.

### 스키마 관리

스키마의 소유권은 Flyway 마이그레이션에 있으며, Hibernate 는 `ddl-auto=validate` 로 검증만 수행합니다.
`DECIMAL` 정밀도나 `UNIQUE` 제약이 Hibernate 의 자동 DDL 로 어긋나는 것을 막기 위함입니다.

## 문서

> 구현이 진행되면서 아래 문서가 추가됩니다.

- API 명세
- ERD 및 테이블 설명
- Daily / Monthly 조회 결과
- 구현 이슈 및 해결 방법
