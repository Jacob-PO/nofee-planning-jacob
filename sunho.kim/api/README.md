# Nofee Carrier Integration API

통신사 공시지원금 통합 조회 및 매장 신뢰도 관리 API

**Base URL:** `http://localhost:8090`

---

## 목차

1. [Carrier Integration API](#1-carrier-integration-api) - 통신사 공시지원금 통합 조회
2. [Direct Subsidy API](#2-direct-subsidy-api) - 실시간 공시지원금 직접 조회
3. [Device Mapping API](#3-device-mapping-api) - 노피 상품 ↔ 통신사 기기 매핑
4. [Trust Score API](#4-trust-score-api) - 매장 신뢰도 관리

---

## 공통 코드

### 통신사 코드
| 코드 | 통신사 | 노피 코드 |
|------|--------|-----------|
| SKT | SK텔레콤 | 0301001001 |
| KT | KT | 0301001002 |
| LGU | LG유플러스 | 0301001003 |

### 가입유형 코드
| 한글 | 노피 코드 | SKT | KT | LGU |
|------|-----------|-----|----|----|
| 신규 | 0301007001 | 11 | 01 | 3 |
| 기기변경 | 0301007002 | 31 | 04 | 1 |
| 번호이동 | 0301007003 | 20 | 02 | 2 |

### 지원유형 코드
| 유형 | 노피 코드 |
|------|-----------|
| 공시지원금 | 0301006001 |
| 선택약정24 | 0301006002 |

---

## 1. Carrier Integration API

**Base Path:** `/api/test/carrier`

통신사(SKT, KT, LGU+) 공시지원금 통합 조회 API. Google Sheets 기반 24시간 TTL 캐시 적용.

### 1.1 헬스 체크

```http
GET /api/test/carrier/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "carrier-integration",
  "cache": "Google Sheets (24h TTL)",
  "endpoints": { ... }
}
```

---

### 1.2 전체 공시지원금 조회

```http
GET /api/test/carrier/subsidies?refresh=false
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| refresh | boolean | X | true: 캐시 무시하고 API 재호출 |

**Response:** `UnifiedSubsidyResponse`
```json
{
  "success": true,
  "sktSubsidies": [...],
  "ktSubsidies": [...],
  "lguSubsidies": [...],
  "elapsedMs": 1234
}
```

---

### 1.3 통신사별 공시지원금 조회

```http
GET /api/test/carrier/subsidies/{carrier}
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| carrier | path | O | SKT, KT, LGU |
| planCode | query | X | 요금제 코드 |
| joinType | query | X | 가입유형 코드 |
| planMonthlyFee | query | X | 월정액 (37000, 85000 등) |
| refresh | query | X | 캐시 갱신 여부 |

**예시:**
```bash
curl "http://localhost:8090/api/test/carrier/subsidies/SKT?joinType=31&planMonthlyFee=85000"
```

---

### 1.4 기기별 공시지원금 조회

```http
GET /api/test/carrier/subsidies/device
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| sktCode | query | X | SKT 기기코드 |
| ktCode | query | X | KT 기기코드 |
| lguCode | query | X | LGU 기기코드 |
| joinType | query | X | 가입유형 (신규/기기변경/번호이동) |
| planMonthlyFee | query | X | 월정액 |

**예시:**
```bash
curl "http://localhost:8090/api/test/carrier/subsidies/device?sktCode=SM-S928NK&joinType=기기변경&planMonthlyFee=85000"
```

---

### 1.5 노피 상품코드로 조회

```http
GET /api/test/carrier/subsidies/nofee/{nofeeProductCode}
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| nofeeProductCode | path | O | 노피 상품코드 (예: SM-S24-U) |

---

### 1.6 캐시 관리

```http
# 캐시 상태 조회
GET /api/test/carrier/cache/status

# 캐시 강제 갱신 (전체)
POST /api/test/carrier/cache/refresh

# 증분 업데이트 (최근 N일 공시일 기준)
POST /api/test/carrier/cache/incremental?days=7

# 캐시 초기화 (삭제)
DELETE /api/test/carrier/cache
```

**캐시 상태 응답:**
```json
{
  "enabled": true,
  "ttlHours": 24,
  "memoryCacheSize": 150,
  "scheduledCleanup": "1시간마다 자동 정리",
  "expiredCacheCount": 0
}
```

---

### 1.7 요금제 API

```http
# 전체 요금제 조회
GET /api/test/carrier/plans

# 통신사별 요금제 조회
GET /api/test/carrier/plans/{carrier}

# 월정액으로 요금제 코드 조회
GET /api/test/carrier/plans/{carrier}/code?monthlyFee=85000

# 요금제 동기화
POST /api/test/carrier/plans/sync

# 통신사 API에서 요금제 동기화
POST /api/test/carrier/plans/sync/carrier-api
```

---

### 1.8 통신사별 직접 조회 API

```http
# KT 요금제 목록
GET /api/test/carrier/kt/plans?networkType=5G

# SKT 요금제 목록
GET /api/test/carrier/skt/plans?networkType=5G

# LGU+ 요금제 목록
GET /api/test/carrier/lgu/plans?networkType=5G

# KT 전체 요금제별 공시지원금
GET /api/test/carrier/kt/subsidies/all?joinType=04&networkType=5G
```

---

## 2. Direct Subsidy API

**Base Path:** `/api/test/carrier/direct`

캐시 없이 실시간으로 통신사 API를 직접 호출하는 API. 항상 최신 데이터 조회 필요 시 사용.

### 2.1 헬스 체크

```http
GET /api/test/carrier/direct/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "direct-subsidy",
  "description": "캐시 없이 실시간 통신사 API 직접 호출",
  "carrierCodes": {
    "SKT": "0301001001",
    "KT": "0301001002",
    "LGU": "0301001003"
  },
  "joinTypeCodes": {
    "신규": "0301007001",
    "기기변경": "0301007002",
    "번호이동": "0301007003"
  }
}
```

---

### 2.2 노피 상품코드 + 통신사로 조회 (권장)

```http
GET /api/test/carrier/direct/subsidy
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| nofeeProductCode | query | O | 노피 상품코드 (예: SM-ZP-7) |
| carrier | query | O | 통신사 (SKT, KT, LGU 또는 노피코드) |
| joinType | query | O | 가입유형 노피코드 |
| planMonthlyFee | query | O | 월정액 |

**예시:**
```bash
curl "http://localhost:8090/api/test/carrier/direct/subsidy?\
nofeeProductCode=SM-ZP-7&\
carrier=SKT&\
joinType=0301007002&\
planMonthlyFee=89000"
```

---

### 2.3 노피 상품코드로 전체 통신사 조회

```http
GET /api/test/carrier/direct/nofee/{nofeeProductCode}
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| nofeeProductCode | path | O | 노피 상품코드 |
| joinType | query | O | 가입유형 노피코드 |
| planMonthlyFee | query | O | 월정액 |

**예시:**
```bash
curl "http://localhost:8090/api/test/carrier/direct/nofee/AP-E-16?\
joinType=0301007002&\
planMonthlyFee=85000"
```

---

### 2.4 상품명으로 조회

```http
GET /api/test/carrier/direct/device
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| productGroupNm | query | O | 상품명 (예: 갤럭시 S24 울트라) |
| carrier | query | O | 통신사 노피코드 |
| joinType | query | O | 가입유형 노피코드 |
| planMonthlyFee | query | O | 월정액 |
| networkType | query | O | 5G 또는 LTE |
| supportType | query | O | 지원유형 노피코드 |

**예시:**
```bash
curl "http://localhost:8090/api/test/carrier/direct/device?\
productGroupNm=아이폰%2016%20프로%20맥스&\
carrier=0301001001&\
joinType=0301007002&\
planMonthlyFee=109000&\
networkType=5G&\
supportType=0301006001"
```

---

## 3. Device Mapping API

**Base Path:** `/api/test/device-mapping`

노피 상품 ↔ 통신사 기기 코드 매핑 관리. Google Sheets를 저장소로 사용.

### 3.1 전체 매핑 조회

```http
GET /api/test/device-mapping/mappings
```

**Response:** `List<DeviceMapping>`
```json
[
  {
    "nofeeProductCode": "SM-S24-U",
    "nofeeProductName": "갤럭시 S24 울트라",
    "sktDeviceCode": "SM-S928NK",
    "sktDeviceName": "갤럭시 S24 울트라 256GB",
    "ktDeviceCode": "SM-S928NK",
    "ktDeviceName": "갤럭시 S24 울트라 256GB",
    "lguDeviceCode": "SM-S928NK",
    "lguDeviceName": "갤럭시 S24 울트라 256GB",
    "mappedAt": "2024-12-09T12:00:00",
    "confidence": "high"
  }
]
```

---

### 3.2 단일 매핑 조회

```http
GET /api/test/device-mapping/mapping/{nofeeCode}
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| nofeeCode | path | O | 노피 상품코드 |

**Response:**
```json
{
  "success": true,
  "data": { ... DeviceMapping }
}
```

---

### 3.3 매핑 동기화

```http
POST /api/test/device-mapping/sync
```

노피 DB의 상품 목록과 각 통신사 API의 기기 목록을 조회하여 자동 매핑 수행.

**Response:**
```json
{
  "success": true,
  "count": 150,
  "elapsed": 5.2,
  "message": "150개 매핑 완료 (5.2초)"
}
```

---

### 3.4 노피 상품 목록 조회

```http
GET /api/test/device-mapping/nofee-products
```

DB에서 활성 상태(`state_code='0204002'`)의 노피 상품 목록 조회.

---

### 3.5 노피 상품별 공시지원금 조회

```http
GET /api/test/device-mapping/subsidies/{nofeeCode}
```

노피 상품코드로 매핑된 통신사 기기의 공시지원금 조회.

---

### 3.6 전체 공시지원금 조회

```http
GET /api/test/device-mapping/subsidies?refresh=false
```

---

### 3.7 매핑 추가 (테스트용)

```http
POST /api/test/device-mapping/mappings
```

**Request Body:**
```json
{
  "nofeeProductCode": "SM-TEST",
  "nofeeProductName": "테스트 상품",
  "sktDeviceCode": "SM-TEST-SKT",
  "ktDeviceCode": "SM-TEST-KT",
  "lguDeviceCode": "SM-TEST-LGU",
  "confidence": "high"
}
```

---

## 4. Trust Score API

**Base Path:** `/api/v1/trust`

매장 신뢰도(Trust Score) 관리 API. EMA 알고리즘 기반 점진적 신뢰도 업데이트.

### 신뢰도 계산 공식

```
T = 0.6 × (σ 일치 신호) + 0.4 × (후기/클레임)

Δ = 0.5×priceMatch + 0.3×conditionMatch + 0.2×reviewScore - 0.5

내부 점수 (0~1) → UI 층 (1~100층) 맵핑
```

### 신뢰 등급

| 등급 | 조건 | 설명 |
|------|------|------|
| EXCELLENT | T >= 0.7 | 우수 - 신뢰도가 매우 높은 매장 |
| GOOD | T >= 0.5 | 양호 - 신뢰도가 높은 매장 |
| NORMAL | T >= 0.3 | 보통 - 일반적인 수준 |
| WARNING | T >= 0.2 | 주의 - 노출 -50% |
| POOR | T < 0.2 | 경고 - 하단 고정 |

---

### 4.1 매장 신뢰 점수 조회

```http
GET /api/v1/trust/stores/{storeId}
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| storeId | path | O | 매장 ID |

**Response:** `TrustScoreResponse`
```json
{
  "storeId": "store-001",
  "storeName": "노피폰 강남점",
  "trustFloor": 75,
  "trustScore": 0.75,
  "priceMatchRate": 0.95,
  "conditionMatchRate": 0.88,
  "avgRating": 4.5,
  "reviewCount": 128,
  "claimCount": 2,
  "visitCount": 1500,
  "lastUpdatedAt": "2024-12-09T12:00:00",
  "grade": "EXCELLENT"
}
```

---

### 4.2 여러 매장 일괄 조회

```http
GET /api/v1/trust/stores?storeIds=store-001,store-002,store-003
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| storeIds | query | O | 쉼표 구분 매장 ID 목록 |

**Response:** `List<TrustScoreResponse>`

---

### 4.3 신뢰 점수 업데이트

```http
POST /api/v1/trust/update
```

거래 완료 후 신뢰 점수를 EMA 알고리즘으로 업데이트.

**Request Body:** `TrustScoreUpdateRequest`
```json
{
  "storeId": "store-001",
  "transactionId": "tx-12345",
  "priceMatch": 1.0,
  "conditionMatch": 0.9,
  "reviewScore": 4.5
}
```

**Response:** `TrustScoreResponse` (업데이트된 점수)

---

### 4.4 신뢰 점수 히스토리 조회

```http
GET /api/v1/trust/stores/{storeId}/history
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| storeId | path | O | 매장 ID |

**Response:** `TrustScoreHistoryResponse`
```json
{
  "storeId": "store-001",
  "history": [
    {
      "timestamp": "2024-12-09T12:00:00",
      "trustScore": 0.75,
      "transactionId": "tx-12345",
      "delta": 0.02
    }
  ]
}
```

---

## DTO 정의

### CarrierSubsidy

```typescript
interface CarrierSubsidy {
  id: string;                  // 복합키 (carrier-joinType-deviceCode-planCode)
  carrier: string;             // SKT, KT, LGU+
  joinType: string;            // 신규, 기기변경, 번호이동
  discountType: string;        // 공시지원, 선택약정
  deviceName: string;          // 기기명
  deviceCode: string;          // 기기 코드
  storage: string;             // 저장용량
  color: string;               // 색상
  planName: string;            // 요금제명
  planCode: string;            // 요금제 코드
  planMonthlyFee: number;      // 월정액 (원)
  planMaintainMonth: number;   // 약정기간 (개월, 기본 6)
  msrp: number;                // 출고가 (원)
  carrierSubsidy: number;      // 공시지원금 (원)
  additionalSubsidy: number;   // 추가지원금 (원)
  installmentPrice: number;    // 할부원금 (출고가 - 공시 - 추가)
  announceDate: string;        // 공시일 (YYYY-MM-DD)
  cachedAt: string;            // 캐시 시간
}
```

### DeviceMapping

```typescript
interface DeviceMapping {
  nofeeProductCode: string;    // 노피 상품코드
  nofeeProductName: string;    // 노피 상품명
  sktDeviceCode: string;       // SKT 기기코드
  sktDeviceName: string;       // SKT 기기명
  ktDeviceCode: string;        // KT 기기코드
  ktDeviceName: string;        // KT 기기명
  lguDeviceCode: string;       // LGU 기기코드
  lguDeviceName: string;       // LGU 기기명
  mappedAt: string;            // 매핑 시간
  confidence: string;          // high, medium, low
}
```

### TrustScoreResponse

```typescript
interface TrustScoreResponse {
  storeId: string;             // 매장 ID
  storeName: string;           // 매장명
  trustFloor: number;          // UI 층 (1~100)
  trustScore: number;          // 내부 점수 (0~1)
  priceMatchRate: number;      // 가격 일치율 (0~1)
  conditionMatchRate: number;  // 조건 일치율 (0~1)
  avgRating: number;           // 평균 평점 (1~5)
  reviewCount: number;         // 리뷰 수
  claimCount: number;          // 클레임 수
  visitCount: number;          // 방문자 수
  lastUpdatedAt: string;       // 마지막 업데이트
  grade: TrustGrade;           // EXCELLENT, GOOD, NORMAL, WARNING, POOR
}
```

---

## 실행 방법

```bash
# 서버 시작
cd api
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew bootRun

# 서버 확인
curl http://localhost:8090/api/test/carrier/health
```

---

## 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Spring Boot API                          │
│  ┌───────────────┬────────────────┬────────────────────┐    │
│  │ Carrier API   │ Direct API     │ Device Mapping API │    │
│  │ (캐시 사용)    │ (실시간 조회)   │ (매핑 관리)        │    │
│  └───────────────┴────────────────┴────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                    Trust Score API                   │    │
│  │                    (신뢰도 관리)                      │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
          │                    │                    │
          ▼                    ▼                    ▼
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│ Google      │      │ 통신사 API   │      │ Nofee DB    │
│ Sheets      │      │ SKT/KT/LGU  │      │ (MySQL)     │
│ (캐시)       │      │             │      │             │
└─────────────┘      └─────────────┘      └─────────────┘
```

---

## 성능 최적화

### 적용된 최적화

1. **캐시 시스템**: Google Sheets 기반 24시간 TTL 캐시
2. **자동 클린업**: 1시간마다 만료 캐시 자동 정리 (`@Scheduled`)
3. **Regex 프리컴파일**: `ModelInfo.java`에서 21개 패턴 static final 선언
4. **HTTP 타임아웃**: 연결 10초, 읽기 30초 제한
5. **증분 업데이트**: 공시일 기준 최근 N일 데이터만 갱신

---

## 라이센스

Nofee Internal Use Only
