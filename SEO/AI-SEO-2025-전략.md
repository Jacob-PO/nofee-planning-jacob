# AI SEO 2025 전략 가이드

> 2025년 12월 기준 웹 검색 및 공식 문서 기반 정리
>
> 작성일: 2025-12-31

---

## 목차

1. [2025년 AI SEO 트렌드](#1-2025년-ai-seo-트렌드)
2. [Google AI Overview 최적화](#2-google-ai-overview-최적화)
3. [구조화된 데이터 (Schema Markup)](#3-구조화된-데이터-schema-markup)
4. [한국 시장 SEO 전략](#4-한국-시장-seo-전략)
5. [경쟁사 분석 (GSMArena, Versus, NanoReview)](#5-경쟁사-분석)
6. [노피 비교 페이지 적용 전략](#6-노피-비교-페이지-적용-전략)
7. [참고 자료](#7-참고-자료)

---

## 1. 2025년 AI SEO 트렌드

### 핵심 변화

1. **GEO(Generative Engine Optimization) 등장**
   - 기존 SEO: 클릭을 유도하는 전략
   - GEO: AI가 직접 답변할 때 인용되도록 최적화
   - ChatGPT, Gemini, Perplexity 등에서 출처로 인용되는 것이 목표

2. **AI Overview 확대**
   - Google 검색 결과의 20%까지 AI Overview가 차지
   - **74%의 AI Overview 인용은 상위 10개 검색결과에서 발생**
   - 기존 SEO 기본기가 여전히 중요

3. **구조화된 데이터의 중요성 증가**
   - AI 시스템이 콘텐츠를 "의미 단위"로 이해
   - GPT-4 성능: 일반 콘텐츠 16% → 구조화된 콘텐츠 54% 향상
   - 구조화된 데이터가 있는 페이지는 30% 더 높은 클릭률

### SEO vs AEO vs GEO

| 구분 | SEO | AEO (Answer Engine Optimization) | GEO |
|------|-----|----------------------------------|-----|
| 목표 | 검색 순위 상승 | Featured Snippet 노출 | AI 답변에 인용 |
| 대상 | Google, Naver | Google, Naver | ChatGPT, Gemini, Perplexity |
| 핵심 | 키워드, 백링크 | 질문-답변 형식 | 신뢰성, 구조화 데이터 |

---

## 2. Google AI Overview 최적화

### Google 공식 입장 (2025년)

> "AI Overviews와 AI Mode에 특별한 최적화는 필요하지 않습니다. 기존 SEO 기본 원칙이 계속 유효합니다."
>
> "새로운 머신 리더블 파일, AI 텍스트 파일, 또는 마크업을 만들 필요가 없습니다. 특별한 schema.org 구조화 데이터도 필요하지 않습니다."
>
> — [Google Search Central](https://developers.google.com/search/docs/appearance/ai-features)

### 그럼에도 중요한 것들

1. **기본 요구사항**
   - 페이지가 Google에 인덱싱되어 있을 것
   - 스니펫 표시가 가능할 것
   - 기본적인 SEO 기술 요구사항 충족

2. **권장 사항**
   - robots.txt로 크롤링 허용
   - 내부 링크로 발견 가능성 향상
   - 중요한 콘텐츠는 텍스트 기반으로 제공
   - 구조화된 데이터가 보이는 텍스트와 일치

3. **콘텐츠 제어**
   - `nosnippet` / `data-nosnippet` 사용 가능
   - `max-snippet` 지시문 사용 가능
   - `noindex` 태그로 AI 기능에서 제외 가능

### FAQ Schema의 효과

- **FAQPage 마크업이 있는 페이지는 AI Overview에 3.2배 더 많이 노출**
- 2025년 AI 세션 527% 증가
- Google이 2023년 8월 FAQ 리치 결과를 제한했지만, AI 플랫폼에서는 여전히 FAQ를 주요 출처로 활용

### 실무 최적화 팁

```markdown
- 50-70단어 요약으로 시작
- FAQ/HowTo 스키마 JSON-LD 사용
- "People Also Ask" 주제 기반 구조화
- 최신 통계 데이터 포함
- `<section>`, 순서 목록, `<table>` 활용
- 앵커 ID로 링크 가능한 섹션 제공 (예: #answer, #steps, #faq)
```

---

## 3. 구조화된 데이터 (Schema Markup)

### 개요

- **정의**: 검색엔진이 콘텐츠를 더 정확하게 이해하도록 돕는 표준화된 마크업
- **표준**: Schema.org (Google, Microsoft, Yahoo, Yandex 공동 프로젝트)
- **권장 형식**: JSON-LD (JavaScript Object Notation for Linked Data)

### 지원 현황

| 검색엔진 | 지원 스키마 수 |
|---------|--------------|
| Google | 35가지 |
| 네이버 | 14가지 |

### 핵심 스키마 타입

#### 1. Organization (조직)
```json
{
  "@context": "https://schema.org",
  "@type": "Organization",
  "name": "노피",
  "url": "https://nofee.team",
  "logo": "https://nofee.team/images/logo.png",
  "description": "전국 휴대폰성지 최저가 비교 플랫폼"
}
```

#### 2. Product (제품)
```json
{
  "@context": "https://schema.org",
  "@type": "Product",
  "name": "아이폰 17",
  "brand": { "@type": "Brand", "name": "Apple" },
  "offers": {
    "@type": "AggregateOffer",
    "lowPrice": 45000,
    "priceCurrency": "KRW",
    "offerCount": 120
  },
  "additionalProperty": [
    { "@type": "PropertyValue", "name": "화면크기", "value": "6.3인치" },
    { "@type": "PropertyValue", "name": "프로세서", "value": "A19" }
  ]
}
```

#### 3. BreadcrumbList (탐색경로)
```json
{
  "@context": "https://schema.org",
  "@type": "BreadcrumbList",
  "itemListElement": [
    { "@type": "ListItem", "position": 1, "name": "홈", "item": "https://nofee.team" },
    { "@type": "ListItem", "position": 2, "name": "스마트폰 비교", "item": "https://nofee.team/compare" }
  ]
}
```

#### 4. FAQPage (FAQ)
```json
{
  "@context": "https://schema.org",
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "아이폰 17과 갤럭시 S25 중 어떤게 좋을까요?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "게임 성능은 아이폰 17이, 카메라 줌은 갤럭시 S25가 우수합니다."
      }
    }
  ]
}
```

#### 5. ItemList (목록 - 비교 페이지용)
```json
{
  "@context": "https://schema.org",
  "@type": "ItemList",
  "name": "아이폰 17 vs 갤럭시 S25 비교",
  "numberOfItems": 2,
  "itemListElement": [
    { "@type": "ListItem", "position": 1, "item": { "@type": "Product", "name": "아이폰 17" } },
    { "@type": "ListItem", "position": 2, "item": { "@type": "Product", "name": "갤럭시 S25" } }
  ]
}
```

### 새로운 스키마 타입 (2025년 주목)

- **Speakable**: 음성 검색 최적화
- **QAPage**: Q&A 콘텐츠
- **WebPage**: 페이지 목적 명시

### SEO 효과

- 리치 결과 페이지 CTR: 일반 결과 대비 **82% 높음**
- 구조화된 데이터가 있는 페이지: **30% 더 높은 클릭률**

---

## 4. 한국 시장 SEO 전략

### 네이버 vs 구글

| 구분 | 네이버 | 구글 |
|------|--------|------|
| 타겟 | B2C, 한국 로컬 | B2B, 글로벌 |
| 특징 | 실시간 고객 행동 기반 | AI Overview 확대 |
| 스키마 지원 | 14가지 | 35가지 |

### 2025년 네이버 알고리즘 변화

- 사용자 경험(UX)과 AI 통합이 핵심
- 실시간 고객 행동 기반 알고리즘으로 전환
- JSON-LD 형식 스키마 마크업 권장

### 권장 전략

1. **리소스 제한 시**: 타겟 고객이 많이 사용하는 플랫폼에 집중
2. **이상적**: 두 플랫폼 모두 최적화
3. **B2C 한국 비즈니스**: 네이버 우선
4. **B2B/글로벌**: 구글 우선

---

## 5. 경쟁사 분석

### GSMArena

**URL 구조**: `https://www.gsmarena.com/compare.php3?idPhone1=13325&idPhone2=12825`

**분석 결과**:
- JSON-LD 구조화 데이터: **없음**
- Open Graph 태그: **없음**
- 타이틀: "Compare Umidigi G9T vs. Energizer Hard Case P28K - GSMArena.com"

**특징**:
- 세계 최대 스마트폰 스펙 사이트임에도 구조화된 데이터 미사용
- SEO보다 콘텐츠 품질과 도메인 권위도에 의존
- 차별화 기능: "Differences mode" (차이점만 하이라이트)

### Versus.com

**URL 구조**: `https://versus.com/en/apple-iphone-16-pro-vs-samsung-galaxy-s24-ultra`

**JSON-LD 스키마 사용**:
```json
{
  "@type": "Organization",
  "name": "Versus",
  "url": "https://versus.com"
}

{
  "@type": "WebSite",
  "name": "Versus",
  "potentialAction": {
    "@type": "SearchAction",
    "target": "https://versus.com/en/search?q={query}"
  }
}

{
  "@type": "BreadcrumbList",
  "itemListElement": [
    {"position": 1, "name": "Home"},
    {"position": 2, "name": "smartphone comparison"},
    {"position": 3, "name": "Apple iPhone 16 Pro vs Samsung Galaxy S24 Ultra"}
  ]
}

{
  "@type": "WebPage",
  "name": "Apple iPhone 16 Pro vs Samsung Galaxy S24 Ultra: What is the difference?",
  "url": "https://versus.com/en/apple-iphone-16-pro-vs-samsung-galaxy-s24-ultra",
  "description": "What is the difference between Samsung Galaxy S24 Ultra and Apple iPhone 16 Pro?"
}
```

**특징**:
- Organization, WebSite, BreadcrumbList, WebPage 스키마 모두 사용
- 질문형 타이틀: "What is the difference?"
- SearchAction으로 사이트 내 검색 기능 명시

### NanoReview

**URL 구조**: `https://nanoreview.net/en/phone-compare/apple-iphone-16-pro-vs-samsung-galaxy-s24-ultra`

**특징**:
- 글로벌 랭킹 6,148 → 8,776 (최근 하락)
- 경쟁사: cpu-monkey.com, technical.city, versus.com, notebookcheck.net
- 벤치마크, 실제 테스트 데이터 중심

---

## 6. 노피 비교 페이지 적용 전략

### 현재 적용된 SEO

#### 메타데이터
```typescript
// 타이틀 (55자 제한)
"{상품1} vs {상품2} 비교 | 스펙·가격 - 노피"

// Description (160자)
"{상품1}와 {상품2} 스펙 비교. 화면 6.3인치 vs 6.8인치. 월 45,000원 vs 52,000원. 어떤 폰이 나에게 맞을까?"

// Keywords (비교 검색 집중)
- "아이폰 17 vs 갤럭시 S25"
- "아이폰 17 갤럭시 S25 비교"
- "아이폰 17 갤럭시 S25 뭐가 좋아"
- "스펙 비교", "카메라 비교", "가격 비교"
```

#### 구조화된 데이터

1. **ItemList 스키마**: 두 상품 비교 정보
2. **Product 스키마**: 각 상품의 스펙, 가격
3. **BreadcrumbList 스키마**: 홈 > 스마트폰 비교 > {상품} vs {상품}

#### 사이트맵
- 인기 상품 10개 간 조합 = **45개 비교 페이지** URL 등록
- changeFrequency: weekly
- priority: 0.7

### 추가 개선 권장사항

#### 1. FAQ 스키마 추가 (AI Overview 최적화)
```json
{
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "아이폰 17과 갤럭시 S25 중 어떤 폰이 더 좋을까요?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "게임 성능은 아이폰 17(A19 칩)이, 카메라 줌은 갤럭시 S25(5배 광학줌)가 우수합니다."
      }
    }
  ]
}
```

#### 2. 콘텐츠 구조 개선
```html
<!-- 비교 요약 섹션 (50-70단어) -->
<section id="summary">
  <h2>아이폰 17 vs 갤럭시 S25 한눈에 비교</h2>
  <p>아이폰 17은 A19 칩으로 게임 성능이 뛰어나고,
  갤럭시 S25는 5배 광학줌으로 카메라가 강점입니다...</p>
</section>

<!-- 스펙 비교 테이블 -->
<table id="specs">
  <thead><tr><th>항목</th><th>아이폰 17</th><th>갤럭시 S25</th></tr></thead>
  <tbody>...</tbody>
</table>

<!-- FAQ 섹션 -->
<section id="faq">
  <h2>자주 묻는 질문</h2>
  ...
</section>
```

#### 3. Versus.com 스타일 질문형 타이틀
```
"아이폰 17 vs 갤럭시 S25: 차이점은?" (versus.com 스타일)
```

---

## 7. 참고 자료

### 공식 문서

- [Google AI Features and Your Website](https://developers.google.com/search/docs/appearance/ai-features)
- [Schema.org](https://schema.org/)
- [Google Search Central - Structured Data](https://developers.google.com/search/docs/appearance/structured-data)

### 2025년 SEO 가이드

- [Schema and AI Overviews: Does structured data improve visibility?](https://searchengineland.com/schema-ai-overviews-structured-data-visibility-462353)
- [Inside Google's AI Overviews (And How to Rank Inside Them)](https://www.seo.com/ai/ai-overviews/)
- [Structured data: SEO and GEO optimization for AI in 2025](https://www.digidop.com/blog/structured-data-secret-weapon-seo)
- [AI SEO in 2025: Optimizing for LLMs & AI Overviews](https://www.resultfirst.com/blog/ai-seo/seo-geo-for-ai-overviews-llms/)
- [Google AI Overviews: The Ultimate Guide to Ranking in 2025](https://www.singlegrain.com/search-everywhere-optimization/google-ai-overviews-the-ultimate-guide-to-ranking-in-2025/)

### 한국 SEO 자료

- [스키마 마크업 가이드: SEO·GEO 성과를 높이는 구조화 전략](https://238lab.kr/blog-seo-schemamarkup)
- [2025년 구글·네이버 알고리즘 변화 완벽 대비 SEO 가이드](https://adall.kr/blog/2a300db2-5013-4108-a86b-df43343f70fd/)
- [구조화된 데이터, 검색 상위노출의 핵심 필수 전략](https://seo.tbwakorea.com/blog/structured-data-guide/)
- [NHN 커머스 - 테크니컬 SEO: 구조화된 데이터 활용](https://marketing-help.nhn-commerce.com/insight/seo/data)

### FAQ Schema

- [Are FAQ Schemas Important for AI Search, GEO & AEO?](https://www.frase.io/blog/faq-schema-ai-search-geo-aeo)
- [FAQ Schema for AI Answers | Setup Guide & Examples](https://www.getpassionfruit.com/blog/faq-schema-for-ai-answers)

---

## 핵심 요약

### 2025년 AI SEO 체크리스트

- [ ] 기본 SEO 최적화 (인덱싱, 크롤링, 내부 링크)
- [ ] JSON-LD 구조화된 데이터 적용
  - [ ] Organization
  - [ ] Product
  - [ ] BreadcrumbList
  - [ ] FAQPage (AI Overview 최적화)
  - [ ] ItemList (비교 페이지)
- [ ] 50-70단어 요약 섹션
- [ ] `<section>`, `<table>`, 순서 목록 활용
- [ ] 앵커 ID로 링크 가능한 섹션 제공
- [ ] 메타데이터 최적화 (title 55자, description 160자)
- [ ] 비교 검색 키워드 집중 (vs, 비교, 뭐가 좋아 등)
- [ ] 사이트맵에 비교 페이지 등록

### 핵심 인사이트

> "구조화된 데이터가 제대로 세팅되면 검색엔진은 페이지의 내용을 텍스트 단위가 아니라 '의미 단위'로 이해하게 되고, 생성형 AI는 이 정보를 신뢰 가능한 근거로 활용할 수 있습니다."

> "74%의 AI Overview 인용은 상위 10개 검색결과에서 발생합니다. 기존 SEO 기본기가 여전히 중요합니다."
