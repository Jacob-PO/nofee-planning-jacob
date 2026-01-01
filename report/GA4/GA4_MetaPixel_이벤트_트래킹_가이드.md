# GA4 및 Meta Pixel 이벤트 트래킹 가이드

## 개요

본 문서는 nofee-front 프로젝트의 GA4 및 Meta Pixel 이벤트 트래킹 구현을 위한 기술 가이드입니다.
공식 문서 및 2025년 최신 권장사항을 기반으로 작성되었습니다.

---

## 1. GA4 (Google Analytics 4) 이벤트

### 1.1 이벤트 유형

GA4는 4가지 이벤트 유형을 제공합니다:

| 유형 | 설명 | 구현 필요 |
|------|------|-----------|
| Automatically Collected | 페이지뷰, 세션 시작 등 자동 수집 | 불필요 |
| Enhanced Measurement | 스크롤, 외부 링크 클릭 등 | 설정만 필요 |
| Recommended Events | Google 권장 이벤트 (view_item, purchase 등) | 수동 구현 |
| Custom Events | 비즈니스별 커스텀 이벤트 | 수동 구현 |

### 1.2 권장 이벤트 (Recommended Events)

Google 공식 문서에서 권장하는 이벤트입니다. 권장 이벤트를 사용하면:
- 향후 GA4 기능 업데이트 자동 호환
- 표준 리포트 활용 가능
- Google Ads 연동 최적화

#### 1.2.1 E-Commerce 이벤트

| 이벤트명 | 용도 | 필수 파라미터 |
|----------|------|---------------|
| `view_item` | 상품 상세 조회 | currency, value, items[] |
| `add_to_cart` | 장바구니 추가 | currency, value, items[] |
| `begin_checkout` | 결제 시작 | currency, value, items[] |
| `add_payment_info` | 결제 정보 입력 | currency, value, items[] |
| `purchase` | 구매 완료 | currency, value, transaction_id, items[] |

#### 1.2.2 Lead Generation 이벤트

| 이벤트명 | 용도 | 필수 파라미터 |
|----------|------|---------------|
| `generate_lead` | 리드 생성 (견적 완료) | currency, value |
| `sign_up` | 회원가입 완료 | - (method 권장) |
| `login` | 로그인 완료 | - (method 권장) |

#### 1.2.3 items 배열 구조

```typescript
interface GA4Item {
  item_id: string;        // 필수 (item_name과 둘 중 하나)
  item_name: string;      // 필수 (item_id와 둘 중 하나)
  item_brand?: string;    // 브랜드 (예: SKT, KT, LGU+)
  item_category?: string; // 카테고리 (예: 기기변경, 번호이동)
  price?: number;         // 가격
  quantity?: number;      // 수량 (기본값: 1)
  discount?: number;      // 할인액
}
```

### 1.3 구현 코드 예시

```typescript
// GA4 이벤트 전송
export const event = (action: string, params?: Record<string, unknown>) => {
  if (!GA_MEASUREMENT_ID || typeof window === 'undefined') return;
  window.gtag?.('event', action, {
    ...params,
    ...(_currentUserNo && { user_no: _currentUserNo }),
  });
};

// view_item 이벤트 예시
event('view_item', {
  currency: 'KRW',
  value: 50000,
  items: [{
    item_id: 'SM-ZF-7',
    item_name: '갤럭시 Z 폴드7',
    item_brand: 'SKT',
    item_category: '번호이동',
    price: 50000,
    quantity: 1,
  }],
});
```

### 1.4 명명 규칙

- 이벤트명: `snake_case` 사용 (예: `view_item`, `generate_lead`)
- 파라미터명: `snake_case` 사용 (예: `item_id`, `item_name`)
- 통화: ISO 4217 3자리 코드 (예: `KRW`, `USD`)

---

## 2. Meta Pixel 이벤트

### 2.1 표준 이벤트 (Standard Events)

Meta Pixel은 사전 정의된 표준 이벤트를 제공합니다. 표준 이벤트 사용 시:
- Meta 광고 최적화 자동 적용
- 전환 추적 및 리타게팅 가능
- Events Manager에서 자동 인식

#### 2.1.1 핵심 전환 이벤트

| 이벤트명 | 용도 | 코드 예시 |
|----------|------|-----------|
| `PageView` | 페이지 조회 | `fbq('track', 'PageView')` |
| `ViewContent` | 상품/콘텐츠 조회 | `fbq('track', 'ViewContent', {...})` |
| `InitiateCheckout` | 결제/신청 시작 | `fbq('track', 'InitiateCheckout', {...})` |
| `Lead` | 리드 생성 (견적 완료) | `fbq('track', 'Lead', {...})` |
| `Purchase` | 구매 완료 | `fbq('track', 'Purchase', {...})` |
| `CompleteRegistration` | 회원가입 완료 | `fbq('track', 'CompleteRegistration', {...})` |

#### 2.1.2 보조 이벤트

| 이벤트명 | 용도 |
|----------|------|
| `AddToCart` | 장바구니 추가 |
| `AddPaymentInfo` | 결제 정보 입력 |
| `AddToWishlist` | 관심 상품 등록 |
| `Search` | 검색 |
| `Contact` | 연락처 제출 |
| `Subscribe` | 구독 |
| `StartTrial` | 무료 체험 시작 |

### 2.2 이벤트 파라미터

#### 2.2.1 공통 파라미터

| 파라미터 | 설명 | 예시 |
|----------|------|------|
| `content_name` | 상품/콘텐츠명 | "갤럭시 Z 폴드7" |
| `content_ids` | 상품 ID 배열 | ["SM-ZF-7"] |
| `content_category` | 카테고리 | "SKT" |
| `content_type` | 콘텐츠 유형 | "product" |
| `value` | 금액 | 50000 |
| `currency` | 통화 | "KRW" |
| `num_items` | 수량 | 1 |

#### 2.2.2 이벤트별 권장 파라미터

**ViewContent**
```javascript
fbq('track', 'ViewContent', {
  content_name: '갤럭시 Z 폴드7',
  content_ids: ['SM-ZF-7'],
  content_category: 'SKT',
  content_type: 'product',
  value: 50000,
  currency: 'KRW',
});
```

**InitiateCheckout**
```javascript
fbq('track', 'InitiateCheckout', {
  content_name: '갤럭시 Z 폴드7',
  content_ids: ['SM-ZF-7'],
  content_type: 'product',
  value: 50000,
  currency: 'KRW',
  num_items: 1,
});
```

**Lead**
```javascript
fbq('track', 'Lead', {
  content_name: '갤럭시 Z 폴드7',
  content_category: 'SKT',
  content_ids: ['SM-ZF-7'],
  value: 50000,
  currency: 'KRW',
});
```

**CompleteRegistration**
```javascript
fbq('track', 'CompleteRegistration', {
  content_name: 'signup',
  status: true,
});
```

### 2.3 Conversion API (CAPI)

iOS 14.5+ 이후 브라우저 쿠키 제한으로 인해 서버 사이드 트래킹이 중요해졌습니다.

#### 2.3.1 Pixel + CAPI 이중 전송

```typescript
function trackDualEvent(
  eventName: string,
  pixelParams: Record<string, unknown>,
  capiCustomData: Record<string, unknown>,
  eventId: string,
  externalId?: string
) {
  // 1. Pixel (클라이언트)
  trackPixelEvent(eventName, pixelParams, eventId);

  // 2. CAPI (서버)
  trackCapiEvent(eventName, eventId, capiCustomData, externalId);
}
```

#### 2.3.2 Event ID 중복 제거

동일한 `eventId`를 Pixel과 CAPI에 전송하면 Meta가 자동으로 중복을 제거합니다.

```typescript
export function generateEventId(): string {
  return `${Date.now()}_${Math.random().toString(36).substring(2, 11)}`;
}
```

### 2.4 iOS 제한사항 (2025년 기준)

- 클릭 귀속 기간: 7일
- 조회 귀속 기간: 1일
- Pixel당 최대 이벤트 수: 8개
- ATT (App Tracking Transparency) 팝업 필요

---

## 3. nofee 프로젝트 이벤트 매핑

### 3.1 퍼널 단계별 이벤트

| 단계 | 사용자 액션 | GA4 이벤트 | Meta Pixel 이벤트 |
|------|------------|-----------|------------------|
| 1 | 홈 방문 | `view_item_list` | `PageView` |
| 2 | 딜 상세 조회 | `view_item` | `ViewContent` |
| 3 | 견적 신청 클릭 | `begin_checkout` | `InitiateCheckout` |
| 4 | 견적 완료 | `generate_lead`, `purchase` | `Lead` |
| 5 | 회원가입 완료 | `sign_up` | `CompleteRegistration` |
| 6 | 로그인 완료 | `login` | - |

### 3.2 구현 파일 구조

```
lib/analytics/
├── gtag.ts              # GA4 이벤트 함수
├── fbPixel.ts           # Meta Pixel 이벤트 함수
├── faceBookPixel.tsx    # Meta Pixel 초기화 컴포넌트
├── trackConversion.ts   # GA4 + Meta Pixel 통합 트래킹
├── RouteChangeTracker.tsx # SPA 라우트 변경 추적
└── GA4UserInit.tsx      # GA4 사용자 식별 초기화
```

### 3.3 스크립트 로드 타이밍

```
1. HTML 로드
2. GA4 스크립트 로드 (<head> 내, afterInteractive)
   - window.gtag 함수 생성
   - dataLayer 큐 생성
3. Meta Pixel 스크립트 로드 (<body> 내, afterInteractive)
   - window.fbq 함수 생성
4. React Hydration
5. useEffect 실행
   - 이벤트 트래킹 호출
```

**주의:** Meta Pixel이 GA4보다 늦게 로드되므로, 이벤트 호출 시 `fbq` 존재 여부 확인 및 retry 로직 필요.

---

## 4. 구현 체크리스트

### 4.1 GA4

- [ ] 모든 권장 이벤트에 필수 파라미터 포함
- [ ] items 배열에 item_id 또는 item_name 필수
- [ ] currency는 ISO 4217 형식 (KRW)
- [ ] user_id 설정으로 크로스 디바이스 추적
- [ ] UTM 파라미터 세션 스토리지 저장

### 4.2 Meta Pixel

- [ ] 모든 표준 이벤트에 value, currency 포함
- [ ] content_ids 배열 형식으로 전송
- [ ] Pixel + CAPI 이중 전송
- [ ] eventId로 중복 제거
- [ ] fbq 로드 전 호출 시 retry 로직

### 4.3 SPA 대응

- [ ] RouteChangeTracker로 라우트 변경 추적
- [ ] 첫 페이지 로드는 스크립트가 처리 (중복 방지)
- [ ] GA4 pageview + Meta Pixel PageView 동시 전송

---

## 참고 자료

### 공식 문서

- [GA4 Recommended Events - Google Developers](https://developers.google.com/analytics/devguides/collection/ga4/reference/events)
- [GA4 Event Reference - Google Analytics Help](https://support.google.com/analytics/answer/9267735?hl=en)
- [Meta Pixel Standard Events - Meta Business Help](https://business.facebook.com/business/help/402791146561655)

### 참고 블로그

- [GA4 Recommended Events Guide 2025 - Analytify](https://analytify.io/ga4-recommended-events/)
- [Meta Pixel Events Guide - Madgicx](https://madgicx.com/blog/facebook-pixel-events)
- [GA4 Event Tracking Checklist 2025 - MeasureSchool](https://measureschool.com/google-analytics-4-event-tracking/)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-01-01 | 초안 작성 | Claude Code |
