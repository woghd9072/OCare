# Daily / Monthly 조회 결과

제공된 입력 데이터 4개 파일을 **실제 API 로 수집한 뒤 조회 API 를 호출한 결과**입니다.
아래 표와 JSON 은 손으로 작성한 것이 아니라 응답을 그대로 옮긴 것입니다.

## 수집 결과

| 파일 | recordkey | 출처 | 단말 | 수신 | 저장 | 영향 일자 |
|---|---|---|---|---:|---:|---:|
| INPUT_DATA1.json | `7836887b…` | SamsungHealth | Android / Samsung | 1,066 | 1,066 | 32일 |
| INPUT_DATA2.json | `3b87c9a4…` | SamsungHealth | Android / Samsung | 1,497 | 1,497 | 32일 |
| INPUT_DATA3.json | `7b012e6e…` | Health Kit | iPhone / Apple inc. | 1,459 | 1,459 | 31일 |
| INPUT_DATA4.json | `e27ba7ef…` | Health Kit | iPhone / Apple inc. | 688 | 688 | 31일 |
| **합계** | | | | **4,710** | **4,710** | |

### 동일 파일 재전송

`INPUT_DATA1.json` 을 한 번 더 전송한 결과입니다. 재전송은 오류가 아니므로 200 으로 응답하고,
저장 건수와 중복 건수를 나눠 알려줍니다. 원본 데이터도 집계값도 늘지 않습니다.

```json
{
  "uploadId": 9,
  "received": 1066,
  "saved": 0,
  "duplicated": 1066,
  "failed": 0,
  "affectedDates": []
}
```

---

## Monthly 조회 결과

`GET /api/v1/health-data/monthly?recordKey={key}&from=2024-11&to=2024-12`

| recordkey | 출처 | 월 | 걸음수 | 칼로리(kcal) | 이동거리(km) | 활동일수 | 구간수 | 칼로리 제공 |
|---|---|---|---:|---:|---:|---:|---:|:---:|
| `7836887b…` | SamsungHealth | 2024-11 | 124,783 | 5,002.50 | 94.342 | 16 | 551 | O |
| `7836887b…` | SamsungHealth | 2024-12 | 115,592 | 4,635.84 | 87.376 | 16 | 515 | O |
| `3b87c9a4…` | SamsungHealth | 2024-11 | 130,945 | 4,671.77 | 100.719 | 16 | 757 | O |
| `3b87c9a4…` | SamsungHealth | 2024-12 | 130,551 | 4,560.13 | 101.097 | 16 | 740 | O |
| `7b012e6e…` | Health Kit | 2024-11 | 115,957 | — | 92.766 | 16 | 714 | X |
| `7b012e6e…` | Health Kit | 2024-12 | 113,883 | — | 91.106 | 15 | 745 | X |
| `e27ba7ef…` | Health Kit | 2024-11 | 136,245 | — | 108.996 | 16 | 344 | X |
| `e27ba7ef…` | Health Kit | 2024-12 | 136,851 | — | 109.481 | 15 | 344 | X |

> 애플 HealthKit 은 칼로리를 제공하지 않아 항상 0 이 들어옵니다.
> `caloriesSupported: false` 로 구분해 내려주므로, 클라이언트가 "활동이 없었다" 로 잘못 표시하지 않습니다.

> 활동일수가 12월에 15~16일인 것은 입력 데이터가 12월 중순까지만 존재하기 때문입니다.
> 월 전체 일수가 아니라 실제 측정이 있었던 날만 센다.

<details><summary>원본 응답 — INPUT_DATA1.json (SamsungHealth)</summary>

```json
{
  "success": true,
  "data": {
    "recordKey": "7836887b-b12a-440f-af0f-851546504b13",
    "from": "2024-11",
    "to": "2024-12",
    "months": 2,
    "summaries": [
      {
        "month": "2024-11",
        "steps": 124783,
        "calories": 5002.5,
        "distance": 94.3421,
        "activeDays": 16,
        "entryCount": 551,
        "caloriesSupported": true
      },
      {
        "month": "2024-12",
        "steps": 115592,
        "calories": 4635.84,
        "distance": 87.37634,
        "activeDays": 16,
        "entryCount": 515,
        "caloriesSupported": true
      }
    ]
  }
}
```

</details>

<details><summary>원본 응답 — INPUT_DATA2.json (SamsungHealth)</summary>

```json
{
  "success": true,
  "data": {
    "recordKey": "3b87c9a4-f983-4168-8f27-85436447bb57",
    "from": "2024-11",
    "to": "2024-12",
    "months": 2,
    "summaries": [
      {
        "month": "2024-11",
        "steps": 130945,
        "calories": 4671.77,
        "distance": 100.71923,
        "activeDays": 16,
        "entryCount": 757,
        "caloriesSupported": true
      },
      {
        "month": "2024-12",
        "steps": 130551,
        "calories": 4560.13,
        "distance": 101.09696,
        "activeDays": 16,
        "entryCount": 740,
        "caloriesSupported": true
      }
    ]
  }
}
```

</details>

<details><summary>원본 응답 — INPUT_DATA3.json (Health Kit)</summary>

```json
{
  "success": true,
  "data": {
    "recordKey": "7b012e6e-ba2b-49c7-bc2e-473b7b58e72e",
    "from": "2024-11",
    "to": "2024-12",
    "months": 2,
    "summaries": [
      {
        "month": "2024-11",
        "steps": 115957,
        "calories": 0.0,
        "distance": 92.76632,
        "activeDays": 16,
        "entryCount": 714,
        "caloriesSupported": false
      },
      {
        "month": "2024-12",
        "steps": 113883,
        "calories": 0.0,
        "distance": 91.105713,
        "activeDays": 15,
        "entryCount": 745,
        "caloriesSupported": false
      }
    ]
  }
}
```

</details>

<details><summary>원본 응답 — INPUT_DATA4.json (Health Kit)</summary>

```json
{
  "success": true,
  "data": {
    "recordKey": "e27ba7ef-8bb2-424c-af1d-877e826b7487",
    "from": "2024-11",
    "to": "2024-12",
    "months": 2,
    "summaries": [
      {
        "month": "2024-11",
        "steps": 136245,
        "calories": 0.0,
        "distance": 108.996001,
        "activeDays": 16,
        "entryCount": 344,
        "caloriesSupported": false
      },
      {
        "month": "2024-12",
        "steps": 136851,
        "calories": 0.0,
        "distance": 109.480799,
        "activeDays": 15,
        "entryCount": 344,
        "caloriesSupported": false
      }
    ]
  }
}
```

</details>

---

## Daily 조회 결과

`GET /api/v1/health-data/daily?recordKey={key}&from=2024-11-01&to=2024-12-31`

측정 데이터가 없는 날은 행이 생략됩니다. 응답에 조회 조건이 함께 담기므로,
빈 결과가 "활동이 없던 날"인지 "조회 범위 밖"인지 구분할 수 있습니다.

### INPUT_DATA1.json — SamsungHealth (`7836887b-b12a-440f-af0f-851546504b13`)

조회 기간 2024-11-01 ~ 2024-12-31 중 데이터가 있는 날 **32일**

| 날짜 | 걸음수 | 칼로리(kcal) | 이동거리(km) | 구간수 |
|---|---:|---:|---:|---:|
| 2024-11-15 | 7,243 | 289.21 | 5.419 | 38 |
| 2024-11-16 | 10,717 | 425.53 | 8.020 | 44 |
| 2024-11-17 | 7,390 | 288.49 | 5.608 | 33 |
| 2024-11-18 | 7,928 | 316.70 | 6.034 | 31 |
| 2024-11-19 | 5,857 | 232.45 | 4.484 | 26 |
| 2024-11-20 | 7,762 | 310.83 | 5.842 | 39 |
| 2024-11-21 | 5,952 | 236.68 | 4.463 | 31 |
| 2024-11-22 | 9,692 | 385.84 | 7.393 | 34 |
| 2024-11-23 | 7,619 | 303.11 | 5.753 | 26 |
| 2024-11-24 | 6,285 | 249.51 | 4.825 | 36 |
| 2024-11-25 | 6,396 | 256.81 | 4.918 | 32 |
| 2024-11-26 | 6,815 | 280.83 | 5.112 | 27 |
| 2024-11-27 | 11,522 | 475.35 | 8.737 | 50 |
| 2024-11-28 | 9,423 | 385.36 | 7.048 | 42 |
| 2024-11-29 | 7,773 | 307.34 | 5.845 | 32 |
| 2024-11-30 | 6,409 | 258.46 | 4.841 | 30 |
| 2024-12-01 | 6,911 | 279.01 | 5.278 | 31 |
| 2024-12-02 | 5,152 | 209.01 | 3.906 | 24 |
| 2024-12-03 | 9,253 | 366.02 | 6.990 | 36 |
| 2024-12-04 | 11,100 | 452.92 | 8.327 | 37 |
| 2024-12-05 | 13,942 | 549.58 | 10.451 | 52 |
| 2024-12-06 | 6,069 | 244.99 | 4.601 | 31 |
| 2024-12-07 | 6,835 | 277.20 | 5.154 | 35 |
| 2024-12-08 | 2,943 | 115.40 | 2.243 | 25 |
| 2024-12-09 | 8,659 | 346.15 | 6.613 | 37 |
| 2024-12-10 | 6,546 | 265.09 | 4.962 | 36 |
| 2024-12-11 | 7,513 | 302.82 | 5.588 | 34 |
| 2024-12-12 | 7,093 | 285.37 | 5.414 | 24 |
| 2024-12-13 | 8,102 | 325.22 | 6.126 | 33 |
| 2024-12-14 | 4,797 | 191.80 | 3.657 | 22 |
| 2024-12-15 | 4,245 | 167.83 | 3.210 | 25 |
| 2024-12-16 | 6,432 | 257.43 | 4.856 | 33 |
| **합계** | **240,375** | **9,638.34** | **181.718** | **1,066** |

<details><summary>원본 응답 — INPUT_DATA1.json 전체 JSON</summary>

```json
{
  "success": true,
  "data": {
    "recordKey": "7836887b-b12a-440f-af0f-851546504b13",
    "from": "2024-11-01",
    "to": "2024-12-31",
    "days": 32,
    "summaries": [
      {
        "date": "2024-11-15",
        "steps": 7243,
        "calories": 289.21,
        "distance": 5.41949,
        "entryCount": 38,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-16",
        "steps": 10717,
        "calories": 425.53,
        "distance": 8.02048,
        "entryCount": 44,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-17",
        "steps": 7390,
        "calories": 288.49,
        "distance": 5.60801,
        "entryCount": 33,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-18",
        "steps": 7928,
        "calories": 316.7,
        "distance": 6.03405,
        "entryCount": 31,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-19",
        "steps": 5857,
        "calories": 232.45,
        "distance": 4.48394,
        "entryCount": 26,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-20",
        "steps": 7762,
        "calories": 310.83,
        "distance": 5.84158,
        "entryCount": 39,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-21",
        "steps": 5952,
        "calories": 236.68,
        "distance": 4.46327,
        "entryCount": 31,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-22",
        "steps": 9692,
        "calories": 385.84,
        "distance": 7.39323,
        "entryCount": 34,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-23",
        "steps": 7619,
        "calories": 303.11,
        "distance": 5.75303,
        "entryCount": 26,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-24",
        "steps": 6285,
        "calories": 249.51,
        "distance": 4.82459,
        "entryCount": 36,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-25",
        "steps": 6396,
        "calories": 256.81,
        "distance": 4.91758,
        "entryCount": 32,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-26",
        "steps": 6815,
        "calories": 280.83,
        "distance": 5.11183,
        "entryCount": 27,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-27",
        "steps": 11522,
        "calories": 475.35,
        "distance": 8.73672,
        "entryCount": 50,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-28",
        "steps": 9423,
        "calories": 385.36,
        "distance": 7.04841,
        "entryCount": 42,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-29",
        "steps": 7773,
        "calories": 307.34,
        "distance": 5.84512,
        "entryCount": 32,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-30",
        "steps": 6409,
        "calories": 258.46,
        "distance": 4.84077,
        "entryCount": 30,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-01",
        "steps": 6911,
        "calories": 279.01,
        "distance": 5.27771,
        "entryCount": 31,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-02",
        "steps": 5152,
        "calories": 209.01,
        "distance": 3.9057,
        "entryCount": 24,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-03",
        "steps": 9253,
        "calories": 366.02,
        "distance": 6.99048,
        "entryCount": 36,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-04",
        "steps": 11100,
        "calories": 452.92,
        "distance": 8.32712,
        "entryCount": 37,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-05",
        "steps": 13942,
        "calories": 549.58,
        "distance": 10.45088,
        "entryCount": 52,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-06",
        "steps": 6069,
        "calories": 244.99,
        "distance": 4.60099,
        "entryCount": 31,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-07",
        "steps": 6835,
        "calories": 277.2,
        "distance": 5.15401,
        "entryCount": 35,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-08",
        "steps": 2943,
        "calories": 115.4,
        "distance": 2.24306,
        "entryCount": 25,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-09",
        "steps": 8659,
        "calories": 346.15,
        "distance": 6.61318,
        "entryCount": 37,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-10",
        "steps": 6546,
        "calories": 265.09,
        "distance": 4.96175,
        "entryCount": 36,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-11",
        "steps": 7513,
        "calories": 302.82,
        "distance": 5.58759,
        "entryCount": 34,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-12",
        "steps": 7093,
        "calories": 285.37,
        "distance": 5.41447,
        "entryCount": 24,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-13",
        "steps": 8102,
        "calories": 325.22,
        "distance": 6.12631,
        "entryCount": 33,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-14",
        "steps": 4797,
        "calories": 191.8,
        "distance": 3.65707,
        "entryCount": 22,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-15",
        "steps": 4245,
        "calories": 167.83,
        "distance": 3.20954,
        "entryCount": 25,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-16",
        "steps": 6432,
        "calories": 257.43,
        "distance": 4.85648,
        "entryCount": 33,
        "caloriesSupported": true
      }
    ]
  }
}
```

</details>

### INPUT_DATA2.json — SamsungHealth (`3b87c9a4-f983-4168-8f27-85436447bb57`)

조회 기간 2024-11-01 ~ 2024-12-31 중 데이터가 있는 날 **32일**

| 날짜 | 걸음수 | 칼로리(kcal) | 이동거리(km) | 구간수 |
|---|---:|---:|---:|---:|
| 2024-11-15 | 9,589 | 345.36 | 7.243 | 65 |
| 2024-11-16 | 5,075 | 199.24 | 4.264 | 40 |
| 2024-11-17 | 5,179 | 183.69 | 3.983 | 41 |
| 2024-11-18 | 13,291 | 453.15 | 10.300 | 54 |
| 2024-11-19 | 8,203 | 305.68 | 6.328 | 50 |
| 2024-11-20 | 10,055 | 341.26 | 7.717 | 57 |
| 2024-11-21 | 9,114 | 327.44 | 6.933 | 52 |
| 2024-11-22 | 8,496 | 291.92 | 6.541 | 39 |
| 2024-11-23 | 5,117 | 194.74 | 3.978 | 32 |
| 2024-11-24 | 9,089 | 329.34 | 6.970 | 52 |
| 2024-11-25 | 8,660 | 308.74 | 6.698 | 51 |
| 2024-11-26 | 7,930 | 281.52 | 6.082 | 45 |
| 2024-11-27 | 9,566 | 350.06 | 7.253 | 51 |
| 2024-11-28 | 9,769 | 347.77 | 7.387 | 54 |
| 2024-11-29 | 8,698 | 303.99 | 6.684 | 41 |
| 2024-11-30 | 3,114 | 107.87 | 2.360 | 33 |
| 2024-12-01 | 6,920 | 228.82 | 5.487 | 37 |
| 2024-12-02 | 8,194 | 294.78 | 6.209 | 49 |
| 2024-12-03 | 11,103 | 383.32 | 8.528 | 48 |
| 2024-12-04 | 8,507 | 308.82 | 6.447 | 57 |
| 2024-12-05 | 10,329 | 375.13 | 7.868 | 50 |
| 2024-12-06 | 12,300 | 419.26 | 9.489 | 60 |
| 2024-12-07 | 5,063 | 220.87 | 4.447 | 34 |
| 2024-12-08 | 5,353 | 183.44 | 4.201 | 40 |
| 2024-12-09 | 5,740 | 191.13 | 4.466 | 60 |
| 2024-12-10 | 9,249 | 320.30 | 7.159 | 51 |
| 2024-12-11 | 10,467 | 350.56 | 8.136 | 46 |
| 2024-12-12 | 10,929 | 372.07 | 8.391 | 42 |
| 2024-12-13 | 9,349 | 314.54 | 7.218 | 42 |
| 2024-12-14 | 4,154 | 145.77 | 3.166 | 37 |
| 2024-12-15 | 3,412 | 116.98 | 2.633 | 32 |
| 2024-12-16 | 9,482 | 334.34 | 7.251 | 55 |
| **합계** | **261,496** | **9,231.90** | **201.816** | **1,497** |

<details><summary>원본 응답 — INPUT_DATA2.json 전체 JSON</summary>

```json
{
  "success": true,
  "data": {
    "recordKey": "3b87c9a4-f983-4168-8f27-85436447bb57",
    "from": "2024-11-01",
    "to": "2024-12-31",
    "days": 32,
    "summaries": [
      {
        "date": "2024-11-15",
        "steps": 9589,
        "calories": 345.36,
        "distance": 7.24323,
        "entryCount": 65,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-16",
        "steps": 5075,
        "calories": 199.24,
        "distance": 4.26379,
        "entryCount": 40,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-17",
        "steps": 5179,
        "calories": 183.69,
        "distance": 3.98311,
        "entryCount": 41,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-18",
        "steps": 13291,
        "calories": 453.15,
        "distance": 10.29972,
        "entryCount": 54,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-19",
        "steps": 8203,
        "calories": 305.68,
        "distance": 6.32778,
        "entryCount": 50,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-20",
        "steps": 10055,
        "calories": 341.26,
        "distance": 7.71734,
        "entryCount": 57,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-21",
        "steps": 9114,
        "calories": 327.44,
        "distance": 6.93269,
        "entryCount": 52,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-22",
        "steps": 8496,
        "calories": 291.92,
        "distance": 6.54098,
        "entryCount": 39,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-23",
        "steps": 5117,
        "calories": 194.74,
        "distance": 3.97768,
        "entryCount": 32,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-24",
        "steps": 9089,
        "calories": 329.34,
        "distance": 6.96978,
        "entryCount": 52,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-25",
        "steps": 8660,
        "calories": 308.74,
        "distance": 6.69834,
        "entryCount": 51,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-26",
        "steps": 7930,
        "calories": 281.52,
        "distance": 6.08157,
        "entryCount": 45,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-27",
        "steps": 9566,
        "calories": 350.06,
        "distance": 7.25321,
        "entryCount": 51,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-28",
        "steps": 9769,
        "calories": 347.77,
        "distance": 7.3867,
        "entryCount": 54,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-29",
        "steps": 8698,
        "calories": 303.99,
        "distance": 6.68351,
        "entryCount": 41,
        "caloriesSupported": true
      },
      {
        "date": "2024-11-30",
        "steps": 3114,
        "calories": 107.87,
        "distance": 2.3598,
        "entryCount": 33,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-01",
        "steps": 6920,
        "calories": 228.82,
        "distance": 5.48735,
        "entryCount": 37,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-02",
        "steps": 8194,
        "calories": 294.78,
        "distance": 6.20902,
        "entryCount": 49,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-03",
        "steps": 11103,
        "calories": 383.32,
        "distance": 8.52758,
        "entryCount": 48,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-04",
        "steps": 8507,
        "calories": 308.82,
        "distance": 6.44708,
        "entryCount": 57,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-05",
        "steps": 10329,
        "calories": 375.13,
        "distance": 7.86842,
        "entryCount": 50,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-06",
        "steps": 12300,
        "calories": 419.26,
        "distance": 9.48885,
        "entryCount": 60,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-07",
        "steps": 5063,
        "calories": 220.87,
        "distance": 4.44694,
        "entryCount": 34,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-08",
        "steps": 5353,
        "calories": 183.44,
        "distance": 4.20104,
        "entryCount": 40,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-09",
        "steps": 5740,
        "calories": 191.13,
        "distance": 4.46605,
        "entryCount": 60,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-10",
        "steps": 9249,
        "calories": 320.3,
        "distance": 7.1585,
        "entryCount": 51,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-11",
        "steps": 10467,
        "calories": 350.56,
        "distance": 8.13617,
        "entryCount": 46,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-12",
        "steps": 10929,
        "calories": 372.07,
        "distance": 8.39101,
        "entryCount": 42,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-13",
        "steps": 9349,
        "calories": 314.54,
        "distance": 7.21835,
        "entryCount": 42,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-14",
        "steps": 4154,
        "calories": 145.77,
        "distance": 3.16634,
        "entryCount": 37,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-15",
        "steps": 3412,
        "calories": 116.98,
        "distance": 2.63296,
        "entryCount": 32,
        "caloriesSupported": true
      },
      {
        "date": "2024-12-16",
        "steps": 9482,
        "calories": 334.34,
        "distance": 7.2513,
        "entryCount": 55,
        "caloriesSupported": true
      }
    ]
  }
}
```

</details>

### INPUT_DATA3.json — Health Kit (`7b012e6e-ba2b-49c7-bc2e-473b7b58e72e`)

조회 기간 2024-11-01 ~ 2024-12-31 중 데이터가 있는 날 **31일**

| 날짜 | 걸음수 | 칼로리(kcal) | 이동거리(km) | 구간수 |
|---|---:|---:|---:|---:|
| 2024-11-15 | 7,542 | — | 6.034 | 49 |
| 2024-11-16 | 6,630 | — | 5.304 | 45 |
| 2024-11-17 | 1,708 | — | 1.366 | 39 |
| 2024-11-18 | 11,119 | — | 8.896 | 56 |
| 2024-11-19 | 7,677 | — | 6.142 | 48 |
| 2024-11-20 | 6,348 | — | 5.078 | 44 |
| 2024-11-21 | 7,916 | — | 6.333 | 39 |
| 2024-11-22 | 9,376 | — | 7.501 | 43 |
| 2024-11-23 | 8,939 | — | 7.151 | 45 |
| 2024-11-24 | 4,490 | — | 3.592 | 39 |
| 2024-11-25 | 7,189 | — | 5.751 | 40 |
| 2024-11-26 | 8,679 | — | 6.943 | 43 |
| 2024-11-27 | 7,566 | — | 6.053 | 44 |
| 2024-11-28 | 9,043 | — | 7.234 | 49 |
| 2024-11-29 | 8,231 | — | 6.585 | 57 |
| 2024-11-30 | 3,504 | — | 2.803 | 34 |
| 2024-12-01 | 7,117 | — | 5.694 | 42 |
| 2024-12-02 | 5,839 | — | 4.671 | 43 |
| 2024-12-03 | 9,475 | — | 7.580 | 57 |
| 2024-12-04 | 7,857 | — | 6.285 | 41 |
| 2024-12-05 | 7,510 | — | 6.008 | 56 |
| 2024-12-06 | 7,136 | — | 5.708 | 58 |
| 2024-12-07 | 12,910 | — | 10.328 | 78 |
| 2024-12-08 | 3,484 | — | 2.787 | 48 |
| 2024-12-09 | 6,455 | — | 5.164 | 45 |
| 2024-12-10 | 7,980 | — | 6.384 | 41 |
| 2024-12-11 | 8,550 | — | 6.840 | 48 |
| 2024-12-12 | 8,697 | — | 6.958 | 47 |
| 2024-12-13 | 9,223 | — | 7.378 | 53 |
| 2024-12-14 | 5,749 | — | 4.599 | 43 |
| 2024-12-15 | 5,901 | — | 4.721 | 45 |
| **합계** | **229,840** | **—** | **183.872** | **1,459** |

<details><summary>원본 응답 — INPUT_DATA3.json 전체 JSON</summary>

```json
{
  "success": true,
  "data": {
    "recordKey": "7b012e6e-ba2b-49c7-bc2e-473b7b58e72e",
    "from": "2024-11-01",
    "to": "2024-12-31",
    "days": 31,
    "summaries": [
      {
        "date": "2024-11-15",
        "steps": 7542,
        "calories": 0.0,
        "distance": 6.03397,
        "entryCount": 49,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-16",
        "steps": 6630,
        "calories": 0.0,
        "distance": 5.30365,
        "entryCount": 45,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-17",
        "steps": 1708,
        "calories": 0.0,
        "distance": 1.3664,
        "entryCount": 39,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-18",
        "steps": 11119,
        "calories": 0.0,
        "distance": 8.895537,
        "entryCount": 56,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-19",
        "steps": 7677,
        "calories": 0.0,
        "distance": 6.141942,
        "entryCount": 48,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-20",
        "steps": 6348,
        "calories": 0.0,
        "distance": 5.078401,
        "entryCount": 44,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-21",
        "steps": 7916,
        "calories": 0.0,
        "distance": 6.3328,
        "entryCount": 39,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-22",
        "steps": 9376,
        "calories": 0.0,
        "distance": 7.500801,
        "entryCount": 43,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-23",
        "steps": 8939,
        "calories": 0.0,
        "distance": 7.150815,
        "entryCount": 45,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-24",
        "steps": 4490,
        "calories": 0.0,
        "distance": 3.592001,
        "entryCount": 39,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-25",
        "steps": 7189,
        "calories": 0.0,
        "distance": 5.750991,
        "entryCount": 40,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-26",
        "steps": 8679,
        "calories": 0.0,
        "distance": 6.94333,
        "entryCount": 43,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-27",
        "steps": 7566,
        "calories": 0.0,
        "distance": 6.052961,
        "entryCount": 44,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-28",
        "steps": 9043,
        "calories": 0.0,
        "distance": 7.234401,
        "entryCount": 49,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-29",
        "steps": 8231,
        "calories": 0.0,
        "distance": 6.584948,
        "entryCount": 57,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-30",
        "steps": 3504,
        "calories": 0.0,
        "distance": 2.803372,
        "entryCount": 34,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-01",
        "steps": 7117,
        "calories": 0.0,
        "distance": 5.693599,
        "entryCount": 42,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-02",
        "steps": 5839,
        "calories": 0.0,
        "distance": 4.671442,
        "entryCount": 43,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-03",
        "steps": 9475,
        "calories": 0.0,
        "distance": 7.579999,
        "entryCount": 57,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-04",
        "steps": 7857,
        "calories": 0.0,
        "distance": 6.285212,
        "entryCount": 41,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-05",
        "steps": 7510,
        "calories": 0.0,
        "distance": 6.007997,
        "entryCount": 56,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-06",
        "steps": 7136,
        "calories": 0.0,
        "distance": 5.708456,
        "entryCount": 58,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-07",
        "steps": 12910,
        "calories": 0.0,
        "distance": 10.327757,
        "entryCount": 78,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-08",
        "steps": 3484,
        "calories": 0.0,
        "distance": 2.786959,
        "entryCount": 48,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-09",
        "steps": 6455,
        "calories": 0.0,
        "distance": 5.164,
        "entryCount": 45,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-10",
        "steps": 7980,
        "calories": 0.0,
        "distance": 6.383997,
        "entryCount": 41,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-11",
        "steps": 8550,
        "calories": 0.0,
        "distance": 6.840276,
        "entryCount": 48,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-12",
        "steps": 8697,
        "calories": 0.0,
        "distance": 6.957598,
        "entryCount": 47,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-13",
        "steps": 9223,
        "calories": 0.0,
        "distance": 7.378419,
        "entryCount": 53,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-14",
        "steps": 5749,
        "calories": 0.0,
        "distance": 4.599201,
        "entryCount": 43,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-15",
        "steps": 5901,
        "calories": 0.0,
        "distance": 4.720801,
        "entryCount": 45,
        "caloriesSupported": false
      }
    ]
  }
}
```

</details>

### INPUT_DATA4.json — Health Kit (`e27ba7ef-8bb2-424c-af1d-877e826b7487`)

조회 기간 2024-11-01 ~ 2024-12-31 중 데이터가 있는 날 **31일**

| 날짜 | 걸음수 | 칼로리(kcal) | 이동거리(km) | 구간수 |
|---|---:|---:|---:|---:|
| 2024-11-15 | 6,672 | — | 5.338 | 22 |
| 2024-11-16 | 12,450 | — | 9.960 | 28 |
| 2024-11-17 | 233 | — | 0.186 | 4 |
| 2024-11-18 | 13,601 | — | 10.881 | 31 |
| 2024-11-19 | 12,192 | — | 9.754 | 24 |
| 2024-11-20 | 5,401 | — | 4.321 | 19 |
| 2024-11-21 | 9,569 | — | 7.655 | 18 |
| 2024-11-22 | 8,613 | — | 6.890 | 25 |
| 2024-11-23 | 8,243 | — | 6.594 | 23 |
| 2024-11-24 | 8 | — | 0.006 | 1 |
| 2024-11-25 | 12,801 | — | 10.241 | 32 |
| 2024-11-26 | 8,142 | — | 6.514 | 20 |
| 2024-11-27 | 6,672 | — | 5.338 | 19 |
| 2024-11-28 | 7,840 | — | 6.272 | 19 |
| 2024-11-29 | 10,896 | — | 8.717 | 27 |
| 2024-11-30 | 12,912 | — | 10.330 | 32 |
| 2024-12-01 | 35 | — | 0.028 | 3 |
| 2024-12-02 | 12,039 | — | 9.631 | 28 |
| 2024-12-03 | 6,953 | — | 5.562 | 20 |
| 2024-12-04 | 14,533 | — | 11.626 | 31 |
| 2024-12-05 | 10,817 | — | 8.654 | 24 |
| 2024-12-06 | 10,766 | — | 8.613 | 27 |
| 2024-12-07 | 7,485 | — | 5.988 | 15 |
| 2024-12-08 | 12,307 | — | 9.846 | 41 |
| 2024-12-09 | 10,958 | — | 8.766 | 22 |
| 2024-12-10 | 12,938 | — | 10.350 | 30 |
| 2024-12-11 | 12,589 | — | 10.071 | 29 |
| 2024-12-12 | 8,808 | — | 7.046 | 25 |
| 2024-12-13 | 10,485 | — | 8.388 | 27 |
| 2024-12-14 | 5,350 | — | 4.280 | 15 |
| 2024-12-15 | 788 | — | 0.630 | 7 |
| **합계** | **273,096** | **—** | **218.477** | **688** |

<details><summary>원본 응답 — INPUT_DATA4.json 전체 JSON</summary>

```json
{
  "success": true,
  "data": {
    "recordKey": "e27ba7ef-8bb2-424c-af1d-877e826b7487",
    "from": "2024-11-01",
    "to": "2024-12-31",
    "days": 31,
    "summaries": [
      {
        "date": "2024-11-15",
        "steps": 6672,
        "calories": 0.0,
        "distance": 5.3376,
        "entryCount": 22,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-16",
        "steps": 12450,
        "calories": 0.0,
        "distance": 9.960003,
        "entryCount": 28,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-17",
        "steps": 233,
        "calories": 0.0,
        "distance": 0.1864,
        "entryCount": 4,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-18",
        "steps": 13601,
        "calories": 0.0,
        "distance": 10.8808,
        "entryCount": 31,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-19",
        "steps": 12192,
        "calories": 0.0,
        "distance": 9.753601,
        "entryCount": 24,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-20",
        "steps": 5401,
        "calories": 0.0,
        "distance": 4.3208,
        "entryCount": 19,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-21",
        "steps": 9569,
        "calories": 0.0,
        "distance": 7.655201,
        "entryCount": 18,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-22",
        "steps": 8613,
        "calories": 0.0,
        "distance": 6.890401,
        "entryCount": 25,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-23",
        "steps": 8243,
        "calories": 0.0,
        "distance": 6.594399,
        "entryCount": 23,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-24",
        "steps": 8,
        "calories": 0.0,
        "distance": 0.0064,
        "entryCount": 1,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-25",
        "steps": 12801,
        "calories": 0.0,
        "distance": 10.240798,
        "entryCount": 32,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-26",
        "steps": 8142,
        "calories": 0.0,
        "distance": 6.5136,
        "entryCount": 20,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-27",
        "steps": 6672,
        "calories": 0.0,
        "distance": 5.3376,
        "entryCount": 19,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-28",
        "steps": 7840,
        "calories": 0.0,
        "distance": 6.272,
        "entryCount": 19,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-29",
        "steps": 10896,
        "calories": 0.0,
        "distance": 8.716799,
        "entryCount": 27,
        "caloriesSupported": false
      },
      {
        "date": "2024-11-30",
        "steps": 12912,
        "calories": 0.0,
        "distance": 10.329599,
        "entryCount": 32,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-01",
        "steps": 35,
        "calories": 0.0,
        "distance": 0.028,
        "entryCount": 3,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-02",
        "steps": 12039,
        "calories": 0.0,
        "distance": 9.6312,
        "entryCount": 28,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-03",
        "steps": 6953,
        "calories": 0.0,
        "distance": 5.5624,
        "entryCount": 20,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-04",
        "steps": 14533,
        "calories": 0.0,
        "distance": 11.626402,
        "entryCount": 31,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-05",
        "steps": 10817,
        "calories": 0.0,
        "distance": 8.6536,
        "entryCount": 24,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-06",
        "steps": 10766,
        "calories": 0.0,
        "distance": 8.612799,
        "entryCount": 27,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-07",
        "steps": 7485,
        "calories": 0.0,
        "distance": 5.988,
        "entryCount": 15,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-08",
        "steps": 12307,
        "calories": 0.0,
        "distance": 9.8456,
        "entryCount": 41,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-09",
        "steps": 10958,
        "calories": 0.0,
        "distance": 8.766399,
        "entryCount": 22,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-10",
        "steps": 12938,
        "calories": 0.0,
        "distance": 10.350401,
        "entryCount": 30,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-11",
        "steps": 12589,
        "calories": 0.0,
        "distance": 10.071201,
        "entryCount": 29,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-12",
        "steps": 8808,
        "calories": 0.0,
        "distance": 7.0464,
        "entryCount": 25,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-13",
        "steps": 10485,
        "calories": 0.0,
        "distance": 8.387998,
        "entryCount": 27,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-14",
        "steps": 5350,
        "calories": 0.0,
        "distance": 4.279999,
        "entryCount": 15,
        "caloriesSupported": false
      },
      {
        "date": "2024-12-15",
        "steps": 788,
        "calories": 0.0,
        "distance": 0.6304,
        "entryCount": 7,
        "caloriesSupported": false
      }
    ]
  }
}
```

</details>

---

## 검증

### 1. 집계값이 원본과 일치합니다

일별 집계 126행 전부를 원본 테이블과 대조했을 때 불일치가 없습니다.

```sql
SELECT COUNT(*) AS mismatched
FROM health_daily_summaries d
JOIN (SELECT record_key, measured_date,
             ROUND(SUM(steps)) AS steps, SUM(calories) AS calories,
             SUM(distance_km) AS distance_km, COUNT(*) AS entry_count
      FROM health_records GROUP BY record_key, measured_date) r
  ON r.record_key = d.record_key AND r.measured_date = d.summary_date
WHERE d.steps <> r.steps OR d.calories <> r.calories
   OR d.distance_km <> r.distance_km OR d.entry_count <> r.entry_count;
-- 결과: 0
```

### 2. 월 합계가 일별 값의 합과 일치합니다

| recordkey | 월 | 월별 집계 | 일별 값의 합 |
|---|---|---:|---:|
| `7836887b…` | 2024-11 | 124,783 | 124,783 |
| `7836887b…` | 2024-12 | 115,592 | 115,592 |
| `3b87c9a4…` | 2024-11 | 130,945 | 130,945 |
| `3b87c9a4…` | 2024-12 | 130,551 | 130,551 |
| `7b012e6e…` | 2024-11 | 115,957 | 115,957 |
| `7b012e6e…` | 2024-12 | 113,883 | 113,883 |
| `e27ba7ef…` | 2024-11 | 136,245 | 136,245 |
| `e27ba7ef…` | 2024-12 | 136,851 | 136,851 |

### 3. 소수 걸음수를 합산 후 한 번만 반올림합니다

애플 데이터의 걸음수는 소수입니다. 구간별로 반올림한 뒤 더하면 원본 합계와 어긋납니다.

| 대상 | 정확한 합 | 합산 후 반올림 | 구간별 반올림 후 합산 |
|---|---:|---:|---:|
| INPUT_DATA4, 2024-11-16 | 12449.99999999999997 | **12,450** | 12,449 |
| INPUT_DATA4, 전체 | — | **273,096** | 273,101 |
| INPUT_DATA3, 전체 | — | **229,840** | 229,837 |

차이는 크지 않지만 조회 결과가 원본과 정확히 일치해야 하므로 합산 후 1회 반올림을 택했습니다.
