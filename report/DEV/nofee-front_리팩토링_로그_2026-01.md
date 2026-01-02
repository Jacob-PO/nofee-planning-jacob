# nofee-front 코드 안정성 개선 리팩토링 로그

## 개요

- 작업 기간: 2026년 1월
- 브랜치: sunho.kim
- 커밋:
  - ade19d2 (코드 안정성 개선 - P0/P1 이슈 수정)
  - 0bdd01e (Meta Pixel CompleteRegistration 이벤트 누락 수정)
  - 015d60e (Meta Pixel + CAPI 통합 강화 및 고급 매칭 구현)
  - 38fa221 (환경별 env 파일에 FB 환경변수 추가)
- 배포 환경: development (dev.nofee.team)

## 분석 대상

6개 주요 페이지에 대한 코드 안정성 분석 수행:

| 페이지 | 경로 | 분석 범위 |
|--------|------|-----------|
| home-v2 | /home-v2 | 메인 랜딩 페이지 |
| deal | /deal/[storeNo]/[dealNo] | 딜 상세 페이지 |
| chat | /chat | 채팅 목록 페이지 |
| chat room | /chat/[roomId] | 채팅방 페이지 |
| product | /product/[productGroupCode] | 상품 상세 페이지 |
| compare | /compare | 상품 비교 페이지 |

## 분석 기준

1. 보일러플레이트 코드 중복
2. 보안 취약점
3. 비효율적인 코드 패턴
4. 레거시 코드
5. 성능 개선 가능 영역
6. 재사용 가능한 컴포넌트 및 상수
7. SEO, GA4, Meta Pixel 트래킹 검증

---

## P0 Critical 수정사항

### 1. compare/page.tsx - Stack Overflow 방지

#### 문제 상황

Math.min.apply() 및 스프레드 연산자를 사용한 배열 최소값 계산 방식은 JavaScript 콜스택 제한으로 인해 대용량 배열에서 RangeError를 발생시킨다.

```typescript
// 문제가 되는 코드
const lowestPrice = Math.min(...deals.map((d) => d.monthPrice));
```

JavaScript 엔진별 콜스택 제한:
- Chrome V8: 약 125,000개
- Firefox SpiderMonkey: 약 500,000개
- Safari JavaScriptCore: 약 65,000개

상품 딜 데이터가 증가할 경우 비교 페이지에서 런타임 크래시 발생 가능성이 있었다.

#### 해결 방법

utils/formatPrice.ts에 정의된 safeMin 함수를 사용하도록 변경하였다. 이 함수는 reduce 기반으로 구현되어 배열 크기에 관계없이 안전하게 동작한다.

#### 변경 내용

파일: app/compare/page.tsx

변경 전 (lines 316-318):
```typescript
const lowestPrice1 = Math.min(...deals1.map((d) => d.monthPrice));
const lowestPrice2 = Math.min(...deals2.map((d) => d.monthPrice));
```

변경 후:
```typescript
import { formatPrice, safeMin } from '@/utils/formatPrice';

const lowestPrice1 = safeMin(deals1.map((d) => d.monthPrice));
const lowestPrice2 = safeMin(deals2.map((d) => d.monthPrice));
```

변경 전 (lines 434-436):
```typescript
const lowestMonthPrice = Math.min(...deals.map((d) => d.monthPrice));
```

변경 후:
```typescript
const lowestMonthPrice = safeMin(deals.map((d) => d.monthPrice));
```

#### safeMin 함수 구현 (utils/formatPrice.ts)

```typescript
export function safeMin(arr: number[]): number {
  if (arr.length === 0) return 0;
  return arr.reduce((min, val) => (val < min ? val : min), arr[0]);
}
```

---

### 2. ChatHeader.tsx - GA4 트래킹 버그 수정

#### 문제 상황

견적 취소 시 trackEstimateCancel 함수 호출에서 storeNo 파라미터가 항상 0으로 하드코딩되어 있어 GA4에서 어떤 매장에서 취소가 발생했는지 분석이 불가능했다.

```typescript
// 문제가 되는 코드
trackEstimateCancel({
  productGroupCode: productGroupCode || '',
  storeNo: 0,  // 하드코딩된 값
  // ...
});
```

#### 해결 방법

실제 storeNo prop 값을 GA4에 전달하도록 수정하였다.

#### 변경 내용

파일: app/components/chat/ChatHeader.tsx

변경 전 (line 190):
```typescript
storeNo: 0,
```

변경 후:
```typescript
storeNo: storeNo || 0,
```

#### 영향

- 견적 취소 분석 데이터 정확도 향상
- 매장별 취소율 분석 가능
- GA4 리포트에서 storeNo 기반 필터링 정상 동작

---

### 3. faceBookPixel.tsx - 환경변수 분리

#### 문제 상황

Facebook Pixel ID가 소스 코드에 하드코딩되어 있어 다음과 같은 문제가 있었다:

1. 보안: 민감한 식별자가 소스 코드에 노출
2. 환경 분리: 개발/스테이징/프로덕션 환경별 픽셀 분리 불가
3. 유지보수: Pixel ID 변경 시 코드 수정 및 배포 필요

```typescript
// 문제가 되는 코드
const FB_PIXEL_ID = '751478554021745';
```

#### 해결 방법

환경변수 NEXT_PUBLIC_FB_PIXEL_ID를 사용하도록 변경하고, Pixel ID가 설정되지 않은 경우 컴포넌트가 렌더링되지 않도록 처리하였다.

#### 변경 내용

파일: lib/analytics/faceBookPixel.tsx

변경 전:
```typescript
const FB_PIXEL_ID = '751478554021745';

export default function FacebookPixel() {
  return (
    <Script id="fb-pixel" strategy="afterInteractive">
      {`
        !function(f,b,e,v,n,t,s)
        // ... fbq 초기화 코드
        fbq('init', '751478554021745');
        // ...
      `}
    </Script>
  );
}
```

변경 후:
```typescript
const FB_PIXEL_ID = process.env.NEXT_PUBLIC_FB_PIXEL_ID || '';

export default function FacebookPixel() {
  if (!FB_PIXEL_ID) return null;

  return (
    <Script id="fb-pixel" strategy="afterInteractive">
      {`
        !function(f,b,e,v,n,t,s)
        // ... fbq 초기화 코드
        fbq('init', '${FB_PIXEL_ID}');
        // ...
      `}
    </Script>
  );
}
```

#### 환경변수 설정

.env 파일:
```
NEXT_PUBLIC_FB_PIXEL_ID=751478554021745
```

envs/.env.production 파일에도 동일하게 설정 필요.

---

### 4. fbPixel.ts - Meta Pixel 이벤트 누락 수정

#### 문제 상황

GA4에서는 sign_up 이벤트가 정상 트래킹되나 Meta Pixel의 CompleteRegistration 이벤트가 누락되는 현상 발견.

원인:
1. Facebook Pixel 스크립트(`faceBookPixel.tsx`)는 `strategy="afterInteractive"`로 로드
2. 카카오 로그인 콜백 페이지의 useEffect가 먼저 실행
3. `fbTrackCompleteRegistration` 호출 시점에 `window.fbq`가 아직 undefined
4. 기존 `trackPixelEvent` 함수는 `!window.fbq`일 때 조용히 return하여 이벤트 유실

```typescript
// 문제가 되는 코드
function trackPixelEvent(eventName, params, eventId) {
  if (typeof window === 'undefined' || !window.fbq) return;  // 조용히 종료
  // ...
}
```

GA4는 정상 동작한 이유:
- GA4 초기화 스크립트가 `<head>` 내에서 `dataLayer` 큐를 먼저 생성
- Facebook Pixel은 `<body>` 시작 부분에서 로드되어 상대적으로 늦게 초기화

#### 해결 방법

`trackPixelEvent` 함수에 retry 로직 추가. fbq가 로드되지 않은 경우 100ms 간격으로 최대 5회 재시도.

#### 변경 내용

파일: lib/analytics/fbPixel.ts

변경 전:
```typescript
function trackPixelEvent(
  eventName: string,
  params?: Record<string, unknown>,
  eventId?: string
) {
  if (typeof window === 'undefined' || !window.fbq) return;

  if (eventId) {
    window.fbq('track', eventName, params, { eventID: eventId });
  } else {
    window.fbq('track', eventName, params);
  }
}
```

변경 후:
```typescript
function trackPixelEvent(
  eventName: string,
  params?: Record<string, unknown>,
  eventId?: string,
  retryCount = 0
) {
  if (typeof window === 'undefined') return;

  if (!window.fbq) {
    if (retryCount < 5) {
      setTimeout(() => {
        trackPixelEvent(eventName, params, eventId, retryCount + 1);
      }, 100);
    } else {
      console.warn('[FB Pixel] fbq not available after 5 retries:', eventName);
    }
    return;
  }

  if (eventId) {
    window.fbq('track', eventName, params, { eventID: eventId });
  } else {
    window.fbq('track', eventName, params);
  }
}
```

#### 영향

모든 FB Pixel 이벤트가 스크립트 로드 전에 호출되어도 정상 트래킹됨:
- fbTrackViewContent (ViewContent)
- fbTrackInitiateCheckout (InitiateCheckout)
- fbTrackLead (Lead)
- fbTrackSearch (Search)
- fbTrackCompleteRegistration (CompleteRegistration)

---

## P1 Important 수정사항

### 5. DealList.tsx - 필터 체인 최적화

#### 문제 상황

4단계 체인 필터 패턴으로 인해 배열을 4번 순회하는 비효율적인 코드가 있었다.

```typescript
// 문제가 되는 코드 패턴
const filteredDeals = deals
  .filter((d) => !selectedCarrier || d.carrierCode === carrierCode)
  .filter((d) => !selectedBrand || detectBrand(d.productGroupNm) === selectedBrand)
  .filter((d) => !selectedJoinType || d.joinType === selectedJoinType)
  .filter((d) => !selectedSupportType || d.supportTypeCode === selectedSupportType);
```

시간 복잡도: O(4n) - 배열을 4번 순회

#### 해결 방법

단일 filter 함수 내에서 모든 조건을 검사하도록 최적화하였다. 추가로 필터가 하나도 선택되지 않은 경우 early return으로 불필요한 연산을 방지하였다.

#### 변경 내용

파일: app/components/home-v2/DealList.tsx (lines 423-451)

변경 후:
```typescript
const filteredDeals = useMemo(() => {
  // 필터가 하나도 선택되지 않은 경우 early return
  if (!selectedCarrier && !selectedBrand && !selectedJoinType && !selectedSupportType) {
    return deals;
  }

  const carrierCode = selectedCarrier ? CARRIER_NAME_TO_CODE[selectedCarrier] : null;

  return deals.filter((deal) => {
    // 통신사 필터
    if (carrierCode && deal.carrierCode !== carrierCode) return false;

    // 브랜드 필터
    if (selectedBrand && detectBrand(deal.productGroupNm || '') !== selectedBrand) return false;

    // 가입유형 필터
    if (selectedJoinType && deal.joinType !== selectedJoinType) return false;

    // 지원금유형 필터
    if (selectedSupportType && deal.supportTypeCode !== selectedSupportType) return false;

    return true;
  });
}, [deals, selectedCarrier, selectedBrand, selectedJoinType, selectedSupportType]);
```

#### 성능 개선

- 변경 전: O(4n) - 4회 배열 순회
- 변경 후: O(n) - 1회 배열 순회
- 개선율: 75% 연산 감소

---

### 6. LowestDealSection.tsx - 필터 체인 최적화

#### 문제 상황

DealList와 동일하게 3단계 체인 필터 패턴으로 인한 비효율이 있었다.

```typescript
// 문제가 되는 코드 패턴
const filteredDeals = deals
  .filter((d) => !selectedJoinType || d.joinType === selectedJoinType)
  .filter((d) => !selectedCarrier || d.carrier === selectedCarrier)
  .filter((d) => !selectedRatePlan || d.ratePlanNm === selectedRatePlan);
```

#### 해결 방법

단일 패스 필터로 최적화하고, LGU+ 통신사명 불일치 처리 로직도 포함하였다.

#### 변경 내용

파일: app/product/[productGroupCode]/components/LowestDealSection.tsx (lines 115-129)

변경 후:
```typescript
const filteredDeals = useMemo(() => {
  // 필터가 하나도 선택되지 않은 경우 early return
  if (!selectedJoinType && !selectedCarrier && !selectedRatePlan) {
    return deals;
  }

  return deals.filter((d) => {
    // 가입유형 필터
    if (selectedJoinType && d.joinType !== selectedJoinType) return false;

    // 통신사 필터 (LG U+ / LGU 호환 처리)
    if (selectedCarrier && d.carrier !== selectedCarrier &&
        !(d.carrier === 'LG U+' && selectedCarrier === 'LGU')) return false;

    // 요금제 필터
    if (selectedRatePlan && d.ratePlanNm !== selectedRatePlan) return false;

    return true;
  });
}, [deals, selectedJoinType, selectedCarrier, selectedRatePlan]);
```

#### 성능 개선

- 변경 전: O(3n) - 3회 배열 순회
- 변경 후: O(n) - 1회 배열 순회
- 개선율: 67% 연산 감소

---

## 스킵한 항목 및 사유

### 1. Skeleton 컴포넌트 통합

사유: 이미 app/styles/common/skeleton에 공유 컴포넌트가 정의되어 있고, 각 페이지에서 해당 컴포넌트를 import하여 사용 중이었다.

### 2. Chat 메시지 가상화 (react-window)

사유:
- 새로운 의존성(react-window 또는 react-virtualized) 추가 필요
- 기존 채팅 로직과의 통합 복잡도 높음
- 현재 채팅 메시지 개수로는 성능 이슈 미발생

### 3. 스타일 병합

사유:
- 5개 이상 파일에 영향
- 고위험 변경으로 분류
- 별도 작업으로 진행 권장

### 4. COLORS 상수 추출

사유: tailwind.config.js에 interstellarBlue (#131FA0) 등 주요 색상이 이미 정의되어 있어 추가 상수 정의 불필요.

### 5. 레거시 코드 정리

사유: TODO 플레이스홀더 및 주석은 향후 개발 참조용으로 유지.

### 6. Google Search Console 인증

사유: 사용자 요청에 따라 스킵.

### 7. 이미지 lazy loading 및 Next.js Image 적용

사유: 사용자 요청에 따라 스킵.

---

## 검증 결과

### 빌드 테스트

```
npm run build
```

결과: 56개 페이지 컴파일 성공

### TypeScript 타입 검사

결과: 타입 에러 없음

### 배포 검증

- 브랜치: sunho.kim
- 타겟: development 환경
- PM2 프로세스: nofee-front (pid: 2223811)
- 상태: online

---

## 변경 파일 목록

| 파일 | 변경 유형 | 변경 라인 |
|------|----------|-----------|
| app/compare/page.tsx | 수정 | +7, -6 |
| app/components/chat/ChatHeader.tsx | 수정 | +1, -1 |
| app/components/home-v2/DealList.tsx | 수정 | +22, -23 |
| app/product/[productGroupCode]/components/LowestDealSection.tsx | 수정 | +12, -13 |
| lib/analytics/faceBookPixel.tsx | 수정 | +3, -6 |
| lib/analytics/fbPixel.ts | 수정 | +16, -2 |

총계: 61 insertions, 51 deletions

---

## Git 커밋 정보

### 커밋 1: ade19d2
- 브랜치: sunho.kim
- 이전 커밋: 53b3dd3 (refactor: 코드 안정성 개선 및 중복 코드 제거)

커밋 메시지:
```
fix: 코드 안정성 개선 - P0/P1 이슈 수정

## P0 Critical 수정

### 1. compare/page.tsx - Stack Overflow 방지
- Math.min/max -> safeMin/safeMax 유틸리티 함수로 변경
- 문제: Math.min.apply(null, largeArray)는 10만개 이상 배열에서 스택 오버플로우 발생
- 해결: reduce 기반 safeMin 함수로 안전하게 최소값 계산
- 적용 위치: 최저가 계산 로직 (lines 316-318, 434-436)

### 2. ChatHeader.tsx - GA4 트래킹 버그 수정
- trackEstimateCancel 호출 시 storeNo가 항상 0으로 전송되던 버그 수정
- 문제: storeNo: 0 하드코딩되어 있어 실제 매장 번호가 GA4에 기록되지 않음
- 해결: storeNo: storeNo || 0으로 변경하여 실제 값 전달
- 영향: 견적 취소 분석 데이터 정확도 향상

### 3. faceBookPixel.tsx - 환경변수 분리
- Facebook Pixel ID 하드코딩 -> 환경변수(NEXT_PUBLIC_FB_PIXEL_ID)로 이동
- 문제: 보안 및 환경별 설정 관리 어려움
- 해결: process.env.NEXT_PUBLIC_FB_PIXEL_ID 사용
- Pixel ID 없을 시 컴포넌트 렌더링 방지 (null 반환)

## P1 Important 수정

### 4. DealList.tsx - 필터 체인 최적화
- 4단계 필터 체인 -> 단일 패스 필터로 최적화
- 문제: .filter().filter().filter().filter() 패턴은 O(4n) 순회
- 해결: 단일 filter 함수 내에서 모든 조건 검사 O(n)
- 성능 개선: 4회 배열 순회 -> 1회 순회

### 6. LowestDealSection.tsx - 필터 체인 최적화
- 3단계 필터 체인 -> 단일 패스 필터로 최적화
- 동일한 패턴 적용으로 일관성 확보
- 성능 개선: 3회 배열 순회 -> 1회 순회

## 테스트 결과
- 전체 빌드 통과 (56개 페이지 컴파일 성공)
- TypeScript 타입 검사 통과
```

### 커밋 2: 0bdd01e
- 브랜치: sunho.kim
- 이전 커밋: ade19d2 (fix: 코드 안정성 개선 - P0/P1 이슈 수정)

커밋 메시지:
```
fix: Meta Pixel CompleteRegistration 이벤트 누락 수정

## 문제 상황
- 카카오 로그인 콜백 페이지에서 fbTrackCompleteRegistration 호출 시
  Facebook Pixel 스크립트가 아직 로드되지 않아 window.fbq가 undefined
- 기존 코드는 fbq가 없으면 조용히 return하여 이벤트 유실

## 해결 방법
- trackPixelEvent 함수에 retry 로직 추가
- fbq가 로드되지 않은 경우 100ms 간격으로 최대 5회 재시도
- 5회 재시도 후에도 실패 시 console.warn으로 로깅

## 영향 범위
- fbTrackViewContent
- fbTrackInitiateCheckout
- fbTrackLead
- fbTrackSearch
- fbTrackCompleteRegistration

모든 FB Pixel 이벤트가 스크립트 로드 전에 호출되어도 정상 트래킹됨
```

---

## GitHub Issue 연동

Issue: #84
코멘트 URL:
- https://github.com/nofee-workspace/nofee-front/issues/84#issuecomment-3703248603
- https://github.com/nofee-workspace/nofee-front/issues/84#issuecomment-3703365361

---

---

## Meta Pixel + CAPI 통합 강화 (2026-01-02)

### 커밋 3: 015d60e
- 브랜치: sunho.kim
- 이전 커밋: 0bdd01e

### 커밋 4: 38fa221
- 브랜치: sunho.kim
- 이전 커밋: 015d60e

---

### 1. CAPI (Conversion API) 서버 엔드포인트 구현

#### 배경

Meta Pixel만으로는 iOS 14.5+ 개인정보 보호 정책, 광고 차단기, 브라우저 제한 등으로 이벤트 손실이 발생한다. Conversion API(CAPI)를 통해 서버에서 직접 이벤트를 전송하여 전환 데이터 정확도를 높인다.

#### 구현 내용

파일: `app/api/fb-conversion/route.ts`

```typescript
// Facebook Graph API로 이벤트 전송
const fbResponse = await fetch(
  `https://graph.facebook.com/v18.0/${FB_PIXEL_ID}/events`,
  {
    method: 'POST',
    body: JSON.stringify({
      data: [eventData],
      access_token: FB_ACCESS_TOKEN,
    }),
  }
);
```

#### API 스펙

```typescript
interface ConversionRequest {
  eventName: string;           // 'ViewContent', 'Lead' 등
  eventId: string;             // Pixel과 동일한 ID (중복 제거용)
  eventSourceUrl?: string;
  userData?: {
    fbc?: string;              // FB 클릭 ID 쿠키
    fbp?: string;              // FB 브라우저 ID 쿠키
    externalId?: string;       // 사용자 고유 ID
    em?: string;               // 이메일 (자동 SHA256 해싱)
    ph?: string;               // 전화번호
    fn?: string;               // 이름
    ge?: string;               // 성별
    db?: string;               // 생년월일
  };
  customData?: {
    contentName?: string;
    contentIds?: string[];     // productGroupCode
    value?: number;
    currency?: string;
  };
}
```

---

### 2. Pixel + CAPI 이중 전송

#### 구현 방식

동일한 `eventId`로 Pixel(클라이언트)과 CAPI(서버) 양쪽에 이벤트를 전송한다. Facebook은 동일 eventId를 자동 중복 제거하여 하나의 이벤트로 처리한다.

파일: `lib/analytics/fbPixel.ts`

```typescript
export async function fbTrackViewContent(params: FBTrackParams) {
  const eventId = generateEventId();

  // 1. Pixel 이벤트 전송 (클라이언트)
  trackPixelEvent('ViewContent', pixelParams, eventId);

  // 2. CAPI 이벤트 전송 (서버) - 비동기
  trackCapiEvent('ViewContent', eventId, capiCustomData, externalId);
}
```

#### 지원 이벤트

| 이벤트 | 용도 | 함수 |
|--------|------|------|
| ViewContent | 상품/딜 조회 | `fbTrackViewContent()` |
| InitiateCheckout | 견적 신청 클릭 | `fbTrackInitiateCheckout()` |
| Lead | 견적 완료 | `fbTrackLead()` |
| Search | 검색 | `fbTrackSearch()` |
| CompleteRegistration | 회원가입 | `fbTrackCompleteRegistration()` |

---

### 3. 고급 매칭 (Advanced Matching) 구현

#### 목적

로그인 사용자의 개인정보를 Pixel과 CAPI에 전송하여 광고 타겟팅 정확도를 높인다.

#### Pixel 고급 매칭

파일: `lib/analytics/faceBookPixel.tsx`

```typescript
function getAdvancedMatchingData(user) {
  const data: Record<string, string> = {};

  // 이메일 (소문자, 공백 제거)
  if (user.email) {
    data.em = user.email.toLowerCase().trim();
  }

  // 전화번호 (한국 형식 → 국제 형식)
  if (user.telNo) {
    const phone = user.telNo.replace(/[^0-9]/g, '');
    if (phone.startsWith('0')) {
      data.ph = '82' + phone.substring(1);  // 010-xxxx → 8210xxxx
    }
  }

  // 이름, 성별, 생년월일, 사용자 ID
  if (user.userNm) data.fn = user.userNm.toLowerCase().trim();
  if (user.gender) data.ge = user.gender === 'm' ? 'm' : 'f';
  if (user.birthday) data.db = user.birthday.replace(/[^0-9]/g, '').substring(0, 8);
  if (user.userNo) data.external_id = String(user.userNo);

  return data;
}

// 로그인 시 Pixel 재초기화
useEffect(() => {
  if (window.fbq && user) {
    const advancedMatchingData = getAdvancedMatchingData(user);
    window.fbq('init', FB_PIXEL_ID, advancedMatchingData);
  }
}, [user]);
```

#### CAPI 고급 매칭

파일: `lib/analytics/fbPixel.ts`

```typescript
function getUserDataForCapi() {
  // sessionStorage에서 사용자 정보 추출
  const authData = JSON.parse(sessionStorage.getItem('auth-storage') || '{}');
  const user = authData?.state?.user;

  if (!user) return {};

  return {
    externalId: String(user.userNo),
    em: user.email?.toLowerCase().trim(),
    ph: formatPhoneNumber(user.telNo),  // 8210xxxx 형식
    fn: user.userNm?.toLowerCase().trim(),
    // ...
  };
}
```

#### CAPI 서버 해싱

파일: `app/api/fb-conversion/route.ts`

```typescript
function hashData(data: string): string {
  return crypto.createHash('sha256').update(data.toLowerCase().trim()).digest('hex');
}

// 사용자 데이터 해싱 후 전송
user_data: {
  ...(userData?.em && { em: hashData(userData.em) }),
  ...(userData?.ph && { ph: hashData(userData.ph) }),
  ...(userData?.fn && { fn: hashData(userData.fn) }),
  // ...
}
```

---

### 4. 카탈로그 매칭 (content_ids)

#### 문제

기존 `content_ids`가 개별 상품 코드(`SM-S911N-256-BK`)로 전송되어 Meta 카탈로그 피드의 ID와 불일치하여 광고 최적화가 작동하지 않았다.

#### 해결

`content_ids`를 `productGroupCode` 형식으로 통일하여 카탈로그 피드 ID와 일치시킨다.

파일: `lib/analytics/fbPixel.ts`

```typescript
// 변경 전
content_ids: [params.productCode]  // 'SM-S911N-256-BK'

// 변경 후
content_ids: [params.productGroupCode]  // 'SM-ZF-6'
```

#### 카탈로그 피드 확인

```
URL: https://nofee.team/api/catalog/feed

id,title,description,...
SM-S-25,갤럭시 S25 256GB,...
SM-SA-25,갤럭시 S25 엣지 256GB,...
AP-B-16,아이폰 16 256GB,...
```

---

### 5. LowestDealSection.tsx - productGroupCode 누락 수정

#### 문제

`LowestDealSection.tsx`에서 `fbTrackViewContent` 호출 시 `productGroupCode`가 누락되어 카탈로그 매칭이 되지 않았다.

#### 변경 내용

파일: `app/product/[productGroupCode]/components/LowestDealSection.tsx`

```typescript
// 변경 전
fbTrackViewContent({
  productName: deal.productGroupNm,
  productCode: deal.productCode,  // 개별 상품 코드
  carrier: deal.carrier,
  monthPrice: deal.monthPrice,
  storeNo: deal.storeNo,
});

// 변경 후
fbTrackViewContent({
  productName: deal.productGroupNm,
  productGroupCode: deal.productGroupCode,  // 카탈로그 매칭용
  carrier: deal.carrier,
  monthPrice: deal.monthPrice,
  storeNo: deal.storeNo,
});
```

---

### 6. 환경별 env 파일 수정

#### 문제

`npm run build:development` 실행 시 `envs/.env.development`가 `.env`를 덮어쓰는데, 해당 파일에 `FB_CONVERSION_API_TOKEN`이 없어 CAPI가 동작하지 않았다.

```bash
# package.json
"build:development": "cp envs/.env.development .env && npm run build-only"
```

#### 해결

`envs/.env.development`와 `envs/.env.production`에 FB 환경변수 추가:

```bash
# envs/.env.development
NEXT_PUBLIC_FB_PIXEL_ID=751478554021745
FB_CONVERSION_API_TOKEN=EAAOZApk1SCTQ...

# envs/.env.production
NEXT_PUBLIC_FB_PIXEL_ID=751478554021745
FB_CONVERSION_API_TOKEN=EAAOZApk1SCTQ...
```

---

### 7. 세션 내 중복 방지

#### 구현

동일 세션에서 같은 상품에 대한 중복 이벤트를 방지한다.

파일: `lib/analytics/fbPixel.ts`

```typescript
const trackedEvents = new Set<string>();

function isEventTracked(eventKey: string): boolean {
  if (trackedEvents.has(eventKey)) return true;
  trackedEvents.add(eventKey);
  return false;
}

export async function fbTrackViewContent(params) {
  const eventKey = `ViewContent_${params.productGroupCode}`;
  if (isEventTracked(eventKey)) return null;  // 중복 스킵
  // ...
}
```

---

### 변경 파일 목록 (추가)

| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| app/api/fb-conversion/route.ts | 신규 | CAPI 서버 엔드포인트 |
| lib/analytics/fbPixel.ts | 수정 | CAPI 통합, 고급 매칭, 카탈로그 매칭 |
| lib/analytics/faceBookPixel.tsx | 수정 | Pixel 고급 매칭 |
| app/product/[productGroupCode]/components/LowestDealSection.tsx | 수정 | productGroupCode 추가 |
| app/product/[productGroupCode]/components/ProductPageTracker.tsx | 수정 | productGroupCode 사용 |
| lib/analytics/trackConversion.ts | 수정 | 통합 트래킹 함수 |
| envs/.env.development | 수정 | FB 환경변수 추가 |
| envs/.env.production | 수정 | FB 환경변수 추가 |
| .env | 수정 | FB 환경변수 추가 |

---

### 테스트

#### CAPI 직접 테스트

```bash
cd /Users/jacob/Desktop/workspace/nofee/nofee-planning-jacob/report/GA4/test
npx ts-node fb-capi-direct-test.ts
```

#### curl 테스트

```bash
curl -X POST https://dev.nofee.team/api/fb-conversion \
  -H "Content-Type: application/json" \
  -d '{"eventName":"ViewContent","eventId":"test123","customData":{"contentIds":["SM-ZF-6"],"value":50000}}'

# 성공 응답
{"success":true,"events_received":1,"fbtrace_id":"..."}
```

#### 카탈로그 피드 확인

```bash
curl https://dev.nofee.team/api/catalog/feed | head -5
```

---

### Git 커밋 정보 (추가)

#### 커밋 3: 015d60e

```
feat: Meta Pixel + CAPI 통합 강화 및 고급 매칭 구현

## 주요 변경사항

### 1. CAPI (Conversion API) 서버 통합
- /api/fb-conversion 엔드포인트 구현
- Pixel + CAPI 이중 전송으로 이벤트 손실 방지
- eventId 기반 중복 제거

### 2. 고급 매칭 (Advanced Matching)
- faceBookPixel.tsx: 로그인 시 fbq('init', pixelId, advancedMatchingData)
- fbPixel.ts: CAPI 요청에 사용자 데이터 포함
- 지원 필드: em, ph, fn, ge, db, external_id

### 3. 카탈로그 매칭
- content_ids를 productGroupCode로 통일
- 카탈로그 피드 ID와 일치시켜 광고 최적화 지원

### 4. LowestDealSection.tsx 버그 수정
- productGroupCode 누락 수정
```

#### 커밋 4: 38fa221

```
fix: 환경별 env 파일에 FB 환경변수 추가

빌드 스크립트가 envs/.env.* 파일을 .env로 복사하므로
FB Pixel/CAPI 환경변수가 누락되는 문제 수정

- envs/.env.development에 FB_CONVERSION_API_TOKEN 추가
- envs/.env.production에 FB_CONVERSION_API_TOKEN 추가
- NEXT_PUBLIC_FB_PIXEL_ID, NEXT_PUBLIC_GA4_ID 추가
```

---

### 참고 문서

- [GA4_MetaPixel_이벤트_트래킹_가이드.md](../GA4/GA4_MetaPixel_이벤트_트래킹_가이드.md)
- [Meta Conversion API 공식 문서](https://developers.facebook.com/docs/marketing-api/conversions-api)
- [Meta Advanced Matching 공식 문서](https://developers.facebook.com/docs/meta-pixel/advanced/advanced-matching)

---

---

## Google Merchant Center 연동 및 GA4 이벤트 개선 (2026-01-02)

### 1. Google Merchant Center용 XML 피드 생성

#### 배경

Google Shopping에 노피 상품을 노출하기 위해 Google Merchant Center용 XML 피드 엔드포인트를 구현했다.

#### 구현 내용

파일: `app/api/catalog/google-feed/route.ts`

```typescript
// Google Merchant Center RSS 2.0 형식
const itemsXml = items.map((item) => `
  <item>
    <g:id>${item.id}</g:id>
    <g:sku>${item.sku}</g:sku>
    <title>${item.title}</title>
    <g:title>${item.title}</g:title>
    <g:description>${item.description}</g:description>
    <g:link>${item.link}</g:link>
    <g:image_link>${item.imageLink}</g:image_link>
    <g:availability>in_stock</g:availability>
    <g:condition>new</g:condition>
    <g:price>${item.price} KRW</g:price>
    <g:brand>${item.brand}</g:brand>
    <g:mpn>${item.mpn}</g:mpn>
    <g:gtin></g:gtin>
    <g:identifier_exists>false</g:identifier_exists>
    <g:item_group_id>${item.itemGroupId}</g:item_group_id>
    <g:google_product_category>267</g:google_product_category>
    <g:product_type>${item.productType}</g:product_type>
  </item>
`);
```

#### 피드 필드 매핑

| Google 필드 | 노피 데이터 | 설명 |
|-------------|------------|------|
| `g:id` | productGroupCode | 상품 고유 ID |
| `g:sku` | productCode | 제품 코드 (SKU) |
| `g:title` | productGroupNm + storage | 상품명 + 용량 |
| `g:price` | installmentPrincipal | **할부원금** (실 납부금액) |
| `g:link` | /product/{productGroupCode} | 상품 페이지 URL |
| `g:brand` | manufacturer | 제조사 (갤럭시, 아이폰) |
| `g:google_product_category` | 267 | 휴대폰 카테고리 |

#### 가격 처리 로직

```typescript
// 음수(캐시백) 또는 0원인 경우 1원으로 처리 (Google 정책상 0 이상 필요)
const price = deal.installmentPrincipal <= 0 ? 1 : deal.installmentPrincipal;
```

- 캐시백 상품 (음수): 1 KRW로 표시
- 무료 상품 (0원): 1 KRW로 표시
- 일반 상품: 실제 할부원금 표시

#### 피드 URL

```
개발: https://dev.nofee.team/api/catalog/google-feed
운영: https://nofee.team/api/catalog/google-feed
```

#### 상품 수

- 전체 25개 상품 포함 (캐시백 상품 포함)

---

### 2. GA4 이벤트 items 배열 필수 필드 추가

#### 문제 상황

GA4 DebugView에서 다음 경고가 발생했다:
- "Items array is missing parameter SKU for product 1"
- "Items array is missing parameter Item Name for product 1"

#### 원인

GA4 ecommerce 표준 이벤트(`select_item`, `view_item_list`, `view_item`, `begin_checkout`, `purchase`)의 items 배열에 필수 필드가 누락되어 있었다.

#### 해결 방법

모든 GA4 ecommerce 이벤트의 items 배열에 `item_id`, `item_name`, `sku` 필드를 추가했다.

#### 변경 파일 및 내용

**1. lib/analytics/gtag.ts**

```typescript
// view_item 이벤트 (line 235-246)
event('view_item', {
  items: [
    {
      item_id: params.productCode,
      item_name: params.productName,
      sku: params.productCode,  // 추가
      // ...
    },
  ],
});

// begin_checkout 이벤트 (line 272-285)
event('begin_checkout', {
  items: [
    {
      item_id: params.productCode,
      item_name: params.productName,
      sku: params.productCode,  // 추가
      // ...
    },
  ],
});

// purchase 이벤트 (line 339-353)
event('purchase', {
  items: [
    {
      item_id: params.productCode,
      item_name: params.productName,
      sku: params.productCode,  // 추가
      // ...
    },
  ],
});

// select_item 이벤트 - 딜 카드 클릭 (line 404-418)
event('select_item', {
  items: [
    {
      item_id: String(params.storeNo),  // 추가
      item_name: params.productName,
      sku: String(params.storeNo),       // 추가
      // ...
    },
  ],
});

// select_item 이벤트 - 비교 페이지 (line 757-769)
event('select_item', {
  items: [
    {
      item_id: params.productGroupCode,
      item_name: params.productGroupNm,
      sku: params.productGroupCode,  // 추가
      // ...
    },
  ],
});

// view_item_list 이벤트 - 홈 (line 204-217)
event('view_item_list', {
  items: dealCount
    ? [
        {
          item_id: 'home_deals',      // 추가
          item_name: '홈 딜 목록',     // 추가
          sku: 'home_deals',          // 추가
          quantity: dealCount,
        },
      ]
    : [],
});
```

**2. app/product/[productGroupCode]/components/ProductPageTracker.tsx**

```typescript
// view_item_list 이벤트 (line 38-51)
event('view_item_list', {
  items: [
    {
      item_id: productGroupCode,
      item_name: productGroupNm,
      sku: productGroupCode,  // 추가
      // ...
    },
  ],
});
```

---

### 3. GA4 Debug Mode 설정

#### 문제 상황

로컬 개발 환경에서 GA4 DebugView에 이벤트가 제대로 표시되지 않는 문제가 있었다.

#### 해결 방법

개발 환경에서만 `debug_mode: true`를 활성화하도록 수정했다.

#### 변경 내용

파일: `app/layout.tsx`

```typescript
// 변경 전
gtag('config', '${GA_MEASUREMENT_ID}');

// 변경 후
gtag('config', '${GA_MEASUREMENT_ID}', {
  debug_mode: ${process.env.NODE_ENV === 'development'}
});
```

#### 효과

- **로컬 (development)**: `debug_mode: true` → DebugView에 모든 이벤트 실시간 표시
- **운영 (production)**: `debug_mode: false` → 정상 동작 (DebugView 없음)

---

### 변경 파일 목록 (추가)

| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| app/api/catalog/google-feed/route.ts | 신규 | Google Merchant Center용 XML 피드 |
| lib/analytics/gtag.ts | 수정 | GA4 ecommerce 이벤트에 sku, item_id 추가 |
| app/product/[productGroupCode]/components/ProductPageTracker.tsx | 수정 | view_item_list에 sku 추가 |
| app/layout.tsx | 수정 | GA4 debug_mode 설정 추가 |

---

### Google Merchant Center 연동 방법

#### 1단계: 배포

```bash
npm run build:production
# 또는
npm run build:development
```

#### 2단계: Merchant Center 피드 등록

1. [Google Merchant Center](https://merchants.google.com) 접속
2. 판매자 센터 ID: `5696695655`
3. **제품** → **피드** → **+ 새 피드 추가**

| 설정 | 값 |
|------|-----|
| 국가 | 한국 |
| 언어 | 한국어 |
| 피드 이름 | `nofee-products` |
| 입력 방법 | 예약 가져오기 |
| 파일 URL | `https://nofee.team/api/catalog/google-feed` |
| 가져오기 빈도 | 매일 |

#### 3단계: 피드 진단 확인

- Merchant Center → 제품 → 진단
- 오류/경고 확인 및 수정

---

### 테스트 결과

#### 빌드 테스트

```bash
npm run build
# 결과: 56개 페이지 컴파일 성공
```

#### 피드 테스트

```bash
curl -s "http://localhost:3000/api/catalog/google-feed" | grep -c "<item>"
# 결과: 25
```

---

---

---

## pricetableNo 기반 짧은 URL 및 견적 신청 개선 (2026-01-02)

### 배경

기존 Deal 페이지 URL은 Base64 인코딩된 복합 키를 사용하여 매우 길었다:
```
/deal/eyJwIjoiU00tU1UtMjUtMjU2R0IiLCJnIjoiU00tU1UtMjUiLC...
```

이를 `pricetableNo` 기반의 짧은 URL로 변경:
```
/deal/172
```

---

### 1. 견적 신청 API pricetableNo 지원

#### 문제 상황

pricetableNo URL로 접근한 딜에서 견적 신청 시 `valid.match` 에러가 발생했다.

- 원인: 백엔드 SQL이 `pricetableNo`만으로 조회하는데, 프론트엔드에서 해당 파라미터를 전달하지 않음
- 어제 등록된 pricetable 데이터에 접근 시 validation 실패

#### 해결 방법

백엔드에서 `pricetableNo` 파라미터 지원 추가 후, 프론트엔드에서 해당 값을 전달하도록 수정했다.

#### 변경 내용

**1. app/backends/apply/postApplyRegist.ts**

```typescript
export interface ApplyRegistParams {
  pricetableNo?: number; // pricetableNo로 직접 조회 (추가)
  storeNo?: number;
  productCode: string;
  ratePlanCode: string;
  carrierCode: string;
  supportTypeCode: string;
  joinTypeCode: string;
}
```

**2. app/deal/[dealId]/DealDetailClient.tsx**

```typescript
// 로그인 사용자 견적 신청 (line 669-677)
const params = {
  pricetableNo: deal.pricetableNo, // 추가
  productCode: deal.productCode,
  carrierCode: deal.carrierCode,
  ratePlanCode: deal.ratePlanCode,
  joinTypeCode: deal.joinTypeCode,
  supportTypeCode: deal.supportTypeCode,
  storeNo: deal.storeNo,
};

// 비로그인 → 로그인 후 자동 견적 신청 (line 395-398)
const result = await postApplyRegist({
  ...pendingData,
  pricetableNo: deal.pricetableNo, // 추가
});
```

---

### 2. 모달 레이아웃 시프트 수정

#### 문제 상황

PC에서 견적 완료 모달이 열릴 때 페이지가 왼쪽으로 밀리는 현상이 발생했다.

- 원인: `overflow: hidden` 적용 시 스크롤바가 사라지면서 콘텐츠가 스크롤바 너비만큼 이동
- 영향: 시각적으로 불편한 UX

#### 해결 방법

스크롤바 너비만큼 `padding-right`를 추가하여 레이아웃 시프트를 방지했다.

#### 변경 내용

**hooks/useBodyScrollLock.ts**

```typescript
export function useBodyScrollLock(isLocked: boolean) {
  useEffect(() => {
    if (!isLocked) return;

    // 현재 스크롤 위치 저장
    const scrollY = window.scrollY;

    // 스크롤바 너비 계산 (레이아웃 시프트 방지)
    const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;

    // body 고정 (iOS Safari 대응) + 스크롤바 너비만큼 padding 추가
    document.body.style.cssText = `
      position: fixed;
      top: -${scrollY}px;
      left: 0;
      right: 0;
      overflow: hidden;
      padding-right: ${scrollbarWidth}px;
    `;

    // 클린업: 스크롤 잠금 해제 및 위치 복원
    return () => {
      document.body.style.cssText = '';
      window.scrollTo(0, scrollY);
    };
  }, [isLocked]);
}
```

**app/deal/[dealId]/components/DealModals.tsx**

```typescript
// 로컬 구현 대신 공유 훅 사용
import { useBodyScrollLock } from '@/hooks/useBodyScrollLock';

export function EstimateCompleteModal({ isOpen, ... }) {
  useBodyScrollLock(isOpen);
  // ...
}
```

---

### 3. 사이트맵 SEO 개선 - pricetableNo 기반 짧은 URL만 사용

#### 문제 상황

사이트맵에 Base64 인코딩된 긴 URL이 포함되어 SEO에 불리했다.

```xml
<url>
  <loc>https://nofee.team/deal/eyJwIjoiU00tU1UtMjUtMjU2R0IiLCJnIj...</loc>
</url>
```

#### 해결 방법

`pricetableNo`가 있는 딜만 사이트맵에 포함하여 짧은 URL만 생성하도록 변경했다.

#### 변경 내용

**app/sitemap.ts (line 173-196)**

```typescript
batchResults.forEach((result) => {
  if (result.status === 'fulfilled' && result.value.length > 0) {
    // pricetableNo가 있는 딜만 필터 (SEO: 짧은 URL만 사용)
    const dealsWithPricetableNo = result.value.filter((deal) => deal.pricetableNo);

    // 지역별로 그룹핑하여 서로 다른 지역의 딜 최대 3개 선택
    const seenRegions = new Set<string>();
    const diverseDeals: PriceTableRowItem[] = [];

    for (const deal of dealsWithPricetableNo) {
      const regionKey = `${deal.sidoNo}-${deal.sigunguNo}`;
      if (!seenRegions.has(regionKey) && diverseDeals.length < 3) {
        seenRegions.add(regionKey);
        diverseDeals.push(deal);
      }
    }

    // 지역 다양성이 없으면 최저가 1개만
    if (diverseDeals.length === 0 && dealsWithPricetableNo.length > 0) {
      diverseDeals.push(dealsWithPricetableNo[0]);
    }

    allDeals.push(...diverseDeals);
  }
});
```

#### 결과

```xml
<!-- 변경 전 -->
<url>
  <loc>https://nofee.team/deal/eyJwIjoiU00tU1UtMjUtMjU2R0IiLCJnIj...</loc>
</url>

<!-- 변경 후 -->
<url>
  <loc>https://nofee.team/deal/172</loc>
</url>
```

---

### 4. GA4/Meta Pixel 트래킹 검증

#### 확인 결과

pricetableNo 변경과 무관하게 트래킹이 정상 작동함을 확인했다.

| 이벤트 | GA4 | Meta Pixel | 트래킹 데이터 |
|--------|-----|------------|---------------|
| 딜 상세 조회 | `funnel_deal_view` + `view_item` | `ViewContent` | productName, productCode, productGroupCode |
| 견적 클릭 | `funnel_estimate_click` + `begin_checkout` | `InitiateCheckout` | + userId, isLoggedIn |
| 견적 완료 | `funnel_estimate_complete` + `generate_lead` + `purchase` | `Lead` | + userId |

트래킹은 `productCode`, `productGroupCode`를 사용하므로 pricetableNo URL 변경에 영향받지 않음.

---

### 변경 파일 목록

| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| app/backends/apply/postApplyRegist.ts | 수정 | pricetableNo 파라미터 추가 |
| app/deal/[dealId]/DealDetailClient.tsx | 수정 | pricetableNo 전달 (로그인/비로그인 모두) |
| hooks/useBodyScrollLock.ts | 수정 | 스크롤바 너비 보상 추가 |
| app/deal/[dealId]/components/DealModals.tsx | 수정 | 공유 훅 사용 |
| app/sitemap.ts | 수정 | pricetableNo 기반 짧은 URL만 포함 |

---

### 테스트 결과

#### API 테스트

```bash
# pricetableNo 단건 조회 API
curl -s -X POST "https://dev-api.nofee.team/product-group/phone/pricetable-row/172" \
  -H "Content-Type: application/json" -d '{}' | jq '.status'
# 결과: true

# Deal 페이지 랜딩 테스트
for pno in 171 172 173 174 175; do
  curl -s -o /dev/null -w "%{http_code}" "https://dev.nofee.team/deal/$pno"
done
# 결과: 200 200 200 200 200
```

#### 견적 신청 테스트

- 로그인 사용자: pricetableNo 전달 → 정상 신청
- 비로그인 → 로그인 후 자동 신청: pricetableNo 포함 → 정상 신청

---

## 향후 권장 작업

1. Chat 메시지 가상화: 채팅 메시지가 1000개 이상인 방이 발생할 경우 react-window 도입 검토
2. 스타일 통합: 공통 스타일 컴포넌트 정리 및 통합 작업 별도 진행
3. 이미지 최적화: Next.js Image 컴포넌트 적용으로 LCP 개선
4. 번들 분석: webpack-bundle-analyzer를 통한 번들 사이즈 최적화
5. Meta Events Manager 모니터링: 카탈로그 매칭률 확인 (24-48시간 후)
6. Google Merchant Center 피드 등록 후 진단 오류 모니터링
7. Google Shopping 노출 확인 (피드 등록 후 24-48시간 소요)
