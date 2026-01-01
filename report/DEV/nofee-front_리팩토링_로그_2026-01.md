# nofee-front 코드 안정성 개선 리팩토링 로그

## 개요

- 작업 기간: 2026년 1월
- 브랜치: sunho.kim
- 커밋: ade19d2
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

## P1 Important 수정사항

### 4. DealList.tsx - 필터 체인 최적화

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

### 5. LowestDealSection.tsx - 필터 체인 최적화

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

총계: 45 insertions, 49 deletions

---

## Git 커밋 정보

- 커밋 해시: ade19d2
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

### 5. LowestDealSection.tsx - 필터 체인 최적화
- 3단계 필터 체인 -> 단일 패스 필터로 최적화
- 동일한 패턴 적용으로 일관성 확보
- 성능 개선: 3회 배열 순회 -> 1회 순회

## 테스트 결과
- 전체 빌드 통과 (56개 페이지 컴파일 성공)
- TypeScript 타입 검사 통과
```

---

## GitHub Issue 연동

Issue: #84
코멘트 URL: https://github.com/nofee-workspace/nofee-front/issues/84#issuecomment-3703248603

---

## 향후 권장 작업

1. Chat 메시지 가상화: 채팅 메시지가 1000개 이상인 방이 발생할 경우 react-window 도입 검토
2. 스타일 통합: 공통 스타일 컴포넌트 정리 및 통합 작업 별도 진행
3. 이미지 최적화: Next.js Image 컴포넌트 적용으로 LCP 개선
4. 번들 분석: webpack-bundle-analyzer를 통한 번들 사이즈 최적화
