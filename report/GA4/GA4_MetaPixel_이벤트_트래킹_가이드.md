# GA4 및 Meta Pixel 이벤트 트래킹 가이드

## 개요

본 문서는 nofee-front 프로젝트의 GA4 및 Meta Pixel 이벤트 트래킹 구현을 위한 기술 가이드입니다.
**노피 서비스에 최적화된 구현 내용**을 포함합니다.

---

## 1. 노피 전환 퍼널 구조

```
[홈/상품 목록] → [상품 상세] → [딜 상세] → [견적 신청 클릭] → [견적 완료]
     ↓              ↓            ↓              ↓               ↓
  PageView     ViewContent  ViewContent  InitiateCheckout     Lead
              (product)      (deal)                        (핵심 전환)
```

### 1.1 퍼널 단계별 이벤트 매핑

| 단계 | 사용자 액션 | GA4 이벤트 | Meta Pixel 이벤트 | 트래킹 함수 |
|------|------------|-----------|------------------|-------------|
| 1 | 상품 상세 조회 | `view_item` | `ViewContent` | `ProductPageTracker` |
| 2 | 딜 상세 조회 | `view_item` | `ViewContent` | `trackDealViewConversion()` |
| 3 | 견적 신청 클릭 | `begin_checkout` | `InitiateCheckout` | `trackEstimateClickConversion()` |
| 4 | **견적 완료** | `generate_lead` | `Lead` | `trackEstimateCompleteConversion()` |
| 5 | 회원가입 완료 | `sign_up` | `CompleteRegistration` | `fbTrackCompleteRegistration()` |

---

## 2. Meta Pixel + CAPI 구현 (노피 최적화)

### 2.1 핵심 구현 사항

| 기능 | 설명 | 상태 |
|------|------|------|
| Pixel + CAPI 이중 전송 | 클라이언트 + 서버 동시 전송으로 정확도 향상 | ✅ |
| eventId 중복 제거 | 동일 eventId로 Pixel/CAPI 중복 방지 | ✅ |
| 고급 매칭 (Advanced Matching) | 사용자 데이터로 매칭률 향상 | ✅ |
| 카탈로그 매칭 | productGroupCode로 카탈로그 ID 통일 | ✅ |
| 세션 내 중복 방지 | 동일 상품 중복 이벤트 필터링 | ✅ |
| 재시도 로직 | Pixel/CAPI 실패 시 자동 재시도 | ✅ |

### 2.2 카탈로그 매칭 (content_ids)

**중요:** `content_ids`는 Meta 카탈로그 피드의 ID와 동일해야 광고 최적화가 작동합니다.

```typescript
// ✅ 올바른 구현 (productGroupCode 사용)
content_ids: ['SM-ZF-6']      // 카탈로그 피드 ID와 동일
content_ids: ['IP-16-PRO']

// ❌ 잘못된 구현 (개별 productCode 사용)
content_ids: ['SM-S911N-256-BK']  // 카탈로그에 없는 ID
```

**노피 카탈로그 피드 구조:**
- URL: `https://nofee.team/api/catalog/feed`
- ID 형식: `productGroupCode` (예: SM-ZF-6, IP-16-PRO)
- Pixel content_ids와 카탈로그 ID가 일치해야 매칭됨

### 2.3 고급 매칭 (Advanced Matching)

로그인 사용자의 데이터를 자동으로 Pixel과 CAPI에 전송합니다.

| 필드 | 설명 | 데이터 소스 | 예시 |
|------|------|-------------|------|
| `em` | 이메일 | user.email | test@example.com |
| `ph` | 전화번호 (국제형식) | user.telNo | 821012345678 |
| `fn` | 이름 | user.userNm | 홍길동 |
| `ge` | 성별 | user.gender | m / f |
| `db` | 생년월일 | user.birthday | 19900101 |
| `external_id` | 사용자 고유 ID | user.userNo | 12345 |

**구현 위치:**
- Pixel 고급 매칭: `faceBookPixel.tsx` - `getAdvancedMatchingData()`
- CAPI 고급 매칭: `fbPixel.ts` - `getUserDataForCapi()`

---

## 3. 파일 구조 및 역할

```
lib/analytics/
├── gtag.ts                  # GA4 이벤트 전송 함수
├── fbPixel.ts               # Meta Pixel + CAPI 이벤트 함수
├── faceBookPixel.tsx        # Meta Pixel 초기화 컴포넌트 (고급 매칭)
├── trackConversion.ts       # GA4 + Meta 통합 트래킹 (권장 사용)
├── RouteChangeTracker.tsx   # SPA 라우트 변경 추적
└── GA4UserInit.tsx          # GA4 사용자 식별 초기화

app/api/
├── fb-conversion/route.ts   # CAPI 서버 엔드포인트
└── catalog/feed/route.ts    # Meta 카탈로그 피드
```

### 3.1 통합 트래킹 함수 사용법 (권장)

```typescript
import {
  trackDealViewConversion,
  trackEstimateClickConversion,
  trackEstimateCompleteConversion
} from '@/lib/analytics/trackConversion';

// 딜 상세 조회
trackDealViewConversion({
  productName: '갤럭시 Z 폴드6',
  productCode: 'SM-F956N-512-BK',
  productGroupCode: 'SM-ZF-6',  // ⭐ 카탈로그 매칭용 필수
  carrier: 'SKT',
  monthPrice: 50000,
  storeNo: 123,
});

// 견적 신청 클릭
trackEstimateClickConversion(dealData, {
  isLoggedIn: true,
  ctaType: 'kakao_chat',
});

// 견적 완료 (핵심 전환!)
trackEstimateCompleteConversion(dealData);
```

---

## 4. 이벤트 상세 스펙

### 4.1 ViewContent (상품/딜 조회)

```typescript
// Pixel 파라미터
{
  content_name: '갤럭시 Z 폴드6',
  content_category: 'SKT',
  content_ids: ['SM-ZF-6'],      // productGroupCode
  content_type: 'product',
  value: 50000,
  currency: 'KRW',
}

// CAPI custom_data
{
  content_name: '갤럭시 Z 폴드6',
  content_category: 'SKT',
  content_ids: ['SM-ZF-6'],
  content_type: 'product',
  value: 50000,
  currency: 'KRW',
}
```

### 4.2 InitiateCheckout (견적 신청 클릭)

```typescript
{
  content_name: '갤럭시 Z 폴드6',
  content_ids: ['SM-ZF-6'],
  content_type: 'product',
  value: 50000,
  currency: 'KRW',
  num_items: 1,
  carrier: 'SKT',  // 커스텀 필드
}
```

### 4.3 Lead (견적 완료 - 핵심 전환)

```typescript
{
  content_name: '갤럭시 Z 폴드6',
  content_category: 'SKT',
  content_ids: ['SM-ZF-6'],
  content_type: 'product',
  value: 50000,
  currency: 'KRW',
}
```

---

## 5. CAPI 서버 구현

### 5.1 엔드포인트

```
POST /api/fb-conversion
```

### 5.2 요청 형식

```typescript
interface ConversionRequest {
  eventName: string;           // 'ViewContent', 'Lead' 등
  eventId: string;             // Pixel과 동일한 ID (중복 제거용)
  eventSourceUrl?: string;     // 이벤트 발생 URL
  userData?: {
    fbc?: string;              // FB 클릭 ID 쿠키
    fbp?: string;              // FB 브라우저 ID 쿠키
    externalId?: string;       // 사용자 고유 ID
    // 고급 매칭 필드 (자동 해싱됨)
    em?: string;               // 이메일
    ph?: string;               // 전화번호
    fn?: string;               // 이름
    ge?: string;               // 성별
    db?: string;               // 생년월일
  };
  customData?: {
    contentName?: string;
    contentCategory?: string;
    contentIds?: string[];
    contentType?: string;
    value?: number;
    currency?: string;
  };
}
```

### 5.3 응답 형식

```typescript
// 성공
{ success: true, events_received: 1 }

// 실패
{ success: false, error: 'Invalid access token' }
```

---

## 6. 환경 변수 설정

### 6.1 필수 환경 변수

```bash
# .env
NEXT_PUBLIC_FB_PIXEL_ID=751478554021745
FB_CONVERSION_API_TOKEN=EAAOZApk1SCTQ...

# GA4
NEXT_PUBLIC_GA4_ID=G-3H2KNEQTMR
NEXT_PUBLIC_GTM_ID=GTM-WBLWMQZB
```

### 6.2 Vercel 프로덕션 설정

Vercel 대시보드에서 동일한 환경 변수를 설정해야 합니다.

---

## 7. 테스트 방법

### 7.1 CAPI 직접 테스트

```bash
cd /Users/jacob/Desktop/workspace/nofee/nofee-planning-jacob/report/GA4/test
npx ts-node fb-capi-direct-test.ts
```

### 7.2 브라우저 테스트 (Playwright)

```bash
npx ts-node fb-advanced-matching-test.ts
```

### 7.3 Meta Events Manager 확인

1. https://business.facebook.com/events_manager 접속
2. 해당 Pixel ID 선택
3. "Test Events" 탭에서 실시간 이벤트 확인
4. "Overview"에서 카탈로그 매칭률 확인 (24-48시간 후)

---

## 8. 트러블슈팅

### 8.1 Pixel이 로드되지 않음

**원인:** `NEXT_PUBLIC_FB_PIXEL_ID` 환경 변수 미설정

**해결:**
```bash
# .env에 추가
NEXT_PUBLIC_FB_PIXEL_ID=751478554021745
```

### 8.2 CAPI 요청 실패

**원인:** `FB_CONVERSION_API_TOKEN` 만료 또는 미설정

**해결:**
1. Meta Business Suite에서 새 토큰 발급
2. .env 및 Vercel에 토큰 업데이트

### 8.3 카탈로그 매칭률 낮음

**원인:** content_ids가 카탈로그 피드 ID와 불일치

**해결:**
- content_ids에 `productGroupCode` 사용 확인
- 카탈로그 피드 URL 확인: `https://nofee.team/api/catalog/feed`

### 8.4 고급 매칭 데이터 누락

**원인:** 로그인하지 않은 상태이거나 사용자 데이터 없음

**확인:**
- 브라우저 콘솔에서 `[FB Pixel] Advanced matching updated:` 로그 확인
- sessionStorage의 `auth-storage` 데이터 확인

---

## 9. 성능 최적화

### 9.1 세션 내 중복 방지

```typescript
// fbPixel.ts
const trackedEvents = new Set<string>();

function isEventTracked(eventKey: string): boolean {
  if (trackedEvents.has(eventKey)) return true;
  trackedEvents.add(eventKey);
  return false;
}

// 사용 예
const eventKey = `ViewContent_${params.productGroupCode}`;
if (isEventTracked(eventKey)) return null;  // 중복 스킵
```

### 9.2 Pixel 로드 재시도

```typescript
function trackPixelEvent(eventName, params, eventId, retryCount = 0) {
  if (!window.fbq) {
    if (retryCount < 5) {
      setTimeout(() => {
        trackPixelEvent(eventName, params, eventId, retryCount + 1);
      }, 100);
    }
    return;
  }
  window.fbq('track', eventName, params, { eventID: eventId });
}
```

### 9.3 CAPI 비동기 처리

```typescript
// UI 블로킹 방지를 위해 await 없이 호출
trackCapiEvent(eventName, eventId, capiCustomData, externalId);
// 내부적으로 실패 시 1초 후 1회 재시도
```

---

## 10. GA4 구현 요약

### 10.1 권장 이벤트 사용

| 이벤트 | 용도 | 필수 파라미터 |
|--------|------|---------------|
| `view_item` | 상품/딜 조회 | items[], value, currency |
| `begin_checkout` | 견적 신청 클릭 | items[], value, currency |
| `generate_lead` | 견적 완료 | value, currency |
| `sign_up` | 회원가입 | method |
| `login` | 로그인 | method |

### 10.2 items 배열 구조

```typescript
items: [{
  item_id: 'SM-ZF-6',           // productGroupCode
  item_name: '갤럭시 Z 폴드6',
  item_brand: 'Samsung',
  item_category: 'SKT',
  price: 50000,
  quantity: 1,
}]
```

---

## 11. 체크리스트

### 11.1 구현 완료 항목

- [x] FB Pixel 초기화 (faceBookPixel.tsx)
- [x] CAPI 서버 엔드포인트 (/api/fb-conversion)
- [x] Pixel + CAPI 이중 전송
- [x] eventId 기반 중복 제거
- [x] 고급 매칭 (Advanced Matching)
- [x] productGroupCode 기반 content_ids
- [x] 카탈로그 피드 (/api/catalog/feed)
- [x] 세션 내 중복 이벤트 방지
- [x] 재시도 로직 (Pixel 5회, CAPI 1회)
- [x] 환경 변수 설정 (.env)

### 11.2 프로덕션 배포 전 확인

- [ ] Vercel 환경 변수 설정
- [ ] Meta Events Manager에서 이벤트 수신 확인
- [ ] 카탈로그 매칭률 확인 (24-48시간 후)

---

## 참고 자료

### 공식 문서

- [GA4 Recommended Events](https://developers.google.com/analytics/devguides/collection/ga4/reference/events)
- [Meta Pixel Standard Events](https://developers.facebook.com/docs/meta-pixel/reference)
- [Meta Conversion API](https://developers.facebook.com/docs/marketing-api/conversions-api)
- [Meta Advanced Matching](https://developers.facebook.com/docs/meta-pixel/advanced/advanced-matching)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-01-01 | 초안 작성 | Claude Code |
| 2026-01-01 | 노피 최적화 버전 업데이트 - 고급 매칭, 카탈로그 매칭, 테스트 방법 추가 | Claude Code |
