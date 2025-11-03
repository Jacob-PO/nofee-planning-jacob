# 노피 DB 분석 요약

**분석 일시**: 2025-11-03
**데이터베이스**: db_nofee
**총 테이블 수**: 62개

---

## 📊 주요 통계

### 데이터 규모 (상위 10개 테이블)

| 순위 | 테이블명 | 데이터 건수 | 설명 |
|------|---------|-----------|------|
| 1 | tb_pricetable_phone_hist | 180,690건 | 가격표-휴대폰 이력 |
| 2 | tb_user | 5,236건 | 회원 |
| 3 | tb_pricetable_phone_temp | 4,797건 | 가격표(임시)-휴대폰 |
| 4 | tb_pricetable_phone | 4,759건 | 가격표-휴대폰 |
| 5 | tb_apply_phone | 4,100건 | 신청-휴대폰 |
| 6 | tb_apply_phone_memo | 3,838건 | 신청-휴대폰 메모 |
| 7 | tb_store_purchase | 3,026건 | 판매점-고객 DB 구매 |
| 8 | tb_review_virtual | 1,398건 | 가상 후기 |
| 9 | tb_campaign_phone_image | 789건 | 캠페인 이미지 |
| 10 | tb_pricetable_store_phone_row | 520건 | 판매점 휴대폰 시세표 (Row 형태) |

---

## 🗂️ 테이블 카테고리 분류

### 1. 계정 관리 (3개)
- `tb_admin_account` (4건) - 관리자 계정
- `tb_agency_account` (54건) - 판매점 관리자 계정
- `tb_user` (5,236건) - 일반 회원

### 2. 상품 관리 (8개)
- `tb_product_group_phone` (78건) - 상품 그룹-휴대폰
- `tb_product_group_phone_color` (218건) - 상품 색상
- `tb_product_phone` (159건) - 상품-휴대폰
- `tb_product_cable` (39건) - 상품-유선
- `tb_carrier_plan_phone` (3건) - 통신사별 정책
- `tb_rate_plan` (16건) - 요금제
- `tb_rate_plan_phone` (42건) - 요금제 (휴대폰용)
- `tb_common_code` (147건) - 공통코드

### 3. 가격표 관리 (10개)
**휴대폰 가격표**
- `tb_pricetable_phone` (4,759건) - 현재 가격표
- `tb_pricetable_phone_temp` (4,797건) - 임시 가격표
- `tb_pricetable_phone_hist` (180,690건) - 가격표 이력
- `tb_pricetable_phone_fail` (0건) - 실패 기록
- `tb_pricetable_store_phone_col` (134건) - 판매점 시세표 (Col)
- `tb_pricetable_store_phone_row` (520건) - 판매점 시세표 (Row)

**유선 가격표**
- `tb_pricetable_cable` (39건) - 현재 가격표
- `tb_pricetable_cable_temp` (39건) - 임시 가격표
- `tb_pricetable_cable_hist` (312건) - 가격표 이력
- `tb_pricetable_cable_fail` (0건) - 실패 기록

### 4. 신청/견적 관리 (8개)
**일반 신청**
- `tb_apply_phone` (4,100건) - 휴대폰 신청
- `tb_apply_phone_memo` (3,838건) - 휴대폰 신청 메모
- `tb_apply_phone_user` (475건) - 개통 회원 정보
- `tb_apply_cable` (84건) - 유선 신청
- `tb_apply_cable_memo` (11건) - 유선 신청 메모

**캠페인 신청**
- `tb_apply_campaign_phone` (500건) - 캠페인 휴대폰 신청
- `tb_apply_campaign_phone_memo` (356건) - 캠페인 신청 메모
- `tb_apply_campaign_phone_user` (59건) - 캠페인 개통 회원

### 5. 판매점 관리 (6개)
- `tb_store` (54건) - 판매점 정보
- `tb_agency_store_mapp` (54건) - 판매점 계정 매핑
- `tb_store_purchase` (3,026건) - 고객 DB 구매 내역
- `tb_store_favorite` (0건) - 판매점 좋아요
- `tb_store_complaint` (0건) - 판매점 신고

### 6. 캠페인/이벤트 (3개)
- `tb_campaign_phone` (74건) - 캠페인 정보
- `tb_campaign_phone_image` (789건) - 캠페인 이미지
- `tb_event_phone` (9건) - 이벤트

### 7. 후기 관리 (8개)
**실제 후기**
- `tb_review_phone` (1건) - 상품 후기
- `tb_review_phone_image` (0건) - 상품 후기 이미지
- `tb_review_store_phone` (15건) - 판매점 후기
- `tb_review_store_phone_image` (6건) - 판매점 후기 이미지
- `tb_review_campaign_phone` (0건) - 캠페인 후기
- `tb_review_campaign_phone_image` (0건) - 캠페인 후기 이미지

**가상 후기**
- `tb_review_store_phone_virtual` (243건) - 가상 판매점 후기
- `tb_review_store_phone_virtual_image` (12건) - 가상 후기 이미지
- `tb_review_virtual` (1,398건) - 가상 후기

### 8. 커뮤니티 (7개)
- `tb_freeboard` (0건) - 자유게시판
- `tb_freeboard_comment` (0건) - 댓글
- `tb_freeboard_comment_complaint` (0건) - 댓글 신고
- `tb_freeboard_comment_favorite` (0건) - 댓글 좋아요
- `tb_freeboard_complaint` (0건) - 게시글 신고
- `tb_freeboard_favorite` (0건) - 게시글 좋아요
- `tb_freeboard_image` (0건) - 게시글 이미지

### 9. 기타 시스템 (9개)
- `tb_area_sido` (17건) - 시도 정보
- `tb_area_sigungu` (219건) - 시군구 정보
- `tb_cert` (9건) - 본인인증
- `tb_cert_hist` (9건) - 본인인증 이력
- `tb_complaint` (0건) - 신고 (대리점→고객)
- `tb_payment` (3건) - 결제
- `tb_payment_product` (5건) - 결제 상품
- `tb_alimtalk_template` (1건) - 알림톡 템플릿
- `tb_alimtalk_template_button` (1건) - 알림톡 버튼

---

## 💡 주요 인사이트

### 1. 핵심 비즈니스 플로우
1. **회원** (5,236명) → **신청** (4,100건 휴대폰 + 500건 캠페인)
2. **판매점** (54개) → **가격표 등록** (4,759건 현재 + 180,690건 이력)
3. **판매점** → **고객 DB 구매** (3,026건)
4. **개통 완료** (475건 일반 + 59건 캠페인)

### 2. 가격표 이력 관리
- 휴대폰 가격표 이력이 **180,690건**으로 가장 많음
- 현재 가격표 대비 **38배** 많은 이력 데이터
- 가격 변동 추적 및 분석에 활용 가능

### 3. DB 판매 비즈니스
- 판매점의 고객 DB 구매: **3,026건**
- 전체 신청 건수 (4,600건) 대비 약 **66%** 판매율
- 핵심 수익 모델로 판단됨

### 4. 신청 대비 개통률
- 일반 신청: 4,100건 → 개통: 475건 (**11.6%**)
- 캠페인 신청: 500건 → 개통: 59건 (**11.8%**)
- 개통률 개선이 필요한 영역

### 5. 미활용 기능
- **자유게시판** 전체 0건 (미사용 상태)
- **캠페인 후기** 0건
- **상품 후기** 1건만 존재
- 커뮤니티 기능 활성화 필요

### 6. 가상 후기 활용
- 실제 판매점 후기: 15건
- 가상 판매점 후기: 243건
- 가상 후기: 1,398건
- **초기 서비스 신뢰도 확보 전략**으로 활용 중

---

## 🎯 개선 제안

### 1. 개통률 개선 (11.6% → 목표 50%)
- 알림톡 시스템 활용 (이미 템플릿 준비됨)
- 견적신청 10분룰 적용
- 개통 유도 프로세스 개선

### 2. 커뮤니티 활성화
- 자유게시판 기능 론칭
- 실제 후기 작성 유도 (현재 1건 → 목표 100건)
- 중고나라 스타일의 콘텐츠 마케팅

### 3. 가격표 이력 데이터 활용
- 180,690건의 가격 변동 데이터 분석
- 최저가 알림 서비스 개발
- 시세 변동 추이 시각화

### 4. 판매점 관리 강화
- 판매점 수: 54개 (적정 규모)
- 판매점별 성과 분석 대시보드
- 우수 판매점 인센티브 제도

---

## 📁 생성된 파일

1. **[db_config.json](../../../config/db_config.json)** - DB 접속 정보
2. **[analyze_db_structure.py](./analyze_db_structure.py)** - DB 분석 스크립트
3. **[db_structure_analysis.json](./db_structure_analysis.json)** - 상세 분석 데이터 (JSON)
4. **[db_structure_report.md](./db_structure_report.md)** - 전체 테이블 상세 리포트
5. **[db_analysis_summary.md](./db_analysis_summary.md)** - 본 문서 (요약)
