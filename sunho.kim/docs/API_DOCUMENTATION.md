# 노피 API 문서

> 기존 노피 백엔드 (nofee-springboot) API 분석 및 새 프론트엔드 연동 가이드

## 서버 정보

### API 서버
- **Base URL**: `https://api.nofee.kr` (Production)
- **개발 서버**: 별도 확인 필요

### 데이터베이스
- **Host**: 43.203.125.223
- **Port**: 3306
- **Database**: db_nofee
- **JDBC URL**: `jdbc:mysql://43.203.125.223:3306/db_nofee`

---

## 새 프론트엔드에서 필요한 핵심 API

### 1. 상품/딜 관련 API

#### 1.1 시세표 상품 목록 (메인 딜 리스트)
```
POST /api/product-group/phone/store/pricetable-row
```

**Request Body:**
```json
{
  "page": 0,
  "size": 20,
  "sido": "서울",
  "sigungu": "강남구",
  "carrier": "SKT",           // 선택: SKT, KT, LGU+
  "joinType": "번호이동",       // 선택: 신규, 기변, 번호이동
  "discountType": "공시지원"   // 선택: 공시지원, 선택약정
}
```

**Response:**
```json
{
  "content": [
    {
      "priceTableRowNo": 123,
      "productGroupCode": "IP16PRO",
      "productGroupName": "아이폰 16 Pro",
      "productCode": "IP16PRO256",
      "productName": "아이폰 16 Pro 256GB",
      "storage": "256GB",
      "color": "블랙 티타늄",
      "carrier": "SKT",
      "joinType": "번호이동",
      "discountType": "공시지원",
      "releasePrice": 1550000,
      "carrierSubsidy": 300000,
      "shopSubsidy": 150000,
      "installmentPrice": 1100000,
      "monthlyPlan": 69000,
      "storeNo": 1,
      "storeName": "강남모바일",
      "sido": "서울",
      "sigungu": "강남구"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0
}
```

#### 1.2 시세표 지역 목록 (시/도)
```
POST /api/product-group/phone/store/pricetable-sido
```

**Response:**
```json
{
  "data": ["서울", "경기", "인천", "부산", "대구", "광주", "대전", "울산", "세종", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"]
}
```

#### 1.3 시세표 지역 목록 (시/군/구)
```
POST /api/product-group/phone/store/pricetable-sigungu
```

**Request Body:**
```json
{
  "sido": "서울"
}
```

**Response:**
```json
{
  "data": ["강남구", "강동구", "강북구", "강서구", "관악구", "광진구", ...]
}
```

#### 1.4 제조사 목록
```
POST /api/product-group/phone/store/pricetable-manufacturer
```

#### 1.5 상품그룹 목록 (기기 목록)
```
POST /api/product-group/phone/store/pricetable-product-group
```

#### 1.6 통신사별 시세표
```
POST /api/product-group/phone/store/pricetable-by-carrier
```

---

### 2. 매장 관련 API

#### 2.1 매장 목록
```
POST /api/store
```

**Request Body:**
```json
{
  "page": 0,
  "size": 20,
  "sido": "서울",
  "sigungu": "강남구"
}
```

**Response:**
```json
{
  "content": [
    {
      "storeNo": 1,
      "storeName": "강남모바일",
      "storeAddress": "서울 강남구 테헤란로 123",
      "storeTelNo": "02-1234-5678",
      "storeKakaoId": "gangnam_mobile",
      "carriers": ["SKT", "KT", "LGU+"],
      "avgRating": 4.8,
      "reviewCount": 342,
      "sido": "서울",
      "sigungu": "강남구",
      "isVerified": true,
      "thumbImgUrl": "/uploads/store/thumb_1.jpg"
    }
  ],
  "totalElements": 50,
  "totalPages": 3
}
```

#### 2.2 매장 상세
```
POST /api/store/{storeNo}
```

**Response:**
```json
{
  "storeNo": 1,
  "storeName": "강남모바일",
  "storeAddress": "서울 강남구 테헤란로 123",
  "storeAddressDetail": "2층",
  "storeTelNo": "02-1234-5678",
  "storeKakaoId": "gangnam_mobile",
  "carriers": ["SKT", "KT", "LGU+"],
  "avgRating": 4.8,
  "reviewCount": 342,
  "visitCount": 1520,
  "businessHours": "10:00 - 21:00",
  "dayOff": "일요일",
  "introduction": "강남 최저가 휴대폰 판매점",
  "isVerified": true,
  "thumbImgUrl": "/uploads/store/thumb_1.jpg",
  "pricetableImgUrls": ["/uploads/store/price_1.jpg"]
}
```

#### 2.3 매장 좋아요
```
POST /api/store/favorite
```

**Request Body:**
```json
{
  "storeNo": 1
}
```

---

### 3. 견적 신청 API (σ 스냅샷)

#### 3.1 견적 신청 등록
```
POST /api/apply/phone/estimate-regist
```

**Request Body:**
```json
{
  "priceTableRowNo": 123,
  "userSido": "서울",
  "userSigungu": "강남구",
  "userName": "홍길동",
  "userTelNo": "010-1234-5678",
  "memo": "토요일 방문 예정"
}
```

**Response:**
```json
{
  "success": true,
  "applyNo": 456,
  "message": "견적 신청이 완료되었습니다."
}
```

#### 3.2 견적 신청 목록 (내 견적)
```
POST /api/apply/phone/estimate
```

**Request Body:**
```json
{
  "page": 0,
  "size": 20
}
```

**Response:**
```json
{
  "content": [
    {
      "applyNo": 456,
      "productGroupName": "아이폰 16 Pro",
      "productName": "아이폰 16 Pro 256GB",
      "carrier": "SKT",
      "joinType": "번호이동",
      "installmentPrice": 1100000,
      "storeName": "강남모바일",
      "stepCode": "APPLIED",
      "stepName": "신청완료",
      "createdAt": "2024-01-15T10:30:00"
    }
  ]
}
```

#### 3.3 견적 신청 상세
```
POST /api/apply/phone/estimate/{applyNo}
```

#### 3.4 견적 신청 취소
```
POST /api/apply/phone/estimate-cancel/{applyNo}
```

---

### 4. 사용자 관련 API

#### 4.1 카카오 로그인
```
POST /api/login/kakao
```

**Request Body:**
```json
{
  "accessToken": "kakao_access_token_here"
}
```

**Response:**
```json
{
  "success": true,
  "accessToken": "jwt_access_token",
  "refreshToken": "jwt_refresh_token",
  "user": {
    "userNo": 1,
    "userName": "홍길동",
    "userEmail": "hong@example.com",
    "preferredSido": "서울",
    "preferredSigungu": "강남구"
  }
}
```

#### 4.2 회원가입
```
POST /api/login/sign-up/kakao
```

**Request Body:**
```json
{
  "accessToken": "kakao_access_token",
  "userName": "홍길동",
  "userTelNo": "010-1234-5678",
  "preferredSido": "서울",
  "preferredSigungu": "강남구",
  "agreedEventMarketing": true,
  "agreedThirdPartySharing": false
}
```

#### 4.3 내 정보 조회
```
POST /api/user
```

**Headers:**
```
Authorization: Bearer {accessToken}
```

#### 4.4 희망지역 변경
```
POST /api/user/area-modify
```

**Request Body:**
```json
{
  "preferredSido": "경기",
  "preferredSigungu": "수원시"
}
```

#### 4.5 토큰 갱신
```
POST /api/login/token/refresh
```

**Request Body:**
```json
{
  "refreshToken": "jwt_refresh_token"
}
```

---

### 5. 리뷰 관련 API

#### 5.1 상품 리뷰 목록
```
POST /api/review/phone/{productGroupCode}
```

**Request Body:**
```json
{
  "page": 0,
  "size": 10
}
```

**Response:**
```json
{
  "content": [
    {
      "reviewNo": 1,
      "userName": "홍*동",
      "rating": 5,
      "content": "친절하고 좋았습니다",
      "imageUrls": ["/uploads/review/1.jpg"],
      "createdAt": "2024-01-10T14:30:00",
      "storeName": "강남모바일"
    }
  ]
}
```

#### 5.2 리뷰 작성 (개통 후)
```
POST /api/review/phone/estimate-review-regist
```

**Request:** Multipart/form-data
```
applyNo: 456
rating: 5
content: "친절하고 좋았습니다"
images: [File, File, ...]
```

---

### 6. 지역 API

#### 6.1 전체 지역 목록
```
POST /api/area/all
```

**Response:**
```json
{
  "data": [
    {
      "sido": "서울",
      "sigunguList": ["강남구", "강동구", "강북구", ...]
    },
    {
      "sido": "경기",
      "sigunguList": ["수원시", "성남시", "고양시", ...]
    }
  ]
}
```

---

### 7. 캠페인/이벤트 API

#### 7.1 진행 중인 이벤트 캠페인
```
POST /api/campaign/phone/event
```

**Response:**
```json
{
  "content": [
    {
      "campaignNo": 1,
      "campaignName": "아이폰 특가 이벤트",
      "description": "아이폰 16 Pro 최대 50만원 할인",
      "startDate": "2024-01-01",
      "endDate": "2024-01-31",
      "bannerImgUrl": "/uploads/campaign/banner_1.jpg"
    }
  ]
}
```

#### 7.2 캠페인 상세
```
POST /api/campaign/phone/{campaignNo}
```

---

## API 호출 규칙

### 인증
- 로그인 후 발급된 JWT 토큰을 `Authorization` 헤더에 포함
- 형식: `Authorization: Bearer {accessToken}`
- 토큰 만료 시 `/api/login/token/refresh`로 갱신

### 요청 형식
- 대부분의 API는 **POST** 메서드 사용
- Content-Type: `application/json`
- 파일 업로드: `multipart/form-data`

### 응답 형식
```json
{
  "success": true,
  "data": { ... },
  "message": "성공 메시지"
}
```

### 에러 응답
```json
{
  "success": false,
  "errorCode": "AUTH_001",
  "message": "인증이 필요합니다."
}
```

### 페이지네이션
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,        // 현재 페이지 (0-based)
  "size": 20,         // 페이지 크기
  "first": true,
  "last": false
}
```

---

## 새 프론트엔드 연동 우선순위

### Phase 1: 핵심 기능
1. ✅ 지역별 시세표 조회 (`/api/product-group/phone/store/pricetable-row`)
2. ✅ 시/도, 시/군/구 목록 조회 (`/api/product-group/phone/store/pricetable-sido`, `pricetable-sigungu`)
3. ✅ 매장 정보 조회 (`/api/store/{storeNo}`)
4. ✅ 견적 신청 (`/api/apply/phone/estimate-regist`)

### Phase 2: 사용자 기능
1. 카카오 로그인/회원가입
2. 내 견적 목록 조회
3. 희망지역 설정

### Phase 3: 부가 기능
1. 리뷰 조회/작성
2. 캠페인/이벤트
3. 매장 좋아요

---

## 데이터 매핑 (기존 → 새 프론트엔드)

| 기존 API 필드 | 새 프론트엔드 타입 | 설명 |
|--------------|------------------|------|
| `priceTableRowNo` | `Deal.id` | 딜 고유 ID |
| `productGroupName` | `Deal.device.name` | 기기명 |
| `storage` | `Deal.device.storage` | 저장용량 |
| `color` | `Deal.device.color` | 색상 |
| `carrier` | `Deal.carrier` | 통신사 |
| `joinType` | `Deal.joinType` | 가입유형 |
| `discountType` | `Deal.discountType` | 할인유형 |
| `releasePrice` | `Deal.priceVector.msrp` | 출고가 |
| `carrierSubsidy` | `Deal.priceVector.carrierSubsidy` | 공시지원금 |
| `shopSubsidy` | `Deal.priceVector.shopSubsidy` | 매장할인 |
| `installmentPrice` | `Deal.priceVector.installmentPrice` | 할부원금 |
| `monthlyPlan` | `Deal.monthlyPlan` | 요금제 |
| `storeNo` | `Deal.storeId` | 매장 ID |
| `storeName` | `Store.name` | 매장명 |
| `avgRating` | `Store.avgRating` | 평균 평점 |
| `reviewCount` | `Store.reviewCount` | 리뷰 수 |

---

## Trust Score 계산 (제안)

기존 백엔드에 TrustScore가 없으므로, 프론트엔드에서 임시 계산:

```typescript
function calculateTrustScore(store: StoreFromAPI): number {
  const ratingWeight = 0.4;
  const reviewWeight = 0.3;
  const verifiedWeight = 0.3;

  const ratingScore = (store.avgRating / 5) * ratingWeight;
  const reviewScore = Math.min(store.reviewCount / 100, 1) * reviewWeight;
  const verifiedScore = store.isVerified ? verifiedWeight : 0;

  return ratingScore + reviewScore + verifiedScore;
}
```

---

## 환경 변수 설정

```env
# .env.local
NEXT_PUBLIC_API_URL=https://api.nofee.kr/api
NEXT_PUBLIC_KAKAO_APP_KEY=your_kakao_app_key
```
