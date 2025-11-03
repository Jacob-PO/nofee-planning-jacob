# 노피 운영 DB 테이블 구조 분석 리포트

**분석 일시**: 2025-11-03 09:48:06
**데이터베이스**: db_nofee
**총 테이블 수**: 62

---

## 목차

- [tb_admin_account](#tb_admin_account) - 관리자 계정 (4건)
- [tb_agency_account](#tb_agency_account) - 판매점 관리자 계정 (54건)
- [tb_agency_store_mapp](#tb_agency_store_mapp) - 판매점 계정-판매점 매핑 (54건)
- [tb_alimtalk_template](#tb_alimtalk_template) - 카카오 알림톡 템플릿 (1건)
- [tb_alimtalk_template_button](#tb_alimtalk_template_button) - 카카오 알림톡 템플릿 버튼 (1건)
- [tb_apply_cable](#tb_apply_cable) - 신청-유선 (84건)
- [tb_apply_cable_memo](#tb_apply_cable_memo) - 설명 없음 (11건)
- [tb_apply_campaign_phone](#tb_apply_campaign_phone) - 캠페인 신청-휴대폰 (500건)
- [tb_apply_campaign_phone_memo](#tb_apply_campaign_phone_memo) - 캠페인 신청 메모-휴대폰 (356건)
- [tb_apply_campaign_phone_user](#tb_apply_campaign_phone_user) - 캠페인 신청-휴대폰-개통 회원 (59건)
- [tb_apply_phone](#tb_apply_phone) - 신청-휴대폰 (4,100건)
- [tb_apply_phone_memo](#tb_apply_phone_memo) - 설명 없음 (3,838건)
- [tb_apply_phone_user](#tb_apply_phone_user) - 신청-휴대폰-개통 회원 (475건)
- [tb_area_sido](#tb_area_sido) - 지역-시도 (17건)
- [tb_area_sigungu](#tb_area_sigungu) - 지역-시군구 (219건)
- [tb_campaign_phone](#tb_campaign_phone) - 캠페인 정보 테이블 (이벤트/소식) (74건)
- [tb_campaign_phone_image](#tb_campaign_phone_image) - 캠페인 이미지 (789건)
- [tb_carrier_plan_phone](#tb_carrier_plan_phone) - 통신사별 정책-휴대폰 (3건)
- [tb_cert](#tb_cert) - 본인인증 (9건)
- [tb_cert_hist](#tb_cert_hist) - 본인인증-기록 (9건)
- [tb_common_code](#tb_common_code) - 공통코드 (147건)
- [tb_complaint](#tb_complaint) - 신고 (대리점에서 고객 신고) (0건)
- [tb_event_phone](#tb_event_phone) - 이벤트-휴대폰 (9건)
- [tb_freeboard](#tb_freeboard) - 자유게시판 (0건)
- [tb_freeboard_comment](#tb_freeboard_comment) - 자유게시판 댓글 (0건)
- [tb_freeboard_comment_complaint](#tb_freeboard_comment_complaint) - 자유게시판 댓글 신고 (0건)
- [tb_freeboard_comment_favorite](#tb_freeboard_comment_favorite) - 자유게시판 댓글 좋아요 (0건)
- [tb_freeboard_complaint](#tb_freeboard_complaint) - 자유게시판 신고 (0건)
- [tb_freeboard_favorite](#tb_freeboard_favorite) - 자유게시판 좋아요 (0건)
- [tb_freeboard_image](#tb_freeboard_image) - 자유게시판 이미지 (0건)
- [tb_payment](#tb_payment) - 결제 (3건)
- [tb_payment_product](#tb_payment_product) - 결제 상품 (5건)
- [tb_pricetable_cable](#tb_pricetable_cable) - 가격표-유선 (39건)
- [tb_pricetable_cable_fail](#tb_pricetable_cable_fail) - 가격표-유선 : 실패 (0건)
- [tb_pricetable_cable_hist](#tb_pricetable_cable_hist) - 가격표-유선 : 이력 (312건)
- [tb_pricetable_cable_temp](#tb_pricetable_cable_temp) - 가격표(임시)-유선 (39건)
- [tb_pricetable_phone](#tb_pricetable_phone) - 가격표-휴대폰 (4,759건)
- [tb_pricetable_phone_fail](#tb_pricetable_phone_fail) - 가격표-휴대폰 : 실패 (0건)
- [tb_pricetable_phone_hist](#tb_pricetable_phone_hist) - 가격표-휴대폰 : 이력 (180,690건)
- [tb_pricetable_phone_temp](#tb_pricetable_phone_temp) - 가격표(임시)-휴대폰 (4,797건)
- [tb_pricetable_store_phone_col](#tb_pricetable_store_phone_col) - 판매점 휴대폰 시세표 (Col 형태) (134건)
- [tb_pricetable_store_phone_row](#tb_pricetable_store_phone_row) - 판매점 휴대폰 시세표 (Row 형태) (520건)
- [tb_product_cable](#tb_product_cable) - 상품-유선 (39건)
- [tb_product_group_phone](#tb_product_group_phone) - 상품 그룹-휴대폰 (78건)
- [tb_product_group_phone_color](#tb_product_group_phone_color) - 상품 그룹-휴대폰 색상 (218건)
- [tb_product_phone](#tb_product_phone) - 상품-휴대폰 (159건)
- [tb_rate_plan](#tb_rate_plan) - 요금제 (16건)
- [tb_rate_plan_phone](#tb_rate_plan_phone) - 요금제 (42건)
- [tb_review_campaign_phone](#tb_review_campaign_phone) - 캠페인 휴대폰-후기 (0건)
- [tb_review_campaign_phone_image](#tb_review_campaign_phone_image) - 캠페인 휴대폰-후기 이미지 (0건)
- [tb_review_phone](#tb_review_phone) - 상품 그룹-후기 (1건)
- [tb_review_phone_image](#tb_review_phone_image) - 상품 그룹-후기 이미지 (0건)
- [tb_review_store_phone](#tb_review_store_phone) - 판매점 휴대폰-후기 (15건)
- [tb_review_store_phone_image](#tb_review_store_phone_image) - 판매점 휴대폰-후기 이미지 (6건)
- [tb_review_store_phone_virtual](#tb_review_store_phone_virtual) - 판매점 휴대폰-후기 (가상) (243건)
- [tb_review_store_phone_virtual_image](#tb_review_store_phone_virtual_image) - 판매점 휴대폰-후기 (가상) 이미지 (12건)
- [tb_review_virtual](#tb_review_virtual) - 설명 없음 (1,398건)
- [tb_store](#tb_store) - 판매점 (54건)
- [tb_store_complaint](#tb_store_complaint) - 판매점 신고 (0건)
- [tb_store_favorite](#tb_store_favorite) - 판매점 좋아요 (0건)
- [tb_store_purchase](#tb_store_purchase) - 판매점-고객 DB 구매 (3,026건)
- [tb_user](#tb_user) - 회원 (5,236건)

---

## tb_admin_account

**설명**: 관리자 계정

**데이터 건수**: 4건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| account_no | int(11) | NO | PRI | - | auto_increment |
| id | varchar(50) | NO |  | - | - |
| password | blob | YES |  | - | - |
| admin_nm | blob | NO |  | - | - |
| tel_no | blob | YES |  | - | - |
| introduce | text | YES |  | - | - |
| note | text | YES |  | - | - |
| auth_code | varchar(10) | NO |  | 0101001001 | - |
| created_at | timestamp | YES |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): account_no

---

## tb_agency_account

**설명**: 판매점 관리자 계정

**데이터 건수**: 54건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| account_no | int(11) | NO | PRI | - | auto_increment |
| id | varchar(50) | NO |  | - | - |
| password | blob | YES |  | - | - |
| agency_nm | blob | NO |  | - | - |
| tel_no | blob | YES |  | - | - |
| introduce | text | YES |  | - | - |
| note | text | YES |  | - | - |
| alarm_message_yn | varchar(1) | NO |  | Y | - |
| alarm_talk_yn | varchar(1) | NO |  | Y | - |
| auth_code | varchar(10) | NO | MUL | 0101002002 | - |
| init_yn | varchar(1) | NO |  | Y | - |
| created_at | timestamp | YES |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): account_no
- **tb_agency_account_idx_1** (INDEX): auth_code

---

## tb_agency_store_mapp

**설명**: 판매점 계정-판매점 매핑

**데이터 건수**: 54건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| account_no | int(11) | NO | PRI | - | - |
| store_no | int(11) | NO | PRI | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): account_no, store_no

---

## tb_alimtalk_template

**설명**: 카카오 알림톡 템플릿

**데이터 건수**: 1건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| template_no | int(11) | NO | PRI | - | auto_increment |
| template_code | varchar(50) | NO | MUL | - | - |
| template_nm | varchar(200) | NO |  | - | - |
| template_content | text | NO |  | - | - |
| status_code | varchar(10) | YES | MUL | 0301001001 | - |
| note | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): template_no
- **uk_template_code** (UNIQUE): template_code, deleted_yn
- **idx_status_code** (INDEX): status_code
- **idx_deleted_yn** (INDEX): deleted_yn

---

## tb_alimtalk_template_button

**설명**: 카카오 알림톡 템플릿 버튼

**데이터 건수**: 1건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| button_no | int(11) | NO | PRI | - | auto_increment |
| template_no | int(11) | NO | MUL | - | - |
| button_type | varchar(10) | NO |  | - | - |
| button_nm | varchar(50) | NO |  | - | - |
| link_mobile | text | YES |  | - | - |
| link_pc | text | YES |  | - | - |
| ord | int(11) | YES |  | 1 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): button_no
- **idx_template_no** (INDEX): template_no, deleted_yn, ord
- **idx_deleted_yn** (INDEX): deleted_yn

---

## tb_apply_cable

**설명**: 신청-유선

**데이터 건수**: 84건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| apply_no | int(11) | NO | PRI | - | auto_increment |
| apply_product_code | varchar(100) | NO |  | - | - |
| user_no | int(11) | NO | MUL | - | - |
| user_nm | blob | NO |  | - | - |
| apply_tel_no | blob | YES |  | - | - |
| current_carrier_code | varchar(10) | YES |  | - | - |
| apply_bundle_yn | varchar(10) | NO |  | Y | - |
| apply_carrier_code | varchar(10) | NO |  | - | - |
| apply_internet_speed_code | varchar(10) | NO |  | - | - |
| apply_tv_product_code | varchar(10) | NO |  | - | - |
| apply_pricetable_dt | date | YES |  | - | - |
| apply_gift_price | int(11) | YES |  | - | - |
| apply_basic_price | int(11) | YES |  | - | - |
| apply_bundle_price | int(11) | YES |  | - | - |
| apply_sido_no | int(11) | YES |  | - | - |
| apply_sido_nm | varchar(100) | YES |  | - | - |
| apply_sigungu_no | int(11) | YES |  | - | - |
| apply_sigungu_nm | varchar(100) | YES |  | - | - |
| apply_at | timestamp | NO |  | current_timestamp() | - |
| store_no | int(11) | YES |  | - | - |
| completed_product_code | varchar(100) | YES |  | - | - |
| completed_bundle_yn | varchar(10) | YES |  | - | - |
| completed_carrier_code | varchar(10) | YES |  | - | - |
| completed_internet_speed_code | varchar(10) | YES |  | - | - |
| completed_tv_product_code | varchar(10) | YES |  | - | - |
| completed_pricetable_dt | date | YES |  | - | - |
| completed_gift_price | int(11) | YES |  | - | - |
| completed_basic_price | int(11) | YES |  | - | - |
| completed_bundle_price | int(11) | YES |  | - | - |
| completed_sido_no | int(11) | YES |  | - | - |
| completed_sido_nm | varchar(100) | YES |  | - | - |
| completed_sigungu_no | int(11) | YES |  | - | - |
| completed_sigungu_nm | varchar(100) | YES |  | - | - |
| completed_at | timestamp | YES |  | - | - |
| review_yn | varchar(1) | NO |  | N | - |
| step_code | varchar(10) | NO |  | 0201001 | - |
| note | text | YES |  | - | - |
| complaint_yn | varchar(1) | NO |  | N | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | YES |  | - | - |
| modified_no | int(11) | YES |  | - | - |
| modified_agency_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): apply_no
- **idx_apply_cable_user** (INDEX): user_no, deleted_yn, step_code
- **idx_apply_cable_main** (INDEX): deleted_yn, step_code, apply_no

---

## tb_apply_cable_memo

**데이터 건수**: 11건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| note_no | int(11) | NO | PRI | - | auto_increment |
| apply_no | int(11) | NO |  | - | - |
| memo | text | YES |  | - | - |
| clip_yn | varchar(1) | NO |  | N | - |
| note | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_agency_no | int(11) | YES |  | - | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| modified_agency_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): note_no

---

## tb_apply_campaign_phone

**설명**: 캠페인 신청-휴대폰

**데이터 건수**: 500건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| apply_no | int(11) | NO | PRI | - | auto_increment |
| apply_product_group_code | varchar(100) | NO |  | - | - |
| apply_product_code | varchar(100) | NO |  | - | - |
| user_no | int(11) | NO |  | - | - |
| user_nm | blob | NO |  | - | - |
| apply_tel_no | blob | YES |  | - | - |
| apply_storage_code | varchar(10) | NO |  | - | - |
| apply_carrier_code | varchar(10) | NO |  | - | - |
| apply_rate_plan_code | varchar(100) | NO |  | - | - |
| apply_month_fee | int(11) | YES |  | - | - |
| apply_optional_discount_rate | decimal(3,2) | YES |  | - | - |
| apply_join_type_code | varchar(10) | NO |  | - | - |
| apply_support_type_code | varchar(10) | NO |  | - | - |
| apply_pricetable_dt | date | YES |  | - | - |
| apply_release_price | int(11) | YES |  | - | - |
| apply_installment_principal | int(11) | YES |  | - | - |
| apply_discount_price | int(11) | YES |  | - | - |
| apply_discount_rate | int(11) | YES |  | - | - |
| apply_month_rate_plan_fee | int(11) | YES |  | - | - |
| apply_change_month_rate_plan_fee | int(11) | YES |  | - | - |
| apply_month_price | int(11) | YES |  | - | - |
| apply_rate_plan_maintain_days | int(11) | YES |  | - | - |
| apply_month_avg_days | decimal(5,2) | YES |  | - | - |
| apply_contract_months | decimal(10,2) | YES |  | - | - |
| apply_rate_plan_maintain_month | int(11) | YES |  | - | - |
| apply_remaining_months | decimal(10,2) | YES |  | - | - |
| apply_sido_no | int(11) | YES |  | - | - |
| apply_sido_nm | varchar(100) | YES |  | - | - |
| apply_sigungu_no | int(11) | YES |  | - | - |
| apply_sigungu_nm | varchar(100) | YES |  | - | - |
| apply_manufacturer_code | varchar(10) | YES |  | - | - |
| apply_at | timestamp | NO |  | current_timestamp() | - |
| store_no | int(11) | YES |  | - | - |
| completed_product_group_code | varchar(100) | YES |  | - | - |
| completed_product_code | varchar(100) | YES |  | - | - |
| completed_tel_no | blob | YES |  | - | - |
| completed_storage_code | varchar(10) | YES |  | - | - |
| completed_carrier_code | varchar(10) | YES |  | - | - |
| completed_rate_plan_code | varchar(100) | YES |  | - | - |
| completed_month_fee | int(11) | YES |  | - | - |
| completed_optional_discount_rate | decimal(3,2) | YES |  | - | - |
| completed_join_type_code | varchar(10) | YES |  | - | - |
| completed_support_type_code | varchar(10) | YES |  | - | - |
| completed_pricetable_dt | date | YES |  | - | - |
| completed_release_price | int(11) | YES |  | - | - |
| completed_installment_principal | int(11) | YES |  | - | - |
| completed_discount_price | int(11) | YES |  | - | - |
| completed_discount_rate | int(11) | YES |  | - | - |
| completed_month_rate_plan_fee | int(11) | YES |  | - | - |
| completed_change_month_rate_plan_fee | int(11) | YES |  | - | - |
| completed_month_price | int(11) | YES |  | - | - |
| completed_rate_plan_maintain_days | int(11) | YES |  | - | - |
| completed_month_avg_days | decimal(5,2) | YES |  | - | - |
| completed_contract_months | decimal(10,2) | YES |  | - | - |
| completed_rate_plan_maintain_month | int(11) | YES |  | - | - |
| completed_remaining_months | decimal(10,2) | YES |  | - | - |
| completed_contract_period | int(11) | YES |  | - | - |
| completed_sido_no | int(11) | YES |  | - | - |
| completed_sido_nm | varchar(100) | YES |  | - | - |
| completed_sigungu_no | int(11) | YES |  | - | - |
| completed_sigungu_nm | varchar(100) | YES |  | - | - |
| completed_price_range_code | varchar(10) | YES |  | - | - |
| completed_manufacturer_code | varchar(10) | YES |  | - | - |
| open_info_yn | varchar(1) | NO |  | N | - |
| completed_at | timestamp | YES |  | - | - |
| review_yn | varchar(1) | NO |  | N | - |
| step_code | varchar(10) | NO |  | 0201001 | - |
| reminder_yn | char(1) | YES | MUL | N | - |
| note | text | YES |  | - | - |
| complaint_yn | varchar(1) | NO |  | N | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | YES |  | - | - |
| modified_no | int(11) | YES |  | - | - |
| modified_agency_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): apply_no
- **idx_apply_campaign_phone_main** (INDEX): deleted_yn, step_code, apply_no
- **idx_reminder_yn** (INDEX): reminder_yn, apply_at

---

## tb_apply_campaign_phone_memo

**설명**: 캠페인 신청 메모-휴대폰

**데이터 건수**: 356건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| note_no | int(11) | NO | PRI | - | auto_increment |
| apply_no | int(11) | NO |  | - | - |
| memo | text | YES |  | - | - |
| clip_yn | varchar(1) | NO |  | N | - |
| note | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_agency_no | int(11) | YES |  | - | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| modified_agency_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): note_no

---

## tb_apply_campaign_phone_user

**설명**: 캠페인 신청-휴대폰-개통 회원

**데이터 건수**: 59건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| apply_user_no | int(11) | NO | PRI | - | auto_increment |
| apply_no | int(11) | NO | MUL | - | - |
| user_no | int(11) | NO |  | - | - |
| user_nm | blob | NO |  | - | - |
| tel_no | blob | NO |  | - | - |
| birthday | blob | NO |  | - | - |
| email | varchar(100) | YES |  | - | - |
| address | blob | NO |  | - | - |
| address_detail | blob | NO |  | - | - |
| id_file_location | text | YES |  | - | - |
| id_file_nm | varchar(60) | YES |  | - | - |
| bank_nm | blob | NO |  | - | - |
| account_holder | blob | NO |  | - | - |
| account_number | blob | NO |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): apply_user_no
- **idx_apply_campaign_phone_user_join** (INDEX): apply_no, deleted_yn

---

## tb_apply_phone

**설명**: 신청-휴대폰

**데이터 건수**: 4,100건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| apply_no | int(11) | NO | PRI | - | auto_increment |
| apply_product_group_code | varchar(100) | NO |  | - | - |
| apply_product_code | varchar(100) | NO |  | - | - |
| user_no | int(11) | NO | MUL | - | - |
| user_nm | blob | NO |  | - | - |
| apply_tel_no | blob | YES |  | - | - |
| apply_storage_code | varchar(10) | NO |  | - | - |
| apply_color_no | int(11) | YES |  | - | - |
| current_carrier_code | varchar(10) | YES |  | - | - |
| apply_carrier_code | varchar(10) | NO |  | - | - |
| apply_rate_plan_code | varchar(100) | NO |  | - | - |
| apply_month_fee | int(11) | YES |  | - | - |
| apply_min_month_fee | int(11) | YES |  | - | - |
| apply_basic_month_fee | int(11) | YES |  | - | - |
| apply_public_support_days | int(3) | YES |  | - | - |
| apply_optional_contract_days | int(3) | YES |  | - | - |
| apply_optional_discount_rate | decimal(3,2) | YES |  | - | - |
| apply_join_type_code | varchar(10) | NO |  | - | - |
| apply_support_type_code | varchar(10) | NO |  | - | - |
| apply_pricetable_dt | date | YES |  | - | - |
| apply_dealer | varchar(50) | YES |  | - | - |
| apply_rate_plan_month_fee | int(11) | YES |  | - | - |
| apply_retail_price | int(11) | YES |  | - | - |
| apply_total_support_fee | int(11) | YES |  | - | - |
| apply_dealer_subsidy | int(11) | YES |  | - | - |
| apply_origin_installment_principal | int(11) | YES |  | - | - |
| apply_origin_month_device_price | int(11) | YES |  | - | - |
| apply_origin_month_price | int(11) | YES |  | - | - |
| apply_month_rate_plan_price | int(11) | YES |  | - | - |
| apply_installment_principal | int(11) | YES |  | - | - |
| apply_month_device_price | int(11) | YES |  | - | - |
| apply_month_price | int(11) | YES |  | - | - |
| apply_margin | decimal(5,2) | YES |  | - | - |
| apply_margin_amount | int(11) | YES |  | - | - |
| apply_sido_no | int(11) | YES |  | - | - |
| apply_sido_nm | varchar(100) | YES |  | - | - |
| apply_sigungu_no | int(11) | YES |  | - | - |
| apply_sigungu_nm | varchar(100) | YES |  | - | - |
| apply_price_range_code | varchar(10) | YES |  | - | - |
| apply_manufacturer_code | varchar(10) | YES |  | - | - |
| apply_at | timestamp | NO | MUL | current_timestamp() | - |
| store_no | int(11) | YES |  | - | - |
| completed_product_group_code | varchar(100) | YES |  | - | - |
| completed_product_code | varchar(100) | YES |  | - | - |
| completed_tel_no | blob | YES |  | - | - |
| completed_storage_code | varchar(10) | YES |  | - | - |
| completed_color_no | int(11) | YES |  | - | - |
| completed_carrier_code | varchar(10) | YES |  | - | - |
| completed_rate_plan_code | varchar(100) | YES |  | - | - |
| completed_month_fee | int(11) | YES |  | - | - |
| completed_min_month_fee | int(11) | YES |  | - | - |
| completed_basic_month_fee | int(11) | YES |  | - | - |
| completed_public_support_days | int(3) | YES |  | - | - |
| completed_optional_contract_days | int(3) | YES |  | - | - |
| completed_optional_discount_rate | decimal(3,2) | YES |  | - | - |
| completed_join_type_code | varchar(10) | YES |  | - | - |
| completed_support_type_code | varchar(10) | YES |  | - | - |
| completed_pricetable_dt | date | YES |  | - | - |
| completed_dealer | varchar(50) | YES |  | - | - |
| completed_rate_plan_month_fee | int(11) | YES |  | - | - |
| completed_retail_price | int(11) | YES |  | - | - |
| completed_total_support_fee | int(11) | YES |  | - | - |
| completed_dealer_subsidy | int(11) | YES |  | - | - |
| completed_origin_installment_principal | int(11) | YES |  | - | - |
| completed_origin_month_device_price | int(11) | YES |  | - | - |
| completed_origin_month_price | int(11) | YES |  | - | - |
| completed_month_rate_plan_price | int(11) | YES |  | - | - |
| completed_installment_principal | int(11) | YES |  | - | - |
| completed_month_device_price | int(11) | YES |  | - | - |
| completed_month_price | int(11) | YES |  | - | - |
| completed_margin | decimal(5,2) | YES |  | - | - |
| completed_margin_amount | int(11) | YES |  | - | - |
| completed_contract_period | int(11) | YES |  | - | - |
| completed_sido_no | int(11) | YES |  | - | - |
| completed_sido_nm | varchar(100) | YES |  | - | - |
| completed_sigungu_no | int(11) | YES |  | - | - |
| completed_sigungu_nm | varchar(100) | YES |  | - | - |
| completed_price_range_code | varchar(10) | YES |  | - | - |
| completed_manufacturer_code | varchar(10) | YES |  | - | - |
| open_info_yn | varchar(1) | NO |  | N | - |
| completed_at | timestamp | YES |  | - | - |
| review_yn | varchar(1) | NO |  | N | - |
| step_code | varchar(10) | NO |  | 0201001 | - |
| reminder_yn | char(1) | YES | MUL | N | - |
| note | text | YES |  | - | - |
| complaint_yn | varchar(1) | NO |  | N | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | YES |  | - | - |
| modified_no | int(11) | YES |  | - | - |
| modified_agency_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): apply_no
- **idx_apply_phone_main** (INDEX): deleted_yn, step_code, apply_no
- **idx_apply_phone_user** (INDEX): user_no, deleted_yn, step_code
- **idx_apply_phone_apply_date** (INDEX): apply_at, deleted_yn
- **idx_reminder_yn** (INDEX): reminder_yn, apply_at

---

## tb_apply_phone_memo

**데이터 건수**: 3,838건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| note_no | int(11) | NO | PRI | - | auto_increment |
| apply_no | int(11) | NO |  | - | - |
| memo | text | YES |  | - | - |
| clip_yn | varchar(1) | NO |  | N | - |
| note | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_agency_no | int(11) | YES |  | - | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| modified_agency_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): note_no

---

## tb_apply_phone_user

**설명**: 신청-휴대폰-개통 회원

**데이터 건수**: 475건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| apply_user_no | int(11) | NO | PRI | - | auto_increment |
| apply_no | int(11) | NO | MUL | - | - |
| user_no | int(11) | NO |  | - | - |
| user_nm | blob | NO |  | - | - |
| tel_no | blob | NO |  | - | - |
| birthday | blob | NO |  | - | - |
| email | varchar(100) | YES |  | - | - |
| address | blob | NO |  | - | - |
| address_detail | blob | NO |  | - | - |
| id_file_location | text | YES |  | - | - |
| id_file_nm | varchar(60) | YES |  | - | - |
| bank_nm | blob | NO |  | - | - |
| account_holder | blob | NO |  | - | - |
| account_number | blob | NO |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): apply_user_no
- **idx_apply_phone_user_join** (INDEX): apply_no, deleted_yn

---

## tb_area_sido

**설명**: 지역-시도

**데이터 건수**: 17건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| sido_no | int(11) | NO | PRI | - | auto_increment |
| sido_nm | varchar(100) | YES |  | - | - |
| note | text | YES |  | - | - |
| ord | int(11) | NO |  | 0 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | NO |  | 0 | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| modified_no | int(11) | NO |  | 0 | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): sido_no
- **idx_area_sido_deleted** (INDEX): deleted_yn, sido_no

---

## tb_area_sigungu

**설명**: 지역-시군구

**데이터 건수**: 219건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| sigungu_no | int(11) | NO | PRI | - | auto_increment |
| sido_no | int(11) | NO | MUL | - | - |
| sigungu_nm | varchar(100) | YES |  | - | - |
| note | text | YES |  | - | - |
| ord | int(11) | NO |  | 0 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): sigungu_no
- **idx_area_sigungu_deleted** (INDEX): deleted_yn, sigungu_no
- **idx_area_sigungu_sido** (INDEX): sido_no, deleted_yn, sigungu_no

---

## tb_campaign_phone

**설명**: 캠페인 정보 테이블 (이벤트/소식)

**데이터 건수**: 74건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| campaign_no | int(11) | NO | PRI | - | auto_increment |
| store_no | int(11) | NO | MUL | - | - |
| campaign_type_code | varchar(10) | YES | MUL | - | - |
| title | varchar(100) | NO |  | - | - |
| content | text | YES |  | - | - |
| start_at | timestamp | YES | MUL | - | - |
| end_at | timestamp | YES |  | - | - |
| product_group_code | varchar(10) | YES |  | - | - |
| product_code | varchar(20) | YES |  | - | - |
| carrier_code | varchar(10) | YES |  | - | - |
| rate_plan_code | varchar(10) | YES |  | - | - |
| join_type_code | varchar(10) | YES |  | - | - |
| support_type_code | varchar(10) | YES |  | - | - |
| installment_principal | int(11) | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_agency_no | int(11) | YES |  | - | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| modified_agency_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |
| deleted_reason | text | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): campaign_no
- **idx_store_no** (INDEX): store_no
- **idx_deleted_yn** (INDEX): deleted_yn
- **idx_campaign_phone_type** (INDEX): campaign_type_code, deleted_yn
- **idx_campaign_phone_store_location** (INDEX): store_no, deleted_yn, campaign_type_code
- **idx_campaign_phone_period** (INDEX): start_at, end_at, deleted_yn
- **idx_campaign_phone_recent** (INDEX): deleted_yn, campaign_no

---

## tb_campaign_phone_image

**설명**: 캠페인 이미지

**데이터 건수**: 789건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| image_no | int(11) | NO | PRI | - | auto_increment |
| campaign_no | int(11) | NO |  | - | - |
| img_location | text | YES |  | - | - |
| img_nm | varchar(60) | YES |  | - | - |
| ord | int(11) | NO |  | 0 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): image_no

---

## tb_carrier_plan_phone

**설명**: 통신사별 정책-휴대폰

**데이터 건수**: 3건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| carrier_code | varchar(10) | NO | PRI | - | - |
| min_month_fee | int(11) | YES |  | - | - |
| basic_month_fee | int(11) | YES |  | - | - |
| public_support_days | int(3) | YES |  | - | - |
| optional_contract_days | int(3) | YES |  | - | - |
| optional_discount_rate | decimal(3,2) | YES |  | - | - |
| note | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): carrier_code

---

## tb_cert

**설명**: 본인인증

**데이터 건수**: 9건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| cert_no | int(11) | NO | PRI | - | auto_increment |
| nm | blob | NO |  | - | - |
| birthday | blob | NO |  | - | - |
| tel_no | blob | NO |  | - | - |
| created_at | timestamp | YES |  | current_timestamp() | - |

### 인덱스

- **PRIMARY** (UNIQUE): cert_no

---

## tb_cert_hist

**설명**: 본인인증-기록

**데이터 건수**: 9건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| cert_no | int(11) | NO | PRI | - | - |
| nm | blob | NO |  | - | - |
| birthday | blob | NO |  | - | - |
| tel_no | blob | NO |  | - | - |
| created_at | timestamp | YES |  | current_timestamp() | - |

### 인덱스

- **PRIMARY** (UNIQUE): cert_no

---

## tb_common_code

**설명**: 공통코드

**데이터 건수**: 147건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| code | varchar(10) | NO | PRI | - | - |
| parent_code | varchar(10) | YES |  | - | - |
| nm_ko | varchar(100) | YES |  | - | - |
| nm_en | varchar(100) | YES |  | - | - |
| nm_zh | varchar(100) | YES |  | - | - |
| keyword | varchar(100) | YES |  | - | - |
| ord | int(11) | YES |  | - | - |
| note | text | YES |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): code

---

## tb_complaint

**설명**: 신고 (대리점에서 고객 신고)

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| complaint_no | int(11) | NO | PRI | - | auto_increment |
| apply_no | int(11) | NO |  | - | - |
| store_no | int(11) | NO |  | - | - |
| nm | blob | NO |  | - | - |
| tel_no | blob | NO |  | - | - |
| contents | text | YES |  | - | - |
| note | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): complaint_no

---

## tb_event_phone

**설명**: 이벤트-휴대폰

**데이터 건수**: 9건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| event_no | int(11) | NO | PRI | - | auto_increment |
| event_nm | varchar(100) | YES |  | - | - |
| button_yn | varchar(1) | NO |  | Y | - |
| button_nm | varchar(255) | YES |  | - | - |
| event_link | text | YES |  | - | - |
| thumb_img_location | text | YES |  | - | - |
| thumb_img_nm | varchar(60) | YES |  | - | - |
| detail_type_code | varchar(10) | NO |  | 0106001 | - |
| detail_img_location | text | YES |  | - | - |
| detail_img_nm | varchar(60) | YES |  | - | - |
| detail_html | text | YES |  | - | - |
| opened_at | timestamp | NO |  | current_timestamp() | - |
| closed_at | timestamp | YES |  | - | - |
| note | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): event_no

---

## tb_freeboard

**설명**: 자유게시판

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| freeboard_no | int(11) | NO | PRI | - | auto_increment |
| user_no | int(11) | NO | MUL | - | - |
| title | varchar(255) | YES |  | - | - |
| content | text | YES |  | - | - |
| total_rating | decimal(3,1) | NO |  | 0.0 | - |
| total_favorite | int(11) | NO |  | 0 | - |
| total_complaint | int(11) | NO |  | 0 | - |
| total_comment | int(11) | NO |  | 0 | - |
| total_view | int(11) | NO |  | 0 | - |
| state_code | varchar(10) | YES |  | 0206001 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): freeboard_no
- **idx_freeboard_main** (INDEX): deleted_yn, freeboard_no
- **idx_freeboard_user** (INDEX): user_no, deleted_yn, freeboard_no

---

## tb_freeboard_comment

**설명**: 자유게시판 댓글

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| comment_no | int(11) | NO | PRI | - | auto_increment |
| freeboard_no | int(11) | NO |  | - | - |
| user_no | int(11) | NO |  | - | - |
| parent_comment_no | int(11) | YES |  | - | - |
| tagged_user_no | int(11) | YES |  | - | - |
| content | text | YES |  | - | - |
| total_favorite | int(11) | NO |  | 0 | - |
| total_complaint | int(11) | NO |  | 0 | - |
| total_reply | int(11) | NO |  | 0 | - |
| state_code | varchar(10) | YES |  | 0206001 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): comment_no

---

## tb_freeboard_comment_complaint

**설명**: 자유게시판 댓글 신고

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| complaint_no | int(11) | NO | PRI | - | auto_increment |
| comment_no | int(11) | NO | MUL | - | - |
| user_no | int(11) | NO | MUL | - | - |
| reason | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): complaint_no
- **uk_comment_user** (UNIQUE): comment_no, user_no, deleted_yn
- **idx_comment_no** (INDEX): comment_no
- **idx_user_no** (INDEX): user_no
- **idx_deleted_yn** (INDEX): deleted_yn

---

## tb_freeboard_comment_favorite

**설명**: 자유게시판 댓글 좋아요

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| favorite_no | int(11) | NO | PRI | - | auto_increment |
| comment_no | int(11) | NO | MUL | - | - |
| user_no | int(11) | NO | MUL | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): favorite_no
- **uk_comment_user** (UNIQUE): comment_no, user_no, deleted_yn
- **idx_comment_no** (INDEX): comment_no
- **idx_user_no** (INDEX): user_no
- **idx_deleted_yn** (INDEX): deleted_yn

---

## tb_freeboard_complaint

**설명**: 자유게시판 신고

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| complaint_no | int(11) | NO | PRI | - | auto_increment |
| freeboard_no | int(11) | NO | MUL | - | - |
| user_no | int(11) | NO | MUL | - | - |
| reason | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): complaint_no
- **uk_freeboard_user** (UNIQUE): freeboard_no, user_no, deleted_yn
- **idx_freeboard_no** (INDEX): freeboard_no
- **idx_user_no** (INDEX): user_no
- **idx_deleted_yn** (INDEX): deleted_yn

---

## tb_freeboard_favorite

**설명**: 자유게시판 좋아요

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| favorite_no | int(11) | NO | PRI | - | auto_increment |
| freeboard_no | int(11) | NO | MUL | - | - |
| user_no | int(11) | NO | MUL | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): favorite_no
- **uk_freeboard_user** (UNIQUE): freeboard_no, user_no, deleted_yn
- **idx_freeboard_no** (INDEX): freeboard_no
- **idx_user_no** (INDEX): user_no
- **idx_deleted_yn** (INDEX): deleted_yn

---

## tb_freeboard_image

**설명**: 자유게시판 이미지

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| image_no | int(11) | NO | PRI | - | auto_increment |
| freeboard_no | int(11) | NO |  | - | - |
| img_location | text | YES |  | - | - |
| img_nm | varchar(60) | YES |  | - | - |
| ord | int(11) | NO |  | 0 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): image_no

---

## tb_payment

**설명**: 결제

**데이터 건수**: 3건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| payment_no | int(11) | NO | PRI | - | auto_increment |
| store_no | int(11) | NO |  | - | - |
| product_no | int(11) | NO |  | - | - |
| note | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| created_agency_no | int(11) | YES |  | - | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| modified_agency_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |
| deleted_reason | text | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): payment_no

---

## tb_payment_product

**설명**: 결제 상품

**데이터 건수**: 5건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| product_no | int(11) | NO | PRI | - | auto_increment |
| nm | varchar(50) | YES |  | - | - |
| cash | int(11) | NO |  | 0 | - |
| price | int(11) | NO |  | 0 | - |
| note | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |
| deleted_reason | text | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): product_no

---

## tb_pricetable_cable

**설명**: 가격표-유선

**데이터 건수**: 39건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| pricetable_dt | date | NO | PRI | - | - |
| product_code | varchar(100) | NO | MUL | - | - |
| carrier_code | varchar(10) | NO | PRI | - | - |
| internet_speed_code | varchar(10) | NO | PRI | - | - |
| tv_product_code | varchar(10) | NO | PRI | - | - |
| carrier | varchar(255) | NO |  | - | - |
| internet_speed | varchar(255) | NO |  | - | - |
| tv_product | varchar(255) | NO |  | - | - |
| gift_price | int(11) | YES |  | - | - |
| basic_price | int(11) | YES |  | - | - |
| bundle_price | int(11) | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): pricetable_dt, carrier_code, internet_speed_code, tv_product_code
- **idx_pricetable_cable_product_code** (INDEX): product_code
- **idx_pricetable_cable_date** (INDEX): pricetable_dt

---

## tb_pricetable_cable_fail

**설명**: 가격표-유선 : 실패

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| pricetable_dt | date | NO |  | - | - |
| product_code | varchar(100) | NO |  | - | - |
| carrier_code | varchar(10) | NO |  | - | - |
| internet_speed_code | varchar(10) | NO |  | - | - |
| tv_product_code | varchar(10) | NO |  | - | - |
| carrier | varchar(255) | NO |  | - | - |
| internet_speed | varchar(255) | NO |  | - | - |
| tv_product | varchar(255) | NO |  | - | - |
| gift_price | int(11) | YES |  | - | - |
| basic_price | int(11) | YES |  | - | - |
| bundle_price | int(11) | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| error_message | text | YES |  | - | - |
| raw_data_json | text | YES |  | - | - |

---

## tb_pricetable_cable_hist

**설명**: 가격표-유선 : 이력

**데이터 건수**: 312건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| pricetable_dt | date | NO | PRI | - | - |
| product_code | varchar(100) | NO |  | - | - |
| carrier_code | varchar(10) | NO | PRI | - | - |
| internet_speed_code | varchar(10) | NO | PRI | - | - |
| tv_product_code | varchar(10) | NO | PRI | - | - |
| carrier | varchar(255) | NO |  | - | - |
| internet_speed | varchar(255) | NO |  | - | - |
| tv_product | varchar(255) | NO |  | - | - |
| gift_price | int(11) | YES |  | - | - |
| basic_price | int(11) | YES |  | - | - |
| bundle_price | int(11) | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): pricetable_dt, carrier_code, internet_speed_code, tv_product_code
- **idx_pricetable_cable_hist_date** (INDEX): pricetable_dt

---

## tb_pricetable_cable_temp

**설명**: 가격표(임시)-유선

**데이터 건수**: 39건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| pricetable_dt | date | NO |  | - | - |
| product_code | varchar(100) | NO |  | - | - |
| carrier_code | varchar(10) | NO |  | - | - |
| internet_speed_code | varchar(10) | NO |  | - | - |
| tv_product_code | varchar(10) | NO |  | - | - |
| carrier | varchar(255) | NO |  | - | - |
| internet_speed | varchar(255) | NO |  | - | - |
| tv_product | varchar(255) | NO |  | - | - |
| gift_price | int(11) | YES |  | - | - |
| basic_price | int(11) | YES |  | - | - |
| bundle_price | int(11) | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |

---

## tb_pricetable_phone

**설명**: 가격표-휴대폰

**데이터 건수**: 4,759건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| pricetable_dt | date | NO | PRI | - | - |
| product_group_code | varchar(100) | NO | PRI | - | - |
| product_code | varchar(100) | NO | PRI | - | - |
| storage_code | varchar(10) | NO | PRI | - | - |
| carrier_code | varchar(10) | NO | PRI | - | - |
| rate_plan_code | varchar(10) | NO | PRI | - | - |
| join_type_code | varchar(10) | NO | PRI | - | - |
| support_type_code | varchar(10) | NO | PRI | - | - |
| carrier | varchar(50) | NO |  | - | - |
| manufacturer | varchar(50) | NO |  | - | - |
| device_nm | varchar(50) | NO |  | - | - |
| product_group_nm | varchar(50) | NO |  | - | - |
| storage | varchar(20) | NO |  | - | - |
| dealer | varchar(50) | NO |  | - | - |
| join_type | varchar(10) | NO |  | - | - |
| support_type | varchar(10) | NO |  | - | - |
| rate_plan | varchar(50) | NO |  | - | - |
| rate_plan_month_fee | int(11) | NO |  | - | - |
| retail_price | int(11) | NO |  | - | - |
| total_support_fee | int(11) | NO |  | - | - |
| dealer_subsidy | int(11) | NO |  | - | - |
| origin_installment_principal | int(11) | NO |  | - | - |
| origin_month_device_price | int(11) | NO |  | - | - |
| origin_month_price | int(11) | NO |  | - | - |
| month_rate_plan_price | int(11) | NO |  | - | - |
| installment_principal | int(11) | NO |  | - | - |
| month_device_price | int(11) | NO |  | - | - |
| month_price | int(11) | NO |  | - | - |
| margin | decimal(6,2) | NO |  | - | - |
| margin_amount | int(11) | NO |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): pricetable_dt, product_group_code, product_code, storage_code, carrier_code, rate_plan_code, join_type_code, support_type_code
- **idx_pricetable_phone_product_code** (INDEX): product_code
- **idx_pricetable_phone_product_group** (INDEX): product_group_code
- **idx_pricetable_phone_date** (INDEX): pricetable_dt

---

## tb_pricetable_phone_fail

**설명**: 가격표-휴대폰 : 실패

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| pricetable_dt | date | YES |  | - | - |
| product_group_code | varchar(100) | YES |  | - | - |
| product_code | varchar(100) | YES |  | - | - |
| storage_code | varchar(10) | YES |  | - | - |
| carrier_code | varchar(10) | YES |  | - | - |
| rate_plan_code | varchar(10) | YES |  | - | - |
| join_type_code | varchar(10) | YES |  | - | - |
| support_type_code | varchar(10) | YES |  | - | - |
| carrier | varchar(50) | YES |  | - | - |
| manufacturer | varchar(50) | YES |  | - | - |
| device_nm | varchar(50) | YES |  | - | - |
| product_group_nm | varchar(50) | YES |  | - | - |
| storage | varchar(20) | YES |  | - | - |
| dealer | varchar(50) | YES |  | - | - |
| join_type | varchar(10) | YES |  | - | - |
| support_type | varchar(10) | YES |  | - | - |
| rate_plan | varchar(50) | YES |  | - | - |
| rate_plan_month_fee | int(11) | YES |  | - | - |
| retail_price | int(11) | YES |  | - | - |
| total_support_fee | int(11) | YES |  | - | - |
| dealer_subsidy | int(11) | YES |  | - | - |
| origin_installment_principal | int(11) | YES |  | - | - |
| origin_month_device_price | int(11) | YES |  | - | - |
| origin_month_price | int(11) | YES |  | - | - |
| month_rate_plan_price | int(11) | YES |  | - | - |
| installment_principal | int(11) | YES |  | - | - |
| month_device_price | int(11) | YES |  | - | - |
| month_price | int(11) | YES |  | - | - |
| margin | decimal(6,2) | YES |  | - | - |
| margin_amount | int(11) | YES |  | - | - |
| created_at | timestamp | YES |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| error_message | text | YES |  | - | - |
| raw_data_json | text | YES |  | - | - |

---

## tb_pricetable_phone_hist

**설명**: 가격표-휴대폰 : 이력

**데이터 건수**: 180,690건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| pricetable_dt | date | NO | PRI | - | - |
| product_group_code | varchar(100) | NO | PRI | - | - |
| product_code | varchar(100) | NO | PRI | - | - |
| storage_code | varchar(10) | NO | PRI | - | - |
| carrier_code | varchar(10) | NO | PRI | - | - |
| rate_plan_code | varchar(10) | NO | PRI | - | - |
| join_type_code | varchar(10) | NO | PRI | - | - |
| support_type_code | varchar(10) | NO | PRI | - | - |
| carrier | varchar(50) | NO |  | - | - |
| manufacturer | varchar(50) | NO |  | - | - |
| device_nm | varchar(50) | NO |  | - | - |
| product_group_nm | varchar(50) | NO |  | - | - |
| storage | varchar(20) | NO |  | - | - |
| dealer | varchar(50) | NO |  | - | - |
| join_type | varchar(10) | NO |  | - | - |
| support_type | varchar(10) | NO |  | - | - |
| rate_plan | varchar(50) | NO |  | - | - |
| rate_plan_month_fee | int(11) | NO |  | - | - |
| retail_price | int(11) | NO |  | - | - |
| total_support_fee | int(11) | NO |  | - | - |
| dealer_subsidy | int(11) | NO |  | - | - |
| origin_installment_principal | int(11) | NO |  | - | - |
| origin_month_device_price | int(11) | NO |  | - | - |
| origin_month_price | int(11) | NO |  | - | - |
| month_rate_plan_price | int(11) | NO |  | - | - |
| installment_principal | int(11) | NO |  | - | - |
| month_device_price | int(11) | NO |  | - | - |
| month_price | int(11) | NO |  | - | - |
| margin | decimal(6,2) | NO |  | - | - |
| margin_amount | int(11) | NO |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): pricetable_dt, product_group_code, product_code, storage_code, carrier_code, rate_plan_code, join_type_code, support_type_code
- **idx_pricetable_phone_hist_optimized** (INDEX): product_group_code, pricetable_dt, product_code

---

## tb_pricetable_phone_temp

**설명**: 가격표(임시)-휴대폰

**데이터 건수**: 4,797건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| pricetable_dt | date | NO |  | - | - |
| product_group_code | varchar(100) | NO |  | - | - |
| product_code | varchar(100) | NO |  | - | - |
| storage_code | varchar(10) | NO |  | - | - |
| carrier_code | varchar(10) | NO |  | - | - |
| rate_plan_code | varchar(10) | NO |  | - | - |
| join_type_code | varchar(10) | NO |  | - | - |
| support_type_code | varchar(10) | NO |  | - | - |
| carrier | varchar(50) | NO |  | - | - |
| manufacturer | varchar(50) | NO |  | - | - |
| device_nm | varchar(50) | NO |  | - | - |
| product_group_nm | varchar(50) | NO |  | - | - |
| storage | varchar(20) | NO |  | - | - |
| dealer | varchar(50) | NO |  | - | - |
| join_type | varchar(10) | NO |  | - | - |
| support_type | varchar(10) | NO |  | - | - |
| rate_plan | varchar(50) | NO |  | - | - |
| rate_plan_month_fee | int(11) | NO |  | - | - |
| retail_price | int(11) | NO |  | - | - |
| total_support_fee | int(11) | NO |  | - | - |
| dealer_subsidy | int(11) | NO |  | - | - |
| origin_installment_principal | int(11) | NO |  | - | - |
| origin_month_device_price | int(11) | NO |  | - | - |
| origin_month_price | int(11) | NO |  | - | - |
| month_rate_plan_price | int(11) | NO |  | - | - |
| installment_principal | int(11) | NO |  | - | - |
| month_device_price | int(11) | NO |  | - | - |
| month_price | int(11) | NO |  | - | - |
| margin | decimal(6,2) | NO |  | - | - |
| margin_amount | int(11) | NO |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |

---

## tb_pricetable_store_phone_col

**설명**: 판매점 휴대폰 시세표 (Col 형태)

**데이터 건수**: 134건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| pricetable_dt | date | NO | PRI | - | - |
| store_no | int(11) | NO | PRI | - | - |
| product_group_code | varchar(100) | NO | PRI | - | - |
| product_code | varchar(100) | NO | PRI | - | - |
| rate_plan_code | varchar(10) | NO | PRI | - | - |
| skt_common_mnp | int(11) | YES |  | - | - |
| skt_common_chg | int(11) | YES |  | - | - |
| skt_common_new | int(11) | YES |  | - | - |
| skt_select_mnp | int(11) | YES |  | - | - |
| skt_select_chg | int(11) | YES |  | - | - |
| skt_select_new | int(11) | YES |  | - | - |
| kt_common_mnp | int(11) | YES |  | - | - |
| kt_common_chg | int(11) | YES |  | - | - |
| kt_common_new | int(11) | YES |  | - | - |
| kt_select_mnp | int(11) | YES |  | - | - |
| kt_select_chg | int(11) | YES |  | - | - |
| kt_select_new | int(11) | YES |  | - | - |
| lg_common_mnp | int(11) | YES |  | - | - |
| lg_common_chg | int(11) | YES |  | - | - |
| lg_common_new | int(11) | YES |  | - | - |
| lg_select_mnp | int(11) | YES |  | - | - |
| lg_select_chg | int(11) | YES |  | - | - |
| lg_select_new | int(11) | YES |  | - | - |
| created_at | timestamp | YES |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): pricetable_dt, store_no, product_group_code, product_code, rate_plan_code
- **idx_pricetable_dt** (INDEX): pricetable_dt
- **idx_store_no** (INDEX): store_no

---

## tb_pricetable_store_phone_row

**설명**: 판매점 휴대폰 시세표 (Row 형태)

**데이터 건수**: 520건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| pricetable_dt | date | NO | PRI | - | - |
| store_no | int(11) | NO | PRI | - | - |
| product_group_code | varchar(100) | NO | PRI | - | - |
| product_code | varchar(100) | NO | PRI | - | - |
| rate_plan_code | varchar(10) | NO | PRI | - | - |
| carrier_code | varchar(10) | NO | PRI | - | - |
| support_type_code | varchar(10) | NO | PRI | - | - |
| join_type_code | varchar(10) | NO | PRI | - | - |
| release_price | int(11) | YES |  | - | - |
| installment_principal | int(11) | NO |  | 0 | - |
| discount_price | int(11) | YES |  | - | - |
| discount_rate | int(11) | YES |  | - | - |
| month_rate_plan_fee | int(11) | NO |  | - | - |
| change_month_rate_plan_fee | int(11) | NO |  | - | - |
| month_price | int(11) | NO |  | - | - |
| rate_plan_maintain_days | int(11) | NO |  | - | - |
| month_avg_days | decimal(5,2) | YES |  | 30.44 | - |
| contract_months | decimal(10,2) | YES |  | - | - |
| rate_plan_maintain_month | int(11) | NO |  | - | - |
| remaining_months | decimal(10,2) | YES |  | - | - |
| optional_discount_rate | decimal(3,2) | YES |  | 0.00 | - |
| created_at | timestamp | YES |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): pricetable_dt, store_no, product_group_code, product_code, rate_plan_code, carrier_code, support_type_code, join_type_code
- **idx_pricetable_dt** (INDEX): pricetable_dt
- **idx_store_no** (INDEX): store_no
- **idx_carrier_code** (INDEX): carrier_code

---

## tb_product_cable

**설명**: 상품-유선

**데이터 건수**: 39건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| product_code | varchar(100) | NO | PRI | - | - |
| carrier_code | varchar(10) | NO | PRI | - | - |
| internet_speed_code | varchar(10) | NO | PRI | - | - |
| tv_product_code | varchar(10) | NO | PRI | - | - |
| ord | int(11) | NO |  | 0 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): product_code, carrier_code, internet_speed_code, tv_product_code

---

## tb_product_group_phone

**설명**: 상품 그룹-휴대폰

**데이터 건수**: 78건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| product_group_code | varchar(100) | NO | PRI | - | - |
| product_group_nm | varchar(60) | NO |  | - | - |
| thumb_img_location | text | YES |  | - | - |
| thumb_img_nm | varchar(60) | YES |  | - | - |
| detail_img_location | text | YES |  | - | - |
| detail_img_nm | varchar(60) | YES |  | - | - |
| manufacturer_code | varchar(10) | NO |  | - | - |
| device_type_code | varchar(10) | NO |  | - | - |
| total_consult | int(11) | NO |  | 0 | - |
| total_rating | decimal(3,1) | NO |  | 0.0 | - |
| total_favorite | int(11) | NO |  | 0 | - |
| total_complaint | int(11) | NO |  | 0 | - |
| total_review | int(11) | NO |  | 0 | - |
| total_view | int(11) | NO |  | 0 | - |
| ord | int(11) | NO |  | 0 | - |
| state_code | varchar(10) | NO |  | 0204001 | - |
| note | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): product_group_code
- **idx_product_group_phone_deleted** (INDEX): deleted_yn, product_group_code

---

## tb_product_group_phone_color

**설명**: 상품 그룹-휴대폰 색상

**데이터 건수**: 218건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| color_no | int(11) | NO | PRI | - | auto_increment |
| product_group_code | varchar(100) | NO |  | - | - |
| color_nm | varchar(60) | YES |  | - | - |
| color | varchar(20) | YES |  | - | - |
| ord | int(11) | NO |  | 0 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): color_no
- **idx_product_group_phone_color_deleted** (INDEX): deleted_yn, product_group_code, color_no

---

## tb_product_phone

**설명**: 상품-휴대폰

**데이터 건수**: 159건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| product_code | varchar(100) | NO | PRI | - | - |
| product_group_code | varchar(100) | NO | MUL | - | - |
| storage_code | varchar(10) | NO |  | - | - |
| retail_price | int(11) | YES |  | - | - |
| ord | int(11) | NO |  | 0 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): product_code
- **idx_product_phone_deleted** (INDEX): deleted_yn, product_code
- **idx_product_phone_group** (INDEX): product_group_code, deleted_yn
- **idx_product_phone_group_storage** (INDEX): product_group_code, storage_code, deleted_yn

---

## tb_rate_plan

**설명**: 요금제

**데이터 건수**: 16건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| rate_plan_code | varchar(100) | NO | PRI | - | - |
| rate_plan_nm | varchar(100) | YES |  | - | - |
| description | text | YES |  | - | - |
| month_fee | int(11) | YES |  | - | - |
| note | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): rate_plan_code
- **idx_rate_plan_phone_deleted** (INDEX): deleted_yn, rate_plan_code

---

## tb_rate_plan_phone

**설명**: 요금제

**데이터 건수**: 42건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| rate_plan_code | varchar(100) | NO | PRI | - | - |
| rate_plan_nm | varchar(100) | YES |  | - | - |
| rate_plan_mapp_nm | varchar(100) | YES |  | - | - |
| description | text | YES |  | - | - |
| carrier_code | varchar(10) | NO |  | - | - |
| month_fee | int(11) | YES |  | - | - |
| ord | int(11) | NO |  | 0 | - |
| note | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): rate_plan_code
- **idx_rate_plan_phone_deleted** (INDEX): deleted_yn, rate_plan_code

---

## tb_review_campaign_phone

**설명**: 캠페인 휴대폰-후기

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| review_no | int(11) | NO | PRI | - | auto_increment |
| user_no | int(11) | YES |  | - | - |
| store_no | int(11) | YES |  | - | - |
| product_group_code | varchar(100) | YES |  | - | - |
| product_code | varchar(100) | YES |  | - | - |
| apply_no | int(11) | YES |  | 0 | - |
| content | text | YES |  | - | - |
| complaint_content | text | YES |  | - | - |
| image | int(11) | NO |  | 0 | - |
| view | int(11) | NO |  | 0 | - |
| favorite | int(11) | NO |  | 0 | - |
| complaint | int(11) | NO |  | 0 | - |
| rating | decimal(2,1) | NO |  | 0.0 | - |
| note | text | YES |  | - | - |
| clip_yn | varchar(1) | NO |  | N | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): review_no

---

## tb_review_campaign_phone_image

**설명**: 캠페인 휴대폰-후기 이미지

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| image_no | int(11) | NO | PRI | - | auto_increment |
| review_no | int(11) | NO |  | - | - |
| img_location | text | YES |  | - | - |
| img_nm | varchar(60) | YES |  | - | - |
| ord | int(11) | NO |  | 0 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): image_no

---

## tb_review_phone

**설명**: 상품 그룹-후기

**데이터 건수**: 1건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| review_no | int(11) | NO | PRI | - | auto_increment |
| user_no | int(11) | YES |  | - | - |
| product_group_code | varchar(100) | NO |  | - | - |
| product_code | varchar(100) | NO |  | - | - |
| apply_no | int(11) | NO |  | 0 | - |
| content | text | YES |  | - | - |
| complaint_content | text | YES |  | - | - |
| image | int(11) | NO |  | 0 | - |
| view | int(11) | NO |  | 0 | - |
| favorite | int(11) | NO |  | 0 | - |
| complaint | int(11) | NO |  | 0 | - |
| rating | decimal(2,1) | NO |  | 0.0 | - |
| note | text | YES |  | - | - |
| clip_yn | varchar(1) | NO |  | N | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): review_no

---

## tb_review_phone_image

**설명**: 상품 그룹-후기 이미지

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| image_no | int(11) | NO | PRI | - | auto_increment |
| review_no | int(11) | NO |  | - | - |
| img_location | text | YES |  | - | - |
| img_nm | varchar(60) | YES |  | - | - |
| ord | int(11) | NO |  | 0 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): image_no

---

## tb_review_store_phone

**설명**: 판매점 휴대폰-후기

**데이터 건수**: 15건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| review_no | int(11) | NO | PRI | - | auto_increment |
| user_no | int(11) | YES |  | - | - |
| store_no | int(11) | YES |  | - | - |
| product_group_code | varchar(100) | YES |  | - | - |
| product_code | varchar(100) | YES |  | - | - |
| apply_no | int(11) | YES |  | 0 | - |
| content | text | YES |  | - | - |
| complaint_content | text | YES |  | - | - |
| image | int(11) | NO |  | 0 | - |
| view | int(11) | NO |  | 0 | - |
| favorite | int(11) | NO |  | 0 | - |
| complaint | int(11) | NO |  | 0 | - |
| rating | decimal(2,1) | NO |  | 0.0 | - |
| note | text | YES |  | - | - |
| clip_yn | varchar(1) | NO |  | N | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): review_no

---

## tb_review_store_phone_image

**설명**: 판매점 휴대폰-후기 이미지

**데이터 건수**: 6건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| image_no | int(11) | NO | PRI | - | auto_increment |
| review_no | int(11) | NO |  | - | - |
| img_location | text | YES |  | - | - |
| img_nm | varchar(60) | YES |  | - | - |
| ord | int(11) | NO |  | 0 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): image_no

---

## tb_review_store_phone_virtual

**설명**: 판매점 휴대폰-후기 (가상)

**데이터 건수**: 243건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| review_no | int(11) | NO | PRI | - | auto_increment |
| user_nm | varchar(100) | YES |  | - | - |
| store_no | int(11) | YES |  | - | - |
| product_group_code | varchar(100) | YES |  | - | - |
| product_code | varchar(100) | YES |  | - | - |
| apply_no | int(11) | YES |  | 0 | - |
| content | text | YES |  | - | - |
| complaint_content | text | YES |  | - | - |
| image | int(11) | NO |  | 0 | - |
| view | int(11) | NO |  | 0 | - |
| favorite | int(11) | NO |  | 0 | - |
| complaint | int(11) | NO |  | 0 | - |
| rating | decimal(2,1) | NO |  | 0.0 | - |
| note | text | YES |  | - | - |
| clip_yn | varchar(1) | NO |  | N | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): review_no

---

## tb_review_store_phone_virtual_image

**설명**: 판매점 휴대폰-후기 (가상) 이미지

**데이터 건수**: 12건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| image_no | int(11) | NO | PRI | - | auto_increment |
| review_no | int(11) | NO |  | - | - |
| img_location | text | YES |  | - | - |
| img_nm | varchar(60) | YES |  | - | - |
| ord | int(11) | NO |  | 0 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): image_no

---

## tb_review_virtual

**데이터 건수**: 1,398건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| review_virtual_no | int(11) | NO | PRI | - | auto_increment |
| product_group_code | varchar(100) | NO |  | - | - |
| product_code | varchar(100) | YES |  | - | - |
| user_name | varchar(50) | YES |  | - | - |
| join_type_name | varchar(20) | YES |  | - | - |
| carrier_name | varchar(10) | YES |  | - | - |
| sido_name | varchar(20) | YES |  | - | - |
| sigungu_name | varchar(30) | YES |  | - | - |
| content | text | NO |  | - | - |
| source_url | text | YES |  | - | - |
| source_site_name | varchar(100) | YES |  | - | - |
| source_site_logo | varchar(255) | YES |  | - | - |
| source_author | varchar(100) | YES |  | - | - |
| source_date | date | YES |  | - | - |
| image_urls | text | YES |  | - | - |
| rating | decimal(2,1) | NO |  | 5.0 | - |
| analyzed_rating | decimal(2,1) | YES |  | - | - |
| collected_at | timestamp | YES |  | current_timestamp() | - |
| view | int(11) | NO |  | 0 | - |
| favorite | int(11) | NO |  | 0 | - |
| image_count | int(11) | NO |  | 0 | - |
| display_order | int(11) | NO |  | 0 | - |
| is_active | varchar(1) | NO |  | Y | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| review_date | date | NO |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): review_virtual_no

---

## tb_store

**설명**: 판매점

**데이터 건수**: 54건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| store_no | int(11) | NO | PRI | - | auto_increment |
| nickname | varchar(100) | YES |  | - | - |
| store_nm | blob | YES |  | - | - |
| bizr_no | blob | YES |  | - | - |
| bizr_no_file_location | text | YES |  | - | - |
| bizr_no_file_nm | varchar(60) | YES |  | - | - |
| bizr_no_cert_yn | varchar(1) | YES |  | N | - |
| bizr_no_cert_at | timestamp | YES |  | - | - |
| presale_consent_link | text | YES |  | - | - |
| presale_consent_cert_yn | varchar(1) | YES |  | N | - |
| presale_consent_cert_at | timestamp | YES |  | - | - |
| representative_cert_file_location | text | YES |  | - | - |
| representative_cert_file_nm | varchar(60) | YES |  | - | - |
| representative_cert_yn | varchar(1) | YES |  | N | - |
| representative_cert_at | timestamp | YES |  | - | - |
| representative | blob | YES |  | - | - |
| tel_no | blob | YES |  | - | - |
| email | blob | YES |  | - | - |
| address | blob | YES |  | - | - |
| address_detail | blob | YES |  | - | - |
| longitude | decimal(11,8) | YES |  | - | - |
| latitude | decimal(11,8) | YES |  | - | - |
| sido_no | int(11) | YES |  | - | - |
| sigungu_no | int(11) | YES |  | - | - |
| business_week | int(3) | YES |  | - | - |
| business_time_start | varchar(5) | YES |  | - | - |
| business_time_end | varchar(5) | YES |  | - | - |
| thumb_img_location | text | YES |  | - | - |
| thumb_img_nm | varchar(60) | YES |  | - | - |
| thread | text | YES |  | - | - |
| pricetable_note | text | YES |  | - | - |
| view | int(11) | NO |  | 0 | - |
| counsel | int(11) | NO | MUL | 0 | - |
| review | int(11) | NO | MUL | 0 | - |
| review_avg | decimal(2,1) | YES |  | 0.0 | - |
| favorite | int(11) | NO |  | 0 | - |
| complaint | int(11) | NO |  | 0 | - |
| note | text | YES |  | - | - |
| last_updated_at | timestamp | YES | MUL | - | - |
| pricetable_exposure_yn | varchar(1) | NO |  | Y | - |
| step_code | varchar(10) | YES |  | 0202001 | - |
| cash | int(11) | NO |  | 0 | - |
| free_dt | date | YES |  | - | - |
| purchase_cash | int(11) | NO |  | 30000 | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_no | int(11) | YES |  | - | - |
| created_agency_no | int(11) | YES |  | - | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| modified_agency_no | int(11) | YES |  | - | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |
| deleted_reason | text | YES |  | - | - |
| master_yn | varchar(1) | NO |  | N | - |

### 인덱스

- **PRIMARY** (UNIQUE): store_no
- **tb_store_ord_idx_1** (INDEX): last_updated_at, review, counsel
- **tb_store_idx_2** (INDEX): last_updated_at
- **tb_store_idx_3** (INDEX): review
- **tb_store_idx_4** (INDEX): counsel
- **tb_store_idx_1** (INDEX): deleted_yn, sido_no, nickname

---

## tb_store_complaint

**설명**: 판매점 신고

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| complaint_no | int(11) | NO | PRI | - | auto_increment |
| store_no | int(11) | NO | MUL | - | - |
| user_no | int(11) | NO | MUL | - | - |
| reason | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |

### 인덱스

- **PRIMARY** (UNIQUE): complaint_no
- **uk_comment_user** (UNIQUE): store_no, user_no
- **idx_store_no** (INDEX): store_no
- **idx_user_no** (INDEX): user_no

---

## tb_store_favorite

**설명**: 판매점 좋아요

**데이터 건수**: 0건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| favorite_no | int(11) | NO | PRI | - | auto_increment |
| store_no | int(11) | NO | MUL | - | - |
| user_no | int(11) | NO | MUL | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |

### 인덱스

- **PRIMARY** (UNIQUE): favorite_no
- **uk_comment_user** (UNIQUE): store_no, user_no
- **idx_store_no** (INDEX): store_no
- **idx_user_no** (INDEX): user_no

---

## tb_store_purchase

**설명**: 판매점-고객 DB 구매

**데이터 건수**: 3,026건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| purchase_no | int(11) | NO | PRI | - | auto_increment |
| store_no | int(11) | NO |  | - | - |
| apply_no | int(11) | NO |  | - | - |
| cash_used | int(11) | YES |  | - | - |
| free_yn | varchar(1) | NO |  | N | - |
| refund_request_yn | varchar(1) | NO |  | N | - |
| refund_request_reason | text | YES |  | - | - |
| refund_reject_yn | varchar(1) | YES |  | - | - |
| refund_reject_reason | text | YES |  | - | - |
| note | text | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| created_agency_no | int(11) | NO |  | - | - |
| modified_at | timestamp | NO |  | current_timestamp() | - |
| modified_no | int(11) | YES |  | - | - |
| modified_agency_no | int(11) | NO |  | - | - |
| deleted_yn | varchar(1) | NO |  | N | - |
| deleted_at | timestamp | YES |  | - | - |
| deleted_reason | text | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): purchase_no

---

## tb_user

**설명**: 회원

**데이터 건수**: 5,236건

### 컬럼 구조

| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |
|--------|------|------|-----|--------|-------|
| user_no | int(11) | NO | PRI | - | auto_increment |
| user_id | blob | NO |  | - | - |
| create_type_code | varchar(10) | YES |  | - | - |
| email | varchar(100) | YES |  | - | - |
| user_nm | blob | YES |  | - | - |
| gender_code | varchar(10) | YES |  | - | - |
| birthday | blob | YES |  | - | - |
| tel_no | blob | YES |  | - | - |
| age_confirmed_yn | char(1) | YES |  | N | - |
| age_confirmed_at | datetime | YES |  | - | - |
| agreed_terms_of_service_yn | char(1) | YES |  | N | - |
| agreed_terms_of_service_at | datetime | YES |  | - | - |
| agreed_privacy_policy_yn | char(1) | YES |  | N | - |
| agreed_privacy_policy_at | datetime | YES |  | - | - |
| agreed_event_marketing_yn | char(1) | YES |  | N | - |
| agreed_event_marketing_at | datetime | YES |  | - | - |
| agreed_third_party_sharing_yn | char(1) | YES |  | N | - |
| agreed_third_party_sharing_at | datetime | YES |  | - | - |
| sido_no | int(11) | YES | MUL | 1 | - |
| sigungu_no | int(11) | YES |  | 1 | - |
| carrier_code | varchar(10) | YES |  | - | - |
| price_range_code | varchar(10) | YES |  | - | - |
| manufacturer_code | varchar(10) | YES |  | - | - |
| status_code | varchar(10) | YES |  | 0205001 | - |
| last_login_at | timestamp | YES |  | - | - |
| created_at | timestamp | NO |  | current_timestamp() | - |
| modified_at | timestamp | YES |  | current_timestamp() | - |
| deleted_yn | varchar(1) | NO | MUL | N | - |
| deleted_at | timestamp | YES |  | - | - |

### 인덱스

- **PRIMARY** (UNIQUE): user_no
- **idx_user_deleted** (INDEX): deleted_yn, user_no
- **idx_user_location** (INDEX): sido_no, sigungu_no, deleted_yn

---

