# API 명세

Base URL: `http://localhost:8080/api/v1`

## 공통 응답 형식

성공과 실패의 모양을 하나로 통일합니다. HTTP 상태 코드만으로는 "무엇이 왜 실패했는지"를
전달하기 어려워, 본문에 성공 여부와 오류 코드를 함께 담습니다.

**성공**

```json
{ "success": true, "data": { } }
```

**실패**

```json
{
  "success": false,
  "error": {
    "code": "MEMBER_EMAIL_DUPLICATED",
    "message": "이미 사용 중인 이메일입니다.",
    "fieldErrors": [
      { "field": "email", "message": "이메일 형식이 올바르지 않습니다." }
    ]
  }
}
```

`fieldErrors` 는 요청 값 검증에 실패했을 때만 담깁니다.
`code` 는 클라이언트가 분기에 사용하는 식별자이고, `message` 는 사람이 읽는 설명입니다.
메시지 문구가 바뀌어도 클라이언트 로직이 깨지지 않도록 둘을 분리했습니다.

값이 없는 필드는 응답에서 생략됩니다(`data`, `error`, `fieldErrors`).

## 오류 코드

| 코드 | 상태 | 설명 |
|---|---|---|
| `INVALID_REQUEST` | 400 | 요청 값 오류. 검증 실패, 형식 오류, 지원하지 않는 출처·단위 |
| `UNAUTHORIZED` | 401 | 인증 필요. 토큰 없음·만료·위조·용도 불일치 |
| `LOGIN_FAILED` | 401 | 이메일 또는 비밀번호 불일치 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `RECORD_KEY_FORBIDDEN` | 403 | 본인이 등록한 레코드키가 아님 |
| `NOT_FOUND` | 404 | 리소스 없음 |
| `RECORD_KEY_NOT_FOUND` | 404 | 등록되지 않은 레코드키 |
| `MEMBER_EMAIL_DUPLICATED` | 409 | 이미 가입된 이메일 |
| `MEMBER_NICKNAME_DUPLICATED` | 409 | 이미 사용 중인 닉네임 |
| `RECORD_KEY_DUPLICATED` | 409 | 이미 등록된 레코드키 |
| `RECORD_KEY_INACTIVE` | 409 | 수집이 중단된 레코드키 |
| `INTERNAL_ERROR` | 500 | 서버 오류 |

## 인증

로그인으로 받은 액세스 토큰을 헤더에 담습니다.

```
Authorization: Bearer {accessToken}
```

| 경로 | 인증 |
|---|---|
| `POST /auth/signup` | 불필요 |
| `POST /auth/login` | 불필요 |
| `POST /auth/refresh` | 불필요 (리프레시 토큰 자체가 인증 수단) |
| 그 외 전부 | **필요** |

`/auth/refresh` 에 인증을 요구하지 않는 이유는, 액세스 토큰이 만료된 상황을 해결하기 위한
요청이기 때문입니다. 인증을 요구하면 모순이 됩니다.

---

# 회원 · 인증

## 회원가입

```
POST /api/v1/auth/signup
```

| 필드 | 타입 | 제약 |
|---|---|---|
| `name` | string | 필수, 50자 이하 |
| `nickname` | string | 필수, 2~50자, 중복 불가 |
| `email` | string | 필수, 이메일 형식, 255자 이하, 중복 불가 |
| `password` | string | 필수, 8~64자, 영문자와 숫자 포함 |

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"name":"홍길동","nickname":"gildong","email":"gildong@example.com","password":"ocare1234"}'
```

**201 Created**

```json
{ "success": true }
```

응답 본문에 회원 정보를 담지 않습니다. 클라이언트가 방금 입력한 값이고,
이후 흐름(로그인은 이메일, 그 뒤는 토큰의 인증 주체)에서 회원 식별자를 쓰지 않기 때문입니다.

비밀번호 상한이 64자인 이유는 BCrypt 가 72바이트를 넘는 입력을 조용히 잘라내기 때문입니다.

## 로그인

```
POST /api/v1/auth/login
```

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"gildong@example.com","password":"ocare1234"}'
```

**200 OK**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
    "refreshToken": "eyJhbGciOiJIUzM4NCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

`expiresIn` 은 액세스 토큰의 남은 유효 시간(초)입니다. 클라이언트가 만료 시점을 예측해
미리 재발급할 수 있도록 내려줍니다. 토큰을 직접 열어보게 하지 않기 위함입니다.

실패 시 **401** `LOGIN_FAILED`. 없는 이메일과 틀린 비밀번호가 같은 응답입니다.

## 토큰 재발급

```
POST /api/v1/auth/refresh
```

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"eyJhbGciOiJIUzM4NCJ9..."}'
```

응답은 로그인과 같습니다. **재발급에 성공하면 리프레시 토큰도 새로 발급되고 이전 것은 무효가 됩니다.**
이전 토큰이 계속 유효하면 탈취된 토큰이 유효기간 내내 재발급에 쓰일 수 있기 때문입니다.

리프레시 토큰을 `Authorization` 헤더가 아닌 본문으로 받는 이유는, 그 헤더가 액세스 토큰
자리이기 때문입니다. 같은 자리에 두 종류를 섞으면 인증 필터가 어느 것을 검증할지 모호해집니다.

## 로그아웃

```
POST /api/v1/auth/logout
Authorization: Bearer {accessToken}
```

**200 OK** — 저장된 리프레시 토큰을 지워 재발급을 막습니다.

이미 발급된 액세스 토큰은 만료(30분)까지 통과합니다. 서명만으로 검증되는 JWT 의 특성상
개별 토큰을 무효화할 수 없으며, 액세스 토큰 유효기간을 짧게 둔 이유가 이것입니다.

---

# 레코드키

레코드키는 단말이 발급하는 사용자 구분 키입니다. 수집·조회 전에 등록되어 있어야 합니다.

## 등록

```
POST /api/v1/record-keys
Authorization: Bearer {accessToken}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| `recordKey` | string | 필수, 64자 이하, 전역 중복 불가 |
| `source` | string | 필수, `SamsungHealth` 또는 `Health Kit` |
| `productName` | string | 선택, 50자 이하 |
| `productVendor` | string | 선택, 50자 이하 |

```bash
curl -X POST http://localhost:8080/api/v1/record-keys \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"recordKey":"7836887b-b12a-440f-af0f-851546504b13","source":"SamsungHealth",
       "productName":"Android","productVendor":"Samsung"}'
```

**201 Created**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "recordKey": "7836887b-b12a-440f-af0f-851546504b13",
    "source": "SamsungHealth",
    "productName": "Android",
    "productVendor": "Samsung",
    "status": "ACTIVE",
    "createdAt": "2026-08-02T13:45:12.331"
  }
}
```

이미 등록된 키면 **409** `RECORD_KEY_DUPLICATED`. 본인 것이든 타인 것이든 같은 응답입니다.
소유자를 알려주면 임의의 키를 넣어보며 등록 여부를 확인하는 수단이 됩니다.

## 내 레코드키 목록

```
GET /api/v1/record-keys
Authorization: Bearer {accessToken}
```

본인이 등록한 것만 반환합니다. 단말을 여러 대 연동한 경우를 위해 목록입니다.

---

# 건강 데이터

## 수집

```
POST /api/v1/health-data
Authorization: Bearer {accessToken}
```

단말이 App to App 으로 받은 payload 를 그대로 전달합니다.

```json
{
  "recordkey": "7836887b-b12a-440f-af0f-851546504b13",
  "type": "steps",
  "lastUpdate": "2024-12-16 14:40:00 +0000",
  "data": {
    "memo": "",
    "source": {
      "name": "SamsungHealth",
      "mode": 9,
      "type": "",
      "product": { "name": "Android", "vender": "Samsung" }
    },
    "entries": [
      {
        "period": { "from": "2024-11-15 00:00:00", "to": "2024-11-15 00:10:00" },
        "steps": 54,
        "distance": { "value": 0.04223, "unit": "km" },
        "calories": { "value": 2.03, "unit": "kcal" }
      }
    ]
  }
}
```

필드명은 단말이 보내는 표기 그대로입니다(`recordkey` 는 전부 소문자, `vender` 는 원문 오타).
이미 배포된 앱의 전송 형식을 서버가 바꿀 수는 없기 때문입니다.

`steps` 는 숫자와 문자열을 모두 받습니다. 삼성은 `54`, 애플은 `"688.5509846105425"` 로 보냅니다.
`period` 는 오프셋이 있는 표기와 없는 표기를 모두 받으며, 없으면 KST 로 간주합니다.

```bash
curl -X POST http://localhost:8080/api/v1/health-data \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  --data-binary @INPUT_DATA1.json
```

**200 OK**

```json
{
  "success": true,
  "data": {
    "uploadId": 1,
    "received": 1066,
    "saved": 1066,
    "duplicated": 0,
    "failed": 0,
    "affectedDates": ["2024-11-15", "2024-11-16", "..."]
  }
}
```

재전송하면 같은 200 이지만 건수가 달라집니다.

```json
{ "uploadId": 1, "received": 1066, "saved": 0, "duplicated": 1066, "failed": 0, "affectedDates": [] }
```

재전송은 오류가 아니라 정상 흐름이므로 200 으로 응답하고, 저장된 건수와 건너뛴 건수를
나눠 알려줍니다. 저장 건수만 주면 재전송으로 걸러진 것인지 오류로 빠진 것인지 구분할 수 없습니다.

| 상황 | 응답 |
|---|---|
| 타인의 레코드키 | 403 `RECORD_KEY_FORBIDDEN` |
| 등록되지 않은 레코드키 | 404 `RECORD_KEY_NOT_FOUND` |
| 수집 중단된 레코드키 | 409 `RECORD_KEY_INACTIVE` |
| 알 수 없는 출처·단위 | 400 `INVALID_REQUEST` |
| 해석할 수 없는 시각 표기 | 400 `INVALID_REQUEST` |

## 일별 조회

```
GET /api/v1/health-data/daily?recordKey={key}&from={yyyy-MM-dd}&to={yyyy-MM-dd}
Authorization: Bearer {accessToken}
```

| 파라미터 | 형식 | 제약 |
|---|---|---|
| `recordKey` | string | 필수 |
| `from` | `yyyy-MM-dd` | 필수 |
| `to` | `yyyy-MM-dd` | 필수, `from` 이후, 최대 366일 |

```bash
curl "http://localhost:8080/api/v1/health-data/daily?recordKey=7836887b-b12a-440f-af0f-851546504b13&from=2024-11-15&to=2024-11-17" \
  -H "Authorization: Bearer $TOKEN"
```

**200 OK**

```json
{
  "success": true,
  "data": {
    "recordKey": "7836887b-b12a-440f-af0f-851546504b13",
    "from": "2024-11-15",
    "to": "2024-11-17",
    "days": 3,
    "summaries": [
      { "date": "2024-11-15", "steps": 7243, "calories": 289.21, "distance": 5.41949,
        "entryCount": 38, "caloriesSupported": true },
      { "date": "2024-11-16", "steps": 10717, "calories": 425.53, "distance": 8.02048,
        "entryCount": 44, "caloriesSupported": true },
      { "date": "2024-11-17", "steps": 7390, "calories": 288.49, "distance": 5.60801,
        "entryCount": 33, "caloriesSupported": true }
    ]
  }
}
```

| 필드 | 설명 |
|---|---|
| `steps` | 총 걸음수. 소수 걸음을 모두 더한 뒤 한 번만 반올림한 값 |
| `calories` | 총 소모 칼로리(kcal) |
| `distance` | 총 이동거리(km) |
| `entryCount` | 집계에 사용된 측정 구간 수 |
| `caloriesSupported` | 출처가 칼로리를 제공하는지 여부 |

**측정 데이터가 없는 날은 행이 생략됩니다.** 응답에 조회 조건(`from`, `to`)이 함께 담기므로
빈 결과가 "활동이 없던 날"인지 "조회 범위 밖"인지 구분할 수 있습니다.

`caloriesSupported` 가 `false` 면 `calories` 값에 의미가 없습니다.
애플 HealthKit 은 칼로리를 제공하지 않아 항상 0 이 들어오는데,
이 플래그가 없으면 클라이언트가 "활동이 없었다" 로 잘못 표시하게 됩니다.

## 월별 조회

```
GET /api/v1/health-data/monthly?recordKey={key}&from={yyyy-MM}&to={yyyy-MM}
Authorization: Bearer {accessToken}
```

| 파라미터 | 형식 | 제약 |
|---|---|---|
| `recordKey` | string | 필수 |
| `from` | `yyyy-MM` | 필수 |
| `to` | `yyyy-MM` | 필수, `from` 이후, 최대 24개월 |

```bash
curl "http://localhost:8080/api/v1/health-data/monthly?recordKey=7836887b-b12a-440f-af0f-851546504b13&from=2024-11&to=2024-12" \
  -H "Authorization: Bearer $TOKEN"
```

**200 OK**

```json
{
  "success": true,
  "data": {
    "recordKey": "7836887b-b12a-440f-af0f-851546504b13",
    "from": "2024-11",
    "to": "2024-12",
    "months": 2,
    "summaries": [
      { "month": "2024-11", "steps": 124783, "calories": 5002.5, "distance": 94.3421,
        "activeDays": 16, "entryCount": 551, "caloriesSupported": true },
      { "month": "2024-12", "steps": 115592, "calories": 4635.84, "distance": 87.37634,
        "activeDays": 16, "entryCount": 515, "caloriesSupported": true }
    ]
  }
}
```

`activeDays` 는 측정 데이터가 존재한 일수입니다. 월 전체 일수와 다르며, 활동 밀도를 판단하는
근거가 됩니다.

월 합계는 원본이 아니라 **일별 값을 그대로 더한 값**입니다. 사용자가 일별 화면과 월별 화면을
함께 볼 때 두 값이 어긋나지 않도록 집계 단계에서 그렇게 정의했습니다.

---

## 전체 흐름 예시

```bash
BASE=http://localhost:8080/api/v1

# 1. 가입
curl -X POST $BASE/auth/signup -H 'Content-Type: application/json' \
  -d '{"name":"홍길동","nickname":"gildong","email":"gildong@example.com","password":"ocare1234"}'

# 2. 로그인
TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"gildong@example.com","password":"ocare1234"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# 3. 레코드키 등록
curl -X POST $BASE/record-keys -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"recordKey":"7836887b-b12a-440f-af0f-851546504b13","source":"SamsungHealth"}'

# 4. 수집
curl -X POST $BASE/health-data -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' --data-binary @INPUT_DATA1.json

# 5. 조회
curl "$BASE/health-data/daily?recordKey=7836887b-b12a-440f-af0f-851546504b13&from=2024-11-15&to=2024-12-16" \
  -H "Authorization: Bearer $TOKEN"
curl "$BASE/health-data/monthly?recordKey=7836887b-b12a-440f-af0f-851546504b13&from=2024-11&to=2024-12" \
  -H "Authorization: Bearer $TOKEN"
```
