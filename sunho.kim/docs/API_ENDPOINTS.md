# Nofee Backend API 완전 가이드

> AI 개발 참고용 API 문서 - 모든 엔드포인트, 요청/응답 타입 포함
>
> **마지막 업데이트:** 2025-12-07
> **총 컨트롤러:** 21개 | **총 엔드포인트:** 92개+

---

## 목차

1. [빠른 시작](#빠른-시작)
2. [코드 체계](#코드-체계-필수-이해)
3. [상품 그룹 API](#1-상품-그룹-api-product-groupphone)
4. [시세표 API](#2-시세표-api-pricetablephone)
5. [매장 API](#3-매장-api-store)
6. [지역 API](#4-지역-api-area)
7. [리뷰 API](#5-리뷰-api-review)
8. [캠페인 API](#6-캠페인-api-campaignphone)
9. [이벤트 API](#7-이벤트-api-eventphone)
10. [로그인 API](#8-로그인-api-login)
11. [회원 API](#9-회원-api-user)
12. [인증 API](#10-인증-api-cert)
13. [신청 API](#11-신청-api-apply)
14. [자유게시판 API](#12-자유게시판-api-freeboard)
15. [유선 상품 API](#13-유선-상품-api-productcable)
16. [통신사 통합 API](#14-통신사-통합-api-테스트)
17. [기기 매핑 API](#15-기기-매핑-api-테스트)
18. [발송 API](#16-발송-api-send)

---

## 빠른 시작

### Base URL
```
개발: https://dev-api.nofee.team
운영: https://api.nofee.team
로컬: http://localhost:8080
```

### 공통 헤더
```http
Content-Type: application/json
```

### 인증 방식 (쿠키 기반 JWT)
```typescript
// 인증이 필요한 API 호출 시 반드시 credentials: 'include' 사용
fetch(url, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',  // 필수!
  body: JSON.stringify(data)
})
```

### 공통 응답 형식
```typescript
interface ApiResponse<T> {
  data: T;
  timestamp: string;  // "2025-12-07T00:00:00.000+00:00"
  status: boolean;    // true: 성공, false: 실패
}

// 에러 응답
interface ApiError {
  message: string;
  timestamp: string;
  status: false;
}
```

### HTTP 상태 코드
| 코드 | 설명 | 처리 방법 |
|------|------|-----------|
| 200 | 성공 | - |
| 400 | 잘못된 요청 | 요청 파라미터 확인 |
| 401 | 인증 필요 | 로그인 필요 |
| 403 | 권한 없음/토큰 만료 | 토큰 갱신 후 재시도 |
| 404 | 리소스 없음 | 경로 확인 |
| 500 | 서버 에러 | 재시도 또는 문의 |

---

## 코드 체계 (필수 이해)

### 통신사 코드 (carrierCode)
| 코드 | 통신사 |
|------|--------|
| `0301001001` | SKT |
| `0301001002` | KT |
| `0301001003` | LG U+ |
| `0301001004` | 알뜰폰 |

### 제조사 코드 (manufacturerCode)
| 코드 | 제조사 |
|------|--------|
| `0301002001` | 삼성 (갤럭시) |
| `0301002002` | 애플 (아이폰) |

### 단말 유형 코드 (deviceTypeCode)
| 코드 | 타입 |
|------|------|
| `0301003001` | 4G/LTE |
| `0301003002` | 5G |

### 용량 코드 (storageCode)
| 코드 | 용량 |
|------|------|
| `0301004001` | 64GB |
| `0301004002` | 128GB |
| `0301004003` | 256GB |
| `0301004004` | 512GB |
| `0301004005` | 1TB |
| `0301004999` | 32GB |

### 지원 유형 코드 (supportTypeCode)
| 코드 | 유형 | 설명 |
|------|------|------|
| `0301006001` | 공시지원금 | 기기 할인 |
| `0301006002` | 선택약정 | 요금제 할인 (25%) |

### 가입 유형 코드 (joinTypeCode)
| 코드 | 유형 |
|------|------|
| `0301007001` | 신규가입 |
| `0301007002` | 번호이동 |
| `0301007003` | 기기변경 |

### 월 요금 범위 코드 (priceRangeCode)
| 코드 | 범위 |
|------|------|
| `0301008001` | 5만원 이하 |
| `0301008002` | 5~6만원 |
| `0301008003` | 6~7만원 |
| `0301008004` | 7~8만원 |
| `0301008005` | 8~9만원 |
| `0301008006` | 9만원 이상 |

### 정렬 기준 코드 (orderCode)
| 코드 | 정렬 기준 |
|------|-----------|
| `0401001` | 기본 정렬 |
| `0401004` | 월 납부금 낮은 순 |
| `0401999` | 할부원금 낮은 순 |

### 신청 진행 상태 코드 (stepCode)
| 코드 | 상태 |
|------|------|
| `0201001` | 신청 |
| `0201002` | 진행중 |
| `0201003` | 대응완료 |
| `0201004` | 개통 진행중 |
| `0201005` | 개통완료 |
| `0201006` | 반려 |
| `0201007` | 취소 |

---

## 1. 상품 그룹 API (product-group/phone)

### 1.1 상품 그룹 목록 조회
> 메인 페이지 상품 리스트

```
POST /product-group/phone
```

**Request Body:**
```typescript
interface ReqListProductGroupPhone {
  orderCode?: string;           // 정렬기준 코드 (default: "0401001")
  carrierCodes?: string[];      // 통신사 필터 ["0301001001","0301001002","0301001003"]
  manufacturerCodes?: string[]; // 제조사 필터 ["0301002001","0301002002"]
  supportTypeCodes?: string[];  // 지원방식 필터 ["0301006001","0301006002"]
  priceRangeCodes?: string[];   // 월 요금 필터 ["0301008001"~"0301008005"]
}
```

**cURL:**
```bash
curl -X POST https://dev-api.nofee.team/product-group/phone \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Response:**
```typescript
interface ResProductGroupPhone {
  productGroupCode: string;      // "SM-S-25"
  productGroupNm: string;        // "갤럭시 S25"
  thumbImage: string;            // 썸네일 이미지 URL
  detailImage: string;           // 상세 이미지 URL
  totalView: number;             // 조회수
  totalReview: number;           // 리뷰수
  totalRating: number;           // 평균 평점
  manufacturerCode: string;      // 제조사 코드
  manufacturer: string;          // "갤럭시"
  deviceTypeCode: string;        // 단말 유형 코드
  deviceType: string;            // "5G"
  releasePrice: number;          // 출고가 (원)
  installmentPrincipal: number;  // 할부원금 (음수=캐시백)
  discountPrice: number;         // 할인금액
  discountRate: number;          // 할인율 (%)
  monthRatePlanFee: number;      // 월 요금제 요금
  changeMonthRatePlanFee: number;// 변경 월 요금제 (의무 이후)
  monthPrice: number;            // 월 납부금
  ratePlanMaintainMonth: number; // 요금제 유지 기간 (개월)
  ratePlanCode: string;          // 요금제 코드
  ratePlanNm: string;            // 요금제명
  ratePlanMonthFee: number;      // 요금제 월 기본요금
  storeNo: number;               // 판매점 번호
  sidoNo: number;                // 시도 번호
  sigunguNo: number;             // 시군구 번호
  productCode: string;           // 상품 코드 "SM-S-25-256GB"
  carrierCode: string;           // 통신사 코드
  carrier: string;               // "LG U+"
  storageCode: string;           // 용량 코드
  storage: string;               // "256GB"
  supportTypeCode: string;       // 지원 유형 코드
  supportType: string;           // "공시지원금"
  joinTypeCode: string;          // 가입 유형 코드
  joinType: string;              // "번호이동"
  colors: {                      // 색상 목록
    colorNo: number;
    colorNm: string;
    color: string;               // "#BDBDBF"
  }[];
}
```

---

### 1.2 상품 그룹 상세 조회

```
POST /product-group/phone/{productGroupCode}
```

**Path Parameter:**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| productGroupCode | string | 상품 그룹 코드 (예: SM-S-25) |

**cURL:**
```bash
curl -X POST https://dev-api.nofee.team/product-group/phone/SM-S-25 \
  -H "Content-Type: application/json" \
  -d '{}'
```

---

### 1.3 인기 상품 TOP 10

```
POST /product-group/phone/top
```

**cURL:**
```bash
curl -X POST https://dev-api.nofee.team/product-group/phone/top \
  -H "Content-Type: application/json" \
  -d '{}'
```

---

### 1.4 시세표에 등록된 상품 조회 (페이징)

```
POST /product-group/phone/pricetable-row
```

**Request Body:**
```typescript
interface ReqListPriceTableRow {
  productGroupCode: string;  // 필수: 상품 그룹 코드
  start?: number;            // 페이징 시작 인덱스 (default: 0)
  length?: number;           // 페이지당 항목 수 (default: 10)
}
```

---

### 1.5 시세표 전체 상품 조회 (페이징)

```
POST /product-group/phone/pricetable-row/all
```

**Request Body:**
```typescript
interface ReqListAllPriceTableRow {
  start?: number;   // 페이징 시작 인덱스 (default: 0)
  length?: number;  // 페이지당 항목 수 (default: 10)
}
```

---

### 1.6 판매점별 시세표 조회 (필터)

```
POST /product-group/phone/store/pricetable-row
```

**Request Body:**
```typescript
interface ReqListStorePriceTableRow {
  storeNo?: number;           // 판매점 번호
  sidoNo?: number;            // 시도 번호
  sigunguNo?: number;         // 시군구 번호
  manufacturerCode?: string;  // 제조사 코드
  productGroupCode?: string;  // 상품 그룹 코드
  productCode?: string;       // 상품 코드
  ratePlanGroupNo?: number;   // 요금제 그룹 번호
  carrierCode?: string;       // 통신사 코드
  supportTypeCode?: string;   // 지원 유형 코드
  joinTypeCode?: string;      // 가입 유형 코드
  start?: number;             // 페이징 시작 (default: 0)
  length?: number;            // 페이지당 수 (default: 10)
}
```

---

### 1.7 시세표 필터 조회 API

```
POST /product-group/phone/store/pricetable-sido
POST /product-group/phone/store/pricetable-sigungu
POST /product-group/phone/store/pricetable-manufacturer
POST /product-group/phone/store/pricetable-product-group
POST /product-group/phone/store/pricetable-product
POST /product-group/phone/store/pricetable-rate-plan-group
POST /product-group/phone/store/pricetable-by-carrier
```

---

## 2. 시세표 API (pricetable/phone)

### 2.1 상품별 시세표 히스토리

```
POST /pricetable/phone
```

**Request Body:**
```typescript
interface ReqPricetablePhoneHist {
  productGroupCode: string;  // 상품 그룹 코드
}
```

**cURL:**
```bash
curl -X POST https://dev-api.nofee.team/pricetable/phone \
  -H "Content-Type: application/json" \
  -d '{"productGroupCode":"SM-S-25"}'
```

**Response:**
```typescript
interface ResPricetablePhoneHist {
  datasets: {
    data: {
      pricetableDt: string;          // "2025-07-22"
      productGroupCode: string;
      productCode: string;
      carrierCode: string;
      ratePlanCode: string;
      ratePlanMonthFee: number;
      installmentPrincipal: number;
      monthDevicePrice: number;
      monthPrice: number;
      carrier: string;
      joinType: string;
      storage: string;
    }[];
  }[];
}
```

---

## 3. 매장 API (store)

### 3.1 매장 목록 조회

```
POST /store
```

**Request Body:**
```typescript
interface ReqStoreList {
  // 필터 조건 (optional)
}
```

**cURL:**
```bash
curl -X POST https://dev-api.nofee.team/store \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Response:**
```typescript
interface ResStore {
  storeNo: number;
  nickname: string;
  sidoNo: number;
  sidoNm: string;
  sigunguNo: number;
  sigunguNm: string;
  businessWeek: number;
  businessTimeStart: string;  // "08:00"
  businessTimeEnd: string;    // "21:00"
  bizrNoCertYn: string;       // "Y" or "N"
  presaleConsentCertYn: string;
  view: number;
  review: number;
  reviewAvg: number;
  thumbImage: string;
  bizrNo: string;             // 사업자번호
  representative: string;     // 대표자명
}
```

---

### 3.2 매장 상세 조회

```
POST /store/{storeNo}
```

**Path Parameter:**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| storeNo | number | 매장 번호 |

---

### 3.3 매장 좋아요 (인증 필요)

```
POST /store/favorite
```

**Request Body:**
```typescript
interface ReqStoreFavorite {
  storeNo: number;  // 매장 번호
}
```

---

### 3.4 매장 신고 (인증 필요)

```
POST /store/complaint
```

**Request Body:**
```typescript
interface ReqStoreComplaint {
  storeNo: number;  // 매장 번호
}
```

---

## 4. 지역 API (area)

### 4.1 전체 지역 조회

```
POST /area/all
```

**cURL:**
```bash
curl -X POST https://dev-api.nofee.team/area/all \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Response:**
```typescript
interface ResSido {
  sidoNo: number;
  nm: string;           // 시도명
  sigungu: {
    sigunguNo: number;
    sigunguNm: string;
  }[];
}
```

---

## 5. 리뷰 API (review)

### 5.1 매장 리뷰 TOP (메인 페이지용)

```
GET /review/store/phone/top
```

**cURL:**
```bash
curl -X GET https://dev-api.nofee.team/review/store/phone/top
```

**Response:**
```typescript
interface ResReviewStorePhone {
  reviewNo: number;
  storeNo: number;
  storeNickname: string;
  content: string;
  rating: number;
  createdDt: string;     // "2025. 08. 31"
  images: string[];
  userNm: string;        // 마스킹 처리 "권**"
  storeThumbImage: string;
}
```

---

### 5.2 매장별 리뷰 목록

```
POST /review/store/phone/{storeNo}
```

**Request Body:**
```typescript
interface ReqReviewStorePhoneList {
  // 페이징 등 옵션
}
```

---

### 5.3 매장 리뷰 등록 (인증 필요, multipart)

```
POST /review/store/phone/review-regist
Content-Type: multipart/form-data
```

**Form Data:**
| 필드 | 타입 | 설명 |
|------|------|------|
| data | JSON | ReqReviewStorePhoneRegist |
| images | File[] | 이미지 파일들 (선택) |

**data (JSON):**
```typescript
interface ReqReviewStorePhoneRegist {
  storeNo: number;
  content: string;
  rating: number;
}
```

---

### 5.4 상품 리뷰 목록

```
POST /review/phone/{productGroupCode}
```

---

### 5.5 상품 리뷰 등록 (개통 완료 시, multipart)

```
POST /review/phone/estimate-review-regist
Content-Type: multipart/form-data
```

---

## 6. 캠페인 API (campaign/phone)

### 6.1 캠페인 목록

```
POST /campaign/phone
```

**Request Body:**
```typescript
interface ReqCampaignPhoneList {
  // 필터 조건
}
```

**cURL:**
```bash
curl -X POST https://dev-api.nofee.team/campaign/phone \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Response:**
```typescript
interface ResCampaignPhone {
  campaignNo: number;
  campaignTypeCode: string;
  title: string;
  content: string;
  storeNo: number;
  storeNickname: string;
  images: { image: string }[];
  campaignType: string;  // "소식"
}
```

---

### 6.2 진행중인 이벤트 캠페인 목록

```
POST /campaign/phone/event
```

---

### 6.3 상품별 이벤트 캠페인 목록

```
POST /campaign/phone/product-event
```

---

### 6.4 캠페인 상세

```
POST /campaign/phone/{campaignNo}
```

---

## 7. 이벤트 API (event/phone)

### 7.1 이벤트 목록

```
POST /event/phone
```

---

### 7.2 이벤트 상세

```
POST /event/phone/{eventNo}
```

---

## 8. 로그인 API (login)

### 8.1 카카오 로그인 URI 조회

```
GET /login/kakao/login-uri
```

---

### 8.2 카카오 인가 코드로 AccessToken 발급

```
GET /login/kakao/access-token?code={code}
```

---

### 8.3 카카오 로그인 (토큰 발급)

```
POST /login/kakao
```

**Request Body:**
```typescript
interface ReqKakaoLogin {
  kakaoAccessToken: string;  // 카카오 액세스 토큰
}
```

---

### 8.4 카카오 회원가입

```
POST /login/sign-up/kakao
```

**Request Body:**
```typescript
interface ReqKakaoSignUp {
  kakaoAccessToken: string;
  ageConfirmedYn: string;           // "Y"
  agreedTermsOfServiceYn: string;   // "Y"
  agreedPrivacyPolicyYn: string;    // "Y"
  agreedEventMarketingYn: string;   // "N"
  agreedThirdPartySharingYn: string;// "N"
}
```

---

### 8.5 홈페이지 회원가입

```
POST /login/sign-up/homepage
```

**Request Body:**
```typescript
interface ReqHomepageSignUp {
  userId: string;                    // 회원 아이디 (이메일)
  receiptId: string;                 // 본인인증 토큰 (바로써트)
  ageConfirmedYn: string;            // "Y"
  agreedTermsOfServiceYn: string;    // "Y"
  agreedPrivacyPolicyYn: string;     // "Y"
  agreedEventMarketingYn: string;    // "N"
  agreedThirdPartySharingYn: string; // "N"
}
```

---

### 8.6 토큰 갱신

```
POST /login/token/refresh
```

**인증:** 필요 (credentials: include)

---

### 8.7 로그아웃

```
POST /login/logout
```

**인증:** 필요

---

## 9. 회원 API (user)

### 9.1 회원 정보 조회

```
POST /user
```

**인증:** 필요

**Response:**
```typescript
interface ResUser {
  email: string;
  createType: string;  // "카카오"
  userNm: string;
  gender: string;
  birthday: string;
  telNo: string;
}
```

---

### 9.2 회원 희망 지역 수정

```
POST /user/area-modify
```

**Request Body:**
```typescript
interface ReqUserArea {
  sidoNo: number;
  sigunguNo: number;
}
```

---

### 9.3 회원 추천 정보 수정

```
POST /user/recommend-modify
```

---

### 9.4 회원 맞춤 정보 수정

```
POST /user/custom-information-modify
```

---

### 9.5 이벤트/마케팅 동의 수정

```
POST /user/agreed-event-marketing-modify
POST /user/agreed-third-party-sharing-modify
```

---

## 10. 인증 API (cert)

### 10.1 이메일 인증 번호 요청

```
POST /cert/email/user-id/request
```

**Request Body:**
```typescript
interface ReqEmailUserId {
  email: string;
}
```

---

### 10.2 이메일 인증 코드 검증

```
POST /cert/email/user-id/verify
```

**Request Body:**
```typescript
interface ReqEmailUserIdVerify {
  email: string;
  code: string;
}
```

---

### 10.3 카카오 본인인증 요청 (바로써트)

```
POST /cert/kakaocert/request
```

**Request Body:**
```typescript
interface ReqCert {
  // 본인인증 요청 정보
}
```

---

### 10.4 카카오 본인인증 상태확인

```
POST /cert/kakaocert/status
```

---

### 10.5 카카오 본인인증 서명검증

```
POST /cert/kakaocert/verify
```

---

## 11. 신청 API (apply)

### 11.1 휴대폰 신청 등록 (시세표 기반)

```
POST /apply/regist
```

**인증:** 필요

**Request Body:**
```typescript
interface ReqRegistApply {
  storeNo: number;           // 판매점 번호
  productCode: string;       // 상품 코드 (필수)
  ratePlanCode: string;      // 요금제 코드 (필수)
  carrierCode: string;       // 통신사 코드 (필수)
  supportTypeCode: string;   // 지원 유형 코드 (필수)
  joinTypeCode: string;      // 가입 유형 코드 (필수)
}
```

---

### 11.2 신청 목록 조회

```
POST /apply
```

**인증:** 필요

---

### 11.3 신청 상세 조회

```
POST /apply/{applyNo}
```

---

### 11.4 신청 취소

```
POST /apply/cancel/{applyNo}
```

---

### 11.5 회원 정보 등록

```
POST /apply/user-regist
```

---

### 11.6 회원 정보 조회

```
POST /apply/{applyNo}/user
```

---

### 11.7 신분증 파일 업로드 (multipart)

```
POST /apply/user-id-file-regist
Content-Type: multipart/form-data
```

**Form Data:**
| 필드 | 타입 | 설명 |
|------|------|------|
| data | JSON | { applyNo: number } |
| idFile | File | 신분증 이미지 파일 |

---

### 11.8 휴대폰 견적 신청 API

```
POST /apply/phone/estimate-regist     # 견적 등록
POST /apply/phone/estimate            # 견적 목록
POST /apply/phone/estimate/{applyNo}  # 견적 상세
POST /apply/phone/estimate-cancel/{applyNo}   # 견적 취소
POST /apply/phone/estimate-remove/{applyNo}   # 견적 삭제
POST /apply/phone/estimate/user-regist        # 개통 회원정보 등록
POST /apply/phone/estimate/{applyNo}/user     # 개통 회원정보 조회
POST /apply/phone/estimate/user-id-file-regist # 신분증 업로드
```

**견적 신청 Request:**
```typescript
interface ReqApplyPhoneEstimate {
  productCode: string;        // 상품 코드
  currentCarrierCode: string; // 현재 통신사 코드
  applyCarrierCode: string;   // 신청 통신사 코드
  ratePlanCode: string;       // 요금제 코드
  joinTypeCode: string;       // 가입 유형 코드
  supportTypeCode: string;    // 지원 유형 코드
}
```

---

### 11.9 유선 견적 신청 API

```
POST /apply/cable/estimate-regist
POST /apply/cable/estimate
POST /apply/cable/estimate/{applyNo}
POST /apply/cable/estimate-cancel/{applyNo}
POST /apply/cable/estimate-remove/{applyNo}
```

---

## 12. 자유게시판 API (freeboard)

### 12.1 게시글 등록 (multipart)

```
POST /freeboard/regist
Content-Type: multipart/form-data
```

**인증:** 필요

**Form Data:**
| 필드 | 타입 | 설명 |
|------|------|------|
| data | JSON | ReqFreeBoardRegist |
| images | File[] | 이미지 파일들 (선택) |

---

### 12.2 게시글 목록

```
POST /freeboard/list
```

---

### 12.3 게시글 상세

```
POST /freeboard/{freeboardNo}
```

---

### 12.4 게시글 수정 (multipart)

```
POST /freeboard/modify
```

---

### 12.5 게시글 삭제

```
POST /freeboard/remove
```

---

### 12.6 게시글 좋아요/신고

```
POST /freeboard/favorite
POST /freeboard/complaint
```

---

### 12.7 댓글 API

```
POST /freeboard/comment/regist
POST /freeboard/comment/list
POST /freeboard/comment/modify
POST /freeboard/comment/remove
POST /freeboard/comment/favorite
POST /freeboard/comment/complaint
```

---

## 13. 유선 상품 API (product/cable)

### 13.1 유선 상품 목록

```
POST /product/cable
```

**Request Body:**
```typescript
interface ReqListProductCable {
  // 필터 조건
}
```

---

### 13.2 유선 상품 상세

```
POST /product/cable/{productCode}
```

---

### 13.3 유선 추천 최저가 목록

```
POST /product/cable/lowest-price
```

---

## 14. 통신사 통합 API (테스트)

### 14.1 전체 공시지원금 조회

```
GET /api/test/carrier/subsidies?refresh=false
```

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| refresh | boolean | 캐시 강제 갱신 (default: false) |

**Response:**
```typescript
interface UnifiedSubsidyResponse {
  success: boolean;
  timestamp: string;
  carriers: {
    SKT?: CarrierSubsidy[];
    KT?: CarrierSubsidy[];
    LGU?: CarrierSubsidy[];
  };
  totalCount: number;
}

interface CarrierSubsidy {
  carrier: string;
  deviceName: string;
  deviceCode: string;
  planName: string;
  planMonthlyFee: number;
  subsidy: number;
  joinType: string;
}
```

---

### 14.2 통신사별 공시지원금 조회

```
GET /api/test/carrier/subsidies/{carrier}
```

**Path Parameter:**
| 파라미터 | 값 |
|----------|------|
| carrier | SKT, KT, LGU |

**Query Parameters:**
| 파라미터 | 설명 |
|----------|------|
| planCode | 요금제 코드 |
| joinType | 가입유형 (SKT: 10/20/30, KT: 01/02/04, LGU: 1/2/3) |
| planMonthlyFee | 요금제 월 금액 |
| refresh | 캐시 갱신 여부 |

---

### 14.3 노피 상품코드로 공시지원금 조회

```
GET /api/test/carrier/subsidies/nofee/{nofeeProductCode}
```

**cURL:**
```bash
curl -X GET "https://dev-api.nofee.team/api/test/carrier/subsidies/nofee/SM-S-25-256GB"
```

---

### 14.4 캐시 관리 API

```
GET  /api/test/carrier/cache/status   # 캐시 상태 조회
POST /api/test/carrier/cache/refresh  # 캐시 강제 갱신
DELETE /api/test/carrier/cache        # 캐시 초기화
```

---

### 14.5 헬스 체크

```
GET /api/test/carrier/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "carrier-integration",
  "cache": "Google Sheets (24h TTL)"
}
```

---

## 15. 기기 매핑 API (테스트)

### 15.1 전체 매핑 조회

```
GET /api/test/device-mapping/mappings
```

---

### 15.2 단일 매핑 조회

```
GET /api/test/device-mapping/mapping/{nofeeCode}
```

---

### 15.3 매핑 동기화

```
POST /api/test/device-mapping/sync
```

---

### 15.4 노피 상품 목록 조회

```
GET /api/test/device-mapping/nofee-products
```

---

### 15.5 노피 상품별 공시지원금 조회

```
GET /api/test/device-mapping/subsidies/{nofeeCode}
GET /api/test/device-mapping/subsidies?refresh=false
```

---

## 16. 발송 API (send)

### 16.1 알림톡 발송

```
POST /send/alimtalk
```

**Request Body:**
```typescript
interface SendRequest {
  to: string;              // 수신자 전화번호
  templateCode: string;    // 템플릿 코드
  templateParams: object;  // 템플릿 파라미터
}
```

---

## 프론트엔드 연동 가이드

### fetcher 함수 (lib/api/fetcher.ts)

```typescript
export async function fetcher<T>(
  url: string,
  options: RequestInit = {},
  authInfo: { isAuth?: boolean; isRedirect?: boolean } = {}
): Promise<T> {
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };

  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}${url}`, {
    ...options,
    headers,
    credentials: 'include',  // 쿠키 기반 인증
  });

  // 403 에러 시 토큰 갱신 시도
  if (res.status === 403 && authInfo.isAuth) {
    await fetch(`${process.env.NEXT_PUBLIC_API_URL}/login/token/refresh`, {
      method: 'POST',
      credentials: 'include',
    });
    // 재시도
  }

  return res.json();
}
```

### 사용 예시

```typescript
// 상품 목록 조회
const products = await fetcher<ApiResponse<{ list: ResProductGroupPhone[] }>>(
  '/product-group/phone',
  {
    method: 'POST',
    body: JSON.stringify({ carrierCodes: ['0301001001'] })
  }
);

// 인증 필요한 API
const user = await fetcher<ApiResponse<ResUser>>(
  '/user',
  { method: 'POST' },
  { isAuth: true }
);
```

### 파일 업로드 예시

```typescript
const formData = new FormData();
formData.append('data', JSON.stringify({ storeNo: 1, content: '좋아요', rating: 5 }));
formData.append('images', imageFile1);
formData.append('images', imageFile2);

await fetch(`${API_URL}/review/store/phone/review-regist`, {
  method: 'POST',
  credentials: 'include',
  body: formData,  // Content-Type 자동 설정됨
});
```

---

## 환경 설정

### .env
```env
# 개발
NEXT_PUBLIC_API_URL=https://dev-api.nofee.team

# 운영
NEXT_PUBLIC_API_URL=https://api.nofee.team

# 로컬
NEXT_PUBLIC_API_URL=http://localhost:8080
```

---

## 전체 엔드포인트 요약

| 도메인 | 컨트롤러 | 엔드포인트 수 |
|--------|----------|---------------|
| 상품 그룹 | ProductGroupPhoneController | 13 |
| 시세표 | PricetablePhoneController | 1 |
| 매장 | StoreController | 4 |
| 지역 | AreaController | 1 |
| 리뷰 | ReviewStorePhoneController, ReviewPhoneController | 5 |
| 캠페인 | CampaignPhoneController | 4 |
| 이벤트 | EventPhoneController | 2 |
| 로그인 | LoginController | 8 |
| 회원 | UserController | 6 |
| 인증 | CertController | 5 |
| 신청 | ApplyController, ApplyPhoneController, ApplyCableController | 16 |
| 자유게시판 | FreeBoardController, FreeBoardCommentController | 13 |
| 유선 상품 | ProductCableController | 3 |
| 통신사 통합 | CarrierIntegrationController | 9 |
| 기기 매핑 | DeviceMappingController | 6 |
| 발송 | SendController | 1 |
| **합계** | **21개** | **97개+** |

---

*문서 생성일: 2025-12-07*
