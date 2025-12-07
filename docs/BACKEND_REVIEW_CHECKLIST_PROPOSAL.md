# 리뷰 체크리스트 필드 추가 제안서

> 작성일: 2025-12-07
> 목적: claude.md 철학(σ-τ 비교 시스템) 완성을 위한 백엔드 API 개선

---

## 1. 배경

### claude.md 핵심 철학
```
σ (약속/Promise) = 시세표에 명시된 가격/조건
τ (실제/Reality) = 거래 후 실제 결과
D (편차/Deviation) = σ-τ 비교
```

**Nofee의 본질:**
> "약속을 기록하고, 실제를 비교해서, 정직에게 수요를 재배분하는 시스템"

### 현재 문제점

리뷰 등록 API (`POST /review/store/phone/review-regist`)가 τ(실제 거래 결과)를 구조화된 형태로 저장하지 못함.

**현재 지원 필드:**
```java
public static class ReqReviewStorePhoneRegist {
    private Integer storeNo;
    private String content;     // 텍스트만
    private BigDecimal rating;  // 별점만
}
```

**필요한 필드 (σ-τ 비교용):**
- `priceMatchYn` - 시세표 가격과 실제 가격 일치 여부
- `noExtraFeeYn` - 추가 비용 없이 거래 완료 여부
- 기타 서비스 품질 체크리스트

---

## 2. 제안: 리뷰 체크리스트 필드 추가

### 2.1 DB 스키마 변경

```sql
-- tb_review_store_phone 테이블에 체크리스트 컬럼 추가
ALTER TABLE tb_review_store_phone ADD COLUMN price_match_yn CHAR(1) DEFAULT 'N' COMMENT '시세표 가격 일치 여부';
ALTER TABLE tb_review_store_phone ADD COLUMN no_extra_fee_yn CHAR(1) DEFAULT 'N' COMMENT '추가비용 없음 여부';
ALTER TABLE tb_review_store_phone ADD COLUMN kind_service_yn CHAR(1) DEFAULT 'N' COMMENT '친절한 서비스 여부';
ALTER TABLE tb_review_store_phone ADD COLUMN fast_response_yn CHAR(1) DEFAULT 'N' COMMENT '빠른 응답 여부';
ALTER TABLE tb_review_store_phone ADD COLUMN fast_process_yn CHAR(1) DEFAULT 'N' COMMENT '빠른 개통 여부';
```

### 2.2 DTO 변경

```java
/**
 * 후기-판매점휴대폰 등록 : 요청
 */
@Data
public static class ReqReviewStorePhoneRegist {
    @Schema(description = "판매점 번호")
    private Integer storeNo;

    @Schema(description = "후기 내용")
    private String content;

    @Schema(description = "별점")
    private BigDecimal rating;

    // === 체크리스트 (σ-τ 비교용) ===
    @Schema(description = "시세표 가격 일치 여부 (핵심)", example = "Y")
    private String priceMatchYn;

    @Schema(description = "추가비용 없음 여부 (핵심)", example = "Y")
    private String noExtraFeeYn;

    @Schema(description = "친절한 서비스 여부", example = "Y")
    private String kindServiceYn;

    @Schema(description = "빠른 응답 여부", example = "Y")
    private String fastResponseYn;

    @Schema(description = "빠른 개통 여부", example = "Y")
    private String fastProcessYn;
}
```

### 2.3 응답 DTO 변경

```java
@Data
public static class ResReviewStorePhone {
    // 기존 필드...

    // === 체크리스트 (σ-τ 비교용) ===
    @Schema(description = "시세표 가격 일치 여부")
    private String priceMatchYn;

    @Schema(description = "추가비용 없음 여부")
    private String noExtraFeeYn;

    @Schema(description = "친절한 서비스 여부")
    private String kindServiceYn;

    @Schema(description = "빠른 응답 여부")
    private String fastResponseYn;

    @Schema(description = "빠른 개통 여부")
    private String fastProcessYn;
}
```

### 2.4 매장별 체크리스트 집계 API (신규)

```
POST /review/store/phone/{storeNo}/stats
```

**Response:**
```json
{
  "totalReviews": 150,
  "avgRating": 4.7,
  "priceMatchCount": 142,
  "priceMatchRate": 0.947,
  "noExtraFeeCount": 138,
  "noExtraFeeRate": 0.92,
  "kindServiceCount": 120,
  "fastResponseCount": 98,
  "fastProcessCount": 110
}
```

---

## 3. 활용: TrustScore 및 신뢰온도 계산

체크리스트 데이터가 있으면 다음 기능 구현 가능:

### 3.1 신뢰온도 (TrustTemperature)
```typescript
// 온도 = 기본온도 + (시세표일치율 × 30°C) + (추가비용없음율 × 20°C) + ...
const temperature = 36.5 + (priceMatchRate * 30) + (noExtraFeeRate * 20) + ...
```

### 3.2 Trust Allocator (수요 재배분)
```typescript
// 정직한 매장에게 더 많은 리드 배분
DemandShare(store) = f(priceMatchRate, noExtraFeeRate, rating, ...)
```

### 3.3 후기 태그 표시
```
💰 시세표 그대로 (142명)
✨ 추가비용 없음 (138명)
😊 친절하고 상세 (120명)
```

---

## 4. 현재 프론트엔드 임시 처리

백엔드 업데이트 전까지 프론트엔드에서 다음과 같이 처리 중:

```typescript
// content에 태그 형식으로 체크리스트 저장
const contentWithTags = `${content}\n\n#시세표_그대로 #추가비용_없음 #친절_상세`;

// 조회 시 content에서 태그 파싱하여 통계 계산
const priceMatchCount = reviews.filter(r =>
  r.content.includes('#시세표_그대로')
).length;
```

**문제점:**
- 비정규화된 데이터 저장
- 태그 형식 변경 시 호환성 문제
- 정확한 집계 어려움

---

## 5. 우선순위

| 필드 | 우선순위 | 이유 |
|-----|---------|-----|
| `priceMatchYn` | **필수** | σ-τ 핵심 비교 지표 |
| `noExtraFeeYn` | **필수** | σ-τ 핵심 비교 지표 |
| `kindServiceYn` | 권장 | 서비스 품질 지표 |
| `fastResponseYn` | 권장 | 서비스 품질 지표 |
| `fastProcessYn` | 권장 | 서비스 품질 지표 |

---

## 6. 일정 제안

1. **1단계**: DB 스키마 변경 + DTO 업데이트 (1일)
2. **2단계**: 등록/조회 API 수정 (1일)
3. **3단계**: 매장별 통계 API 추가 (1일)
4. **4단계**: 프론트엔드 임시 로직 제거 (프론트 담당)

---

## 7. 참고 자료

- [claude.md](/Users/jacob/Desktop/dev/nofee/nofee-front/claude.md) - Nofee 비즈니스 철학
- [API_ENDPOINTS.md](/Users/jacob/Desktop/dev/nofee/API_ENDPOINTS.md) - API 문서
- 당근마켓 거래후기 Reference (매너온도 시스템)

---

**작성자**: Claude Code
**검토 요청**: 백엔드 팀
