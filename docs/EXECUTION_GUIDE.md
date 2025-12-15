# NOPI (NoFee) Execution Guide

*철학을 코드로: AI/PM/개발자를 위한 구현 가이드*
v1.1

> 이 문서는 [claude.md](./claude.md)의 철학/비전을 **실제 구현 가능한 형태**로 변환한 실행 가이드입니다.
> 핵심 기호: **σ(스냅샷) · R(가용반경) · T(신뢰 높이) · ΔP(가격 투명성) · τ(마찰비용) · MR(후회 가드)**

---

## 비즈니스 1원칙 (First Principle)

> **고객 잉여 > 0** + **플랫폼 잉여 > 0** + **반복 가능** ⇒ **지속가능**

---

## 0. 정체성 요약 (Identity Summary)

### 0.1 미션

> "**집 근처에서 후회 없이 최선의 휴대폰 계약을 선택**하게 하고, 그 과정에서 **정직한 동네 매장**이 안정적으로 벌게 만든다."

### 0.2 정식 표기 (Canonical Formula)

```
Nopi(c, R) = argmin_{d ∈ Deals(R)} E[Regret(c, d)]
subject to:
  Store(d) ∈ ReachableZone(R)                // Locality
  ∀ τ: ∃ σ, ℓ; τ derives from σ               // Snapshot invariant
  Ranking(d) ∝ f(Price(d), Trust(Store(d)))   // Honesty attractor
  Trust ↑ if honest; Trust ↓ if dishonest
```

### 0.3 한 줄 정의

**노피는 지역(R) 안에서 가격이 단일수가 될 수 없다는 현실을 구조화하고, 스냅샷(σ)과 신뢰(T)로 그 구조를 검증 가능하게 만드는 '후회 최소화(ΔRegret) 프로토콜'**이다.

---

## 1. 불변식 (Invariants) — 절대 위반 금지

모든 기능/정책/코드는 아래 3가지를 반드시 보존해야 한다:

| # | 불변식 | 설명 | 위반 시 |
|---|--------|------|---------|
| 1 | **Locality** | 거래는 고객 가용반경 R 내에서 의미 있다 | 즉시 중단 |
| 2 | **Snapshot(σ)** | 모든 리드는 계약 스냅샷에 연결되어야 한다 | 즉시 중단 |
| 3 | **Honesty Attractor** | 장기적으로 정직 매장이 더 많은 이익을 얻어야 한다 | 즉시 중단 |

### 1.1 물리학 은유 (Physics Metaphor)

| 물리 법칙 | 노피 적용 |
|-----------|-----------|
| **관성 (뉴턴 1법칙)** | 고객은 기존 루틴(지인/원정)에 머문다 → 노피는 **외력**(정량화·지역축·신뢰) |
| **엔트로피** | 가격/조건의 혼돈 → **σ/ΔP**로 질서 부여 |
| **보존량** | 신뢰(T)는 거래 피드백을 통해 **축적/소멸**되는 양 |

### 1.2 단일 가격 부재 ("Price Non-Existence")

휴대폰 계약의 비용은 **다차원 벡터**: 기기/요금/할인/조건/기간/위약/카드/결합 등.
고객이 체감하는 "최저가"는 환상일 수 있다. 따라서 **가격의 구조화와 검증**이 필수.

---

## 2. 핵심 분자 (Core Molecules)

### 2.1 Mσ — Snapshot (약속의 핵)

**정의**: 견적 순간의 계약 상태를 고정

| 포함 항목 | 설명 |
|-----------|------|
| 기기 | modelId, name, storage, color |
| 통신 | carrier, joinType (신규/기변/번이) |
| 요금 | monthlyPlan, discountType |
| 가격 | msrp, carrierSubsidy, shopSubsidy, installmentPrice, effectiveDevicePrice |
| 조건 | vasRequired, cardRequired, bundleRequired, minMonths, notes |
| 메타 | storeId, customerRegion, createdAt, validUntil |

**KPI 임계치**:
- 가격 일치율 ≥ 95%
- 조건 일치율 ≥ 90%

**규정**: σ 없이 리드/후기 생성 금지 (시스템 레벨 강제)

---

### 2.2 ML — Locality (가용반경 R)

**정의**: 기본은 "동네(시·군·구)"에서 시작, 사용자가 명시적으로 반경 확대 가능

| 반경 옵션 | 설명 |
|-----------|------|
| 5km | 도보/자전거 거리 |
| 10km | 근린 생활권 |
| 20km | 도시 내 이동 |
| 40km | 광역 이동 |

**KPI**:
- 지역 내 전환율 ↑
- 지역 외 리드 ≤ 10%
- 방문거리 중위값 모니터링

---

### 2.3 MT — Trust Score (신뢰 높이, 1~100층)

**계산 공식**:
```
T = 0.6 × (σ 일치 신호) + 0.4 × (후기/클레임)
```

**업데이트 (EMA)**:
```
Δ = 0.5×priceMatch + 0.3×conditionMatch + 0.2×reviewScore - 0.5
T_next = α×T + (1-α)×Δ   // α ≈ 0.95
```

**UI 표현**: "OO 매장 **신뢰 높이 51층**" (내부 0~1 → UI 1~100층 맵핑)

**랭킹 가중치**: 최소 0.35 고정 (가격만으로 상단 불가)

---

### 2.4 MΔP — Price Transparency (가격 투명성)

**표기 원칙**:

| 우선순위 | 항목 | 필수 여부 |
|----------|------|-----------|
| 1 | 월 납부 | 필수 (굵게) |
| 2 | 24개월 총비용 | 필수 |
| 3 | 평균단가 대비 편차 | 권장 |
| 4 | 최근 4주 변동 | 권장 |

**금지 사항**:
- 조건 숨긴 최저가
- "매장 문의"식 불투명 가격
- 불가능 조건의 미끼 가격

---

### 2.5 Mτ — Friction (마찰비용)

**계산**:
```
Ct = 거리(km) × 교통/연비 단가        // 이동비용
Ch = 이동시간(h) × 시간가치(Vh)       // 시간비용
Cr = P(불일치)×λ1 + P(추가조건)×λ2 + 거리기반AS불편×λ3  // 리스크
```

**UI 표기**: 카드 미니칩/툴팁으로 "예상 이동/시간/리스크 비용"

---

### 2.6 MR — Regret Guard (후회 가드)

**정책**: 원정 제안은 아래 조건 충족 시에만 노출
```
절약액 ≥ (Ct + Ch + Cr) × (1 + β)   // β ≈ 0.3~0.5
```

**목적**: "멀리 갔다가 후회" 방지 (손실회피 보정)

---

## 3. 비즈니스 분자 전체 (Business Molecules)

> 12개의 운영 분자 — 각각 "입력→핵심 연산→출력→KPI→가드레일"로 정의

### M1. Locality 분자 (탐색의 1축)

| 항목 | 내용 |
|------|------|
| **입력** | 고객 지역(시/도·구/군), 이동가능 거리 |
| **핵심 연산** | 지역 격자 필터링(ReachableZone) |
| **출력** | 지역 한정 Candidate 딜 집합 |
| **KPI** | 지역 지정률 ≥95%, 지역 밖 리드 ≤5%, 평균 방문거리↓ |
| **가드레일** | Locality Invariant—지역 없으면 견적 버튼 비활성 |

### M2. Deal Vector 정규화 분자 (가격 스칼라화)

| 항목 | 내용 |
|------|------|
| **입력** | (통신사×가입유형×할인×요금제×조건) 벡터 |
| **핵심 연산** | `월 납부액`·`24개월 총비용`·`할부원금` 동시 산출 |
| **출력** | 비교 가능한 스칼라 지표(표준 포맷) |
| **KPI** | 가격 이해도(설문), 견적 전환율, 분쟁율 ≤2% |
| **가드레일** | 총 24개월 비용 **항상 병기**(토글 불가) |

### M3. Snapshot(σ) 분자 (약속의 고정점)

| 항목 | 내용 |
|------|------|
| **입력** | 선택된 표준 딜 + 시간/매장/고객 지역 |
| **핵심 연산** | 계약 스냅샷 생성·서명(해시) |
| **출력** | σ ID, 검증 가능한 참조 상태 |
| **KPI** | 가격일치율 ≥95%, 조건일치율 ≥90% |
| **가드레일** | Snapshot Invariant—σ 없이 리드 생성 금지 |

### M4. Lead OS 분자 (파이프라인 운영)

| 항목 | 내용 |
|------|------|
| **입력** | σ ID, 고객 연락채널 |
| **핵심 연산** | 상태머신(new→contacted→visited→activated) |
| **출력** | 리드 히스토리, 전환 이벤트 |
| **KPI** | 견적→방문→개통 전환율, 리드 체류시간 |
| **가드레일** | 상태 누락 금지, 히스토리 불변(append-only) |

### M5. TrustScore 분자 (정직의 계량화)

| 항목 | 내용 |
|------|------|
| **입력** | 가격/조건 일치율, 후기 점수, 클레임 |
| **핵심 연산(EMA)** | `0.6×일치율 + 0.4×(후기/클레임)` |
| **출력** | 0~1 점수 → UI "신뢰 높이(층:1~100)" |
| **KPI** | 클레임율↓, 재구매/추천률↑, 점수 안정성 |
| **가드레일** | Honesty Conservation—리뷰만으로 점수 상승 불가 |

### M6. Ranking 분자 (노출 분배)

| 항목 | 내용 |
|------|------|
| **입력** | priceScore, trustScore, rating, recency |
| **핵심 연산** | `0.40·가격 + 0.35·신뢰 + 0.15·평점 + 0.10·신규성` |
| **출력** | 지역 내 정렬 리스트(딜/매장 카드) |
| **KPI** | 클릭률·견적률·가격일치율 동시 개선 |
| **가드레일** | TrustScore 가중치 하한(≥0.35) 유지 |

### M7. Review & Evidence 분자 (증거 레이어)

| 항목 | 내용 |
|------|------|
| **입력** | 개통 후 한줄평·별점·증빙(영수증/σ대조) |
| **핵심 연산** | 텍스트 신뢰 키워드 추출("시세표 그대로" 등), 스팸필터 |
| **출력** | 공개 후기, 신뢰 높이 보완지표 |
| **KPI** | 유효후기율, 조작탐지율, 리뷰-일치율 상관 |
| **가드레일** | σ 연동 없는 후기 가중치 축소 |

### M8. Store ROI 분자 (매장 경제성)

| 항목 | 내용 |
|------|------|
| **입력** | 리드 수, 전환율, 평균마진, 구독료 |
| **핵심 연산** | `ROI = (리드×전환×마진 - 99,000) / 99,000` |
| **출력** | ROI 대시보드·경고 |
| **KPI** | ROI_store ≥2 유지, 구독 유지율 ≥90% |
| **가드레일** | 지역·모델 편차 민감도 모니터링(경보) |

### M9. Hotdeal/Signal 분자 (수요-공급 신호)

| 항목 | 내용 |
|------|------|
| **입력** | 신청수 급증, 단가 갱신, 재고·정책 변화 |
| **핵심 연산** | 이상치 탐지 → "핫딜" 태깅(기본 지역 ON) |
| **출력** | 홈 보조 탭·알림(지역 잠금) |
| **KPI** | 견적률↑, 지역 밖 리드 비중 ≤5% |
| **가드레일** | Locality 우선—전국 최저가 사고 유입 차단 |

### M10. Language Simplifier 분자 (용어 번역기)

| 항목 | 내용 |
|------|------|
| **입력** | 통신 용어(번호이동/기변/선택약정…) |
| **핵심 연산** | 고객언어 변환 규칙("번호이동=통신사 바뀜") |
| **출력** | 이해도 높은 UI 텍스트·FAQ |
| **KPI** | 이탈률↓, CS 문의↓, 설문 이해도↑ |
| **가드레일** | 정합성 테스트(전 계약 흐름 동일 의미 보장) |

### M11. Governance & Sanction 분자 (질서 유지)

| 항목 | 내용 |
|------|------|
| **입력** | TrustScore, 불일치 카운트, 클레임 |
| **핵심 연산** | 경고→노출감산→하단고정→퇴출(자동 워크플로) |
| **출력** | 제재 로그·가시화 |
| **KPI** | 불일치 재발률↓, 시스템 신뢰↑ |
| **가드레일** | 기준 공개·이의절차 존재, 자동+인간 검토 병행 |

### M12. Metrics & Causality 분자 (인과 모니터링)

| 항목 | 내용 |
|------|------|
| **입력** | 제품 이벤트·전환·후기·클레임·거리 |
| **핵심 연산** | 인과 그래프(가격일치→Trust→랭킹→견적), 경계치 감시 |
| **출력** | ΔRegret, ROI_store, 일치율, 리드품질 |
| **KPI** | ΔRegret>0, ROI_store≥2, 일치율≥95% |
| **가드레일** | 지표 충돌 시 보존식 우선(Locality/σ/Honesty) |

---

## 4. 알고리즘 (Algorithms)

### 4.1 TEC (총유효비용)

```typescript
function calcTEC(deal: Deal, distanceKm: number, hours: number, params: {
  fuelCostPerKm: number;
  hourValue: number;
  pMismatch: number;
  pExtraCond: number;
  asPenalty: number;
  lambda1: number;
  lambda2: number;
  lambda3: number;
}) {
  const deviceCost24M = deal.priceVector.effectiveDevicePrice + deal.monthlyPlan * 24;
  const Ct = distanceKm * params.fuelCostPerKm;
  const Ch = hours * params.hourValue;
  const Cr = params.pMismatch * params.lambda1
           + params.pExtraCond * params.lambda2
           + params.asPenalty * params.lambda3;
  return deviceCost24M + Ct + Ch + Cr;
}
```

### 4.2 랭킹 함수 (Rank)

```
Rank = 0.40 × PriceScore(ΔP)
     + 0.35 × TrustScore(T)
     + 0.10 × Recency
     + 0.15 × LocalFit(R, 거리, AS접근성)
```

```typescript
function rankScore(deal: Deal, store: Store, ctx: RankContext): number {
  const priceScore  = normalizePrice(deal);
  const trustScore  = store.trustScore;
  const recency     = normalizeRecency(deal.createdAt, ctx.now);
  const localFit    = normalizeLocalFit(ctx.distanceKm, ctx.asAccess);

  let score = 0.40 * priceScore
            + 0.35 * trustScore
            + 0.10 * recency
            + 0.15 * localFit;

  // MR: 원정 대안은 임계 미달 시 숨김
  if (ctx.isOutOfRadius) {
    const saving = ctx.baseTEC - calcTEC(deal, ctx.distanceKm, ctx.hours, ctx.params);
    if (saving < (ctx.Ct + ctx.Ch + ctx.Cr) * (1 + ctx.beta)) {
      score = -Infinity;
    }
  }
  return score;
}
```

### 4.3 Trust 업데이트 (EMA)

```typescript
function updateTrust(store: Store, tx: Transaction, alpha = 0.95, epsilon = 1000) {
  const priceMatch = Math.abs(tx.actualPrice.installmentPrice - tx.snapshotPrice) < epsilon ? 1 : 0;
  const condMatch  = tx.conditionMatched ? 1 : 0;
  const review     = tx.review ? tx.review.rating / 5 : 0.5;

  const delta = 0.5 * priceMatch + 0.3 * condMatch + 0.2 * review - 0.5;
  store.trustScore = alpha * store.trustScore + (1 - alpha) * delta;
}
```

---

## 4. 데이터 모델 (Data Model)

```typescript
// === 기본 타입 ===
type Carrier = 'SKT' | 'KT' | 'LGU+';
type JoinType = '신규' | '기변' | '번호이동';
type DiscountType = '공시지원' | '선택약정';
type LeadStatus = 'new' | 'contacted' | 'visited' | 'activated' | 'cancelled';

// === Deal (딜/시세) ===
export interface Deal {
  id: string;
  device: {
    modelId: string;
    name: string;
    storage: string;
    color?: string;
  };
  carrier: Carrier;
  joinType: JoinType;
  discountType: DiscountType;
  monthlyPlan: number;
  priceVector: {
    msrp: number;                    // 출고가
    carrierSubsidy: number;          // 공시지원금
    shopSubsidy: number;             // 매장추가지원
    installmentPrice: number;        // 할부원금
    effectiveDevicePrice: number;    // 실질기기값
  };
  conditions: {
    vasRequired: string[];           // 부가서비스 필수
    cardRequired?: string;           // 카드 필수
    bundleRequired?: string;         // 결합 필수
    minMonths: number;               // 최소 유지기간
    notes?: string;                  // 기타 조건
  };
  storeId: string;
  region: { sido: string; sigungu: string };
  createdAt: string;                 // ISO
  validUntil?: string;               // ISO
  isActive: boolean;
}

// === ContractSnapshot (σ) ===
export interface ContractSnapshot {
  id: string;
  dealId: string;
  device: Deal['device'];
  carrier: Deal['carrier'];
  joinType: Deal['joinType'];
  discountType: Deal['discountType'];
  monthlyPlan: number;
  installmentPrice: number;
  effectiveDevicePrice: number;
  conditions: Deal['conditions'];
  store: {
    id: string;
    name: string;
    region: Deal['region'];
  };
  customerRegion: { sido: string; sigungu: string };
  createdAt: string;
  validUntil?: string;
}

// === Lead (리드) ===
export interface Lead {
  id: string;
  snapshotId: string;                // σ 필수 연결
  customerId?: string;
  storeId: string;
  contactChannel: 'kakao' | 'phone' | 'etc';
  customerNote?: string;
  status: LeadStatus;
  createdAt: string;
  updatedAt: string;
}

// === Transaction (τ) ===
export interface Transaction {
  id: string;
  leadId: string;
  storeId: string;
  customerId?: string;
  actualPrice: {
    installmentPrice: number;
    effectiveDevicePrice: number;
    monthlyTotal: number;
  };
  actualConditions: Deal['conditions'];
  snapshotId: string;                // σ 참조
  priceDeviation: number;            // actual - snapshot
  conditionMatched: boolean;
  review?: {
    rating: number;
    comment: string;
    createdAt: string;
  };
  createdAt: string;
}

// === Store (매장) ===
export interface Store {
  id: string;
  name: string;
  region: { sido: string; sigungu: string };
  address: string;
  logoUrl?: string;
  trustScore: number;                // 0~1 (UI: 1~100층)
  avgRating: number;
  reviewCount: number;
  visitCount: number;
  subscriptionActive: boolean;
  createdAt: string;
}
```

---

## 5. 정책 (Policies)

### 5.1 온보딩 정책

| 단계 | 검증 항목 |
|------|-----------|
| 1 | 사업자등록증 확인 |
| 2 | 통신판매업 확인 |
| 3 | 오프라인 위치 검증 |
| 4 | 초기 T = 0.5 부여 |

### 5.2 제재 정책

| 조건 | 제재 |
|------|------|
| 불일치 2회 | 경고 |
| 불일치 5회 또는 T < 0.3 | 노출 -50% |
| T < 0.2 | 하단 고정 |
| T < 0.1 또는 중대 클레임 | 퇴출 |

### 5.3 Locality 운영

- **기본**: R = 근린 (시/군/구)
- **확대**: 사용자 명시적 선택 필요
- **원정 대안**: MR 임계 통과 시에만 노출

---

## 6. UX/제품 매핑 (Product Surfaces)

### 6.1 고객 인터페이스

| 화면 | 핵심 기능 |
|------|-----------|
| **홈 (동네성지)** | 반경 선택 슬라이더(5/10/20/40km), 인기 기종 카드 |
| **딜 리스트/카드** | 월 납부(굵게), 24M 총비용, 평균단가 편차, 신뢰 높이 n층, 마찰비용 미니칩 |
| **기기 상세** | "우리 동네에서 더 싸게?" → 지역 기반 후보 랭킹 |
| **견적 모달** | σ 생성 UI → 카카오 견적 연결 |
| **대안 섹션** | MR 조건 충족 시에만 원정 옵션 노출 |

### 6.2 매장 인터페이스

| 화면 | 핵심 기능 |
|------|-----------|
| **대시보드** | 리드 현황, TrustScore, ROI 지표 |
| **σ 관리** | 내가 한 약속 기록 조회 |
| **τ 히스토리** | 실제 거래 결과 및 일치율 |

---

## 7. API 스케치 (분자별 연결 규격)

```
# M2: Deal Vector 정규화
POST /deals:normalize  { vector } → { monthly, total24, principal }

# M3: Snapshot(σ) 생성
POST /snapshots        { dealId, customerRegion } → { snapshotId }

# M4: Lead OS
POST /leads            { snapshotId, channel } → { leadId }
GET  /leads/{leadId}   → { lead, history }

# M5: Trust 업데이트
POST /trust:update     { txId } → { trustScore }
GET  /stores/{storeId}/trust → { score, history }

# M6: Ranking
GET  /rank:list        { region, filters } → [rankedDeals]

# M7: Review & Evidence
POST /reviews:submit   { snapshotId?, text, rating, evidence? }

# M8: Store ROI
GET  /stores/{storeId}/roi?period=30d → { roi, breakdown }

# M9: Hotdeal Signal
GET  /signals:hotdeal  { region } → [tags]

# 기본 CRUD
GET  /deals?region=sido,sigungu&radiusKm=R&device=modelId&plan=...
GET  /deals/{dealId}
POST /transactions     { leadId, actualPrice, actualConditions }
GET  /stores?region=...&sort=trust
```

---

## 8. KPI & 임계치

| 지표 | 목표 |
|------|------|
| ΔRegret_avg | > 0 (설문/시뮬) |
| 가격 일치율 | ≥ 95% |
| 조건 일치율 | ≥ 90% |
| 지역 내 전환율 | ↑ |
| 지역 밖 리드 | ≤ 10% |
| TrustScore 평균 | ≥ 0.7 |
| 클레임율 | < 2% |
| 원정 대안 노출→전환 | ≥ 20% |
| Store ROI | ≥ 2.0 |

**Store ROI 공식**:
```
ROI = (리드 × 전환율 × 마진 - 99,000) / 99,000
```

---

## 9. 운영 원칙 (비즈니스 1원칙과의 결합)

| 원칙 | 관련 분자 | 설명 |
|------|-----------|------|
| **고객 잉여 극대화** | M1·M2·M3 | 엔트로피↓ → 의사결정 시간↓, 후회↓ |
| **매장 잉여 확보** | M4·M6·M8 | 리드 품질↑, 정직 우대 → ROI_store≥2 유지 |
| **플랫폼 잉여 지속** | M5·M7·M11 | 질서 유지 비용≤구독 LTV, 신뢰 보존 |
| **보존식 우선순위** | 전체 | Locality > Snapshot > Honesty — 위반 기능은 즉시 중단 |

---

## 10. 실패 모드 & 즉시 대응 (Failure Modes)

| 분자 | 실패 모드 | 즉시 대응 |
|------|-----------|-----------|
| **M1** | Locality 붕괴 (핫딜 전국화) | 지역 잠금 강화, 기본 ON 유지 |
| **M2** | 가격 착시 (월 납부만 강조) | 총24개월 비용 **강제 병기** |
| **M3/M4** | σ 누락 → 분쟁↑ | σ 없이 리드 생성 시 **400 에러** |
| **M5/M7** | 리뷰 오염 (게이밍) | σ-일치율 가중치 상향, 스팸탐지 강화 |
| **M6** | 정직 역전 (가격만 높은 가중치) | Trust 가중 하한선 **≥0.35 고정** |
| **M8** | ROI 급락 | 지역·모델별 경보, 운영 개입 |
| **M9** | 전국 최저가 유입 | Locality 우선 강제, 원정 MR 임계 적용 |

---

## 11. 운영 체크리스트 (Decision Gate)

### Level 1 — 불변식 위반 여부 (즉시 중단)

- [ ] Locality를 약화/우회하는가?
- [ ] σ 없이 리드/후기/랭킹을 만드는가?
- [ ] 정직보다 속임에 유리하게 만드는가?

### Level 2 — 가치 기여

- [ ] 고객 가치(U1~U5) 중 1개 이상 확실히 감소하는가?
- [ ] 파이프라인 전환율에 기여하는가?
- [ ] 거래→후기→T→랭킹 피드백 강화하는가?

### Level 3 — 사업성

- [ ] ROI_store ≥ 2.0 유지 가능한가?
- [ ] CAC 통제 가능한가?

---

## 12. 용어 정의 (Glossary)

| 기호 | 명칭 | 정의 |
|------|------|------|
| **σ** | 스냅샷 | 견적 시점 계약 상태의 고정값 |
| **R** | 가용반경 | 고객이 선택한 물리적 탐색 반경 |
| **T** | 신뢰 높이 | 1~100층 (내부 0~1), 정직 우위 보존량 |
| **ΔP** | 가격 투명성 | 월 납부 + 24M 총비용 + 평균단가 편차 |
| **τ** | 마찰비용 | 이동(Ct) + 시간(Ch) + 리스크(Cr) |
| **MR** | 후회 가드 | 원정 제안 임계: 절약액 ≥ (Ct+Ch+Cr)×(1+β) |
| **TEC** | 총유효비용 | DeviceCost(24M) + Ct + Ch + Cr |

---

## 13. MVP 범위 (Minimum Viable Product)

1. **σ 강제**: 스냅샷 없는 리드/후기/랭킹 불가
2. **R 기본 홈**: 지역/반경 슬라이더 + 지역 내 랭킹
3. **T 노출**: "신뢰 높이 n층" + 근거(가격/조건 일치율)
4. **ΔP 표기**: 월 납부(굵게) + 24M 총비용(필수) + 평균 대비 편차
5. **MR 대안**: 임계 통과 시에만 원정 섹션 노출
6. **후기 수집**: 간단 별점 + 한줄평 (σ 기준 일치 여부 체크박스)

---

## 14. 구현 우선순위 (Dev/PM)

| 우선순위 | 항목 | 설명 |
|----------|------|------|
| P0 | 도메인 레벨 검증 | Locality/σ 불변식 강제 |
| P0 | 스키마 버저닝 | 과거 σ 해석 가능하도록 |
| P1 | 신뢰 가시화 | 일치율/클레임/리뷰 로그 대시보드 |
| P1 | 탐색 성능 | 지역/반경 필터 인덱싱, 랭킹 캐시 |
| P2 | 변동 감지 | 가격/조건 변경 알림 |

---

## 15. 브랜딩 문장 (Internal/External)

> **"어디서나 살 수 있어도, 집 근처가 보통 더 합리적입니다.**
> **정말 '원정이 이득'이면, 노피가 총비용까지 계산해 가치 있는 원정만 보여드립니다."**

---

## 16. 문서 관계

```
┌─────────────────────────────────────────────────────┐
│                    claude.md                         │
│              (철학/비전/본질 정의)                    │
│                                                      │
│  "노피는 왜 존재하는가?"                              │
│  "정직이 이득인 시장을 만드는 시스템"                  │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│               EXECUTION_GUIDE.md                     │
│              (실행 가이드/구현 명세)                   │
│                                                      │
│  "어떻게 구현하는가?"                                 │
│  수식, 알고리즘, 스키마, 정책, KPI                    │
└─────────────────────────────────────────────────────┘
```

---

## 17. 한 페이지 요약 (One-Page Summary)

> **노피는** 지역(R) 안에서 **총유효비용(TEC)**을 최소화하도록
> 가격을 구조화(ΔP)하고, **스냅샷(σ)**으로 약속을 고정하며,
> 거래 피드백으로 **신뢰(T)**를 축적하여 정직한 매장이 우위에 서게 만드는
> **후회 최소화 프로토콜**이다.
> 원정은 **MR 임계**를 만족할 때만 권한다.

### 핵심 분자 연결도

```
┌─────────────────────────────────────────────────────────────────┐
│                        고객 여정                                 │
│  [지역설정] → [딜탐색] → [견적생성] → [상담] → [개통] → [후기]    │
│      M1         M2/M6       M3        M4       τ        M7       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        신뢰 피드백 루프                          │
│  σ(약속) → τ(실제) → Deviation → TrustScore → Ranking → σ...   │
│    M3                           M5           M6                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        매장 경제성                               │
│  리드품질 → 전환율 → 마진 → ROI ≥ 2.0 → 구독 유지               │
│    M4                      M8                                    │
└─────────────────────────────────────────────────────────────────┘
```

### 불변식 체크 (모든 기능 적용 전)

1. **Locality 유지?** — 지역 없이 견적 불가
2. **σ 연결?** — 스냅샷 없이 리드/후기 불가
3. **정직 우위?** — Trust 가중치 ≥ 0.35 유지

---

*Last updated: 2025-12-08*
