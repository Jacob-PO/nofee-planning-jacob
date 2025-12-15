# Deal 페이지 가격 API 개선

## 목표

> **외부 API 의존성 제거**
>
> Deal 페이지 "가격 구성" 섹션이 외부 서버(Subapi) 장애 시 표시 안 되는 문제 해결

---

## 현재 문제점

### AS-IS 데이터 흐름

| Deal 페이지 | → | 내부 DB (안정적) | → | 할부원금, 월납부금, 요금제 |
| --- | --- | --- | --- | --- |
| Deal 페이지 | → | **외부 Subapi (불안정) ⚠️** | → | **출고가, 공시지원금, 추가지원금** |

> ⚠️ **문제**: 외부 Subapi 장애 시 "가격 구성" 섹션 표시 불가

---

## 해결 방안

### TO-BE 데이터 흐름

**1단계: 스케줄러가 통신사 데이터 수집 (1시간마다)**

| 통신사 서버 | → | 스케줄러 | → | 내부 DB |
| --- | --- | --- | --- | --- |
| SKT, KT, LGU+ | | 1시간 간격 자동 수집 | | tb_public_subsidy, tb_product_phone |

**2단계: Deal 페이지에서 내부 DB 조회**

| Deal 페이지 | → | 내부 DB (안정적) ✅ | → | 모든 데이터 |
| --- | --- | --- | --- | --- |

> ✅ **해결**: 스케줄러가 미리 수집한 데이터를 내부 DB에서 조회 → 안정적 서비스

---

## 신규 테이블 구조

### tb_carrier_rate_plan (통신사 요금제)

> 스케줄러가 통신사에서 수집한 요금제 정보

| 컬럼 | 설명 |
| --- | --- |
| plan_code | 통신사 요금제 코드 |
| carrier_code | 통신사 코드 |
| rate_plan_code | 노피 요금제 코드 (매핑) |
| plan_nm | 요금제명 |
| month_fee | 월정액 |

### tb_public_subsidy (공시지원금)

> 스케줄러가 통신사에서 수집한 공시지원금 정보

| 컬럼 | 설명 |
| --- | --- |
| carrier_code | 통신사 코드 |
| product_code | 상품 코드 |
| rate_plan_code | 요금제 코드 |
| join_type_code | 가입유형 코드 |
| public_subsidy | 공시지원금 |
| additional_subsidy | 추가지원금 (공시지원금의 15%) |

### tb_product_phone (출고가 추가)

> 기존 테이블에 통신사별 출고가 컬럼 추가

| 컬럼 | 설명 |
| --- | --- |
| sk_release_price | SKT 출고가 |
| kt_release_price | KT 출고가 |
| lg_release_price | LGU+ 출고가 |

---

## 현재 데이터 현황 (개발 서버)

| 테이블 | SKT | KT | LGU+ | 총 |
| --- | --- | --- | --- | --- |
| tb_carrier_rate_plan | 70건 | 92건 | 96건 | 258건 |
| tb_public_subsidy | 462건 | 864건 | 1,152건 | 2,478건 |
| tb_product_phone (출고가) | 85건 | 88건 | 77건 | - |

> 📌 최근 동기화: 2025-12-13 00:03:34 (1시간마다 실행)

---

## Deal 페이지 UI 매핑

### 가격 구성 섹션

| UI 항목 | 데이터 소스 | 조회 방법 |
| --- | --- | --- |
| ① 출고가 | tb_product_phone | carrier_code에 따라 sk/kt/lg_release_price |
| ② 공시지원금 | tb_public_subsidy | product_code + carrier_code + rate_plan_code + join_type_code |
| ③ 추가지원금 | tb_public_subsidy | 위와 동일 |
| ④ 판매점 할인 | 계산 | 출고가 - 공시지원금 - 추가지원금 - 실구매가 |
| ⑤ 실 구매가 | tb_pricetable_store_phone_row | 기존과 동일 |
| ⑥ 월요금 평균납부액 | tb_pricetable_store_phone_row | monthPrice (기존 필드)

---

## 에이전시 어드민 입력 항목

판매점이 시세표 업로드 시 입력:

| 구분 | 항목 | 설명 |
| --- | --- | --- |
| 공통 | 할부원금 | 판매점에서 제시하는 실제 할부원금 |
| 공시지원금 | 요금제 유지기간 | 공시지원금 개통 시 요금제 유지해야 하는 기간 (개월) |
| 공시지원금 | 변경가능 요금제 | 유지기간 후 변경 가능한 최소 요금제 |
| 선택약정 | 요금제 유지기간 | 선택약정 개통 시 요금제 유지해야 하는 기간 (개월) |
| 선택약정 | 변경가능 요금제 | 유지기간 후 변경 가능한 최소 요금제 |

> 💡 **참고**: 판매점마다 요금제 조건이 다를 수 있으므로, 각 판매점이 직접 입력한 값을 사용

---

## 작업 체크리스트

### 백엔드 - 스케줄러 (완료 ✅)

- [x] tb_carrier_rate_plan 테이블 생성
- [x] tb_public_subsidy 테이블 생성
- [x] tb_product_phone에 출고가 컬럼 추가
- [x] 1시간마다 통신사 데이터 수집 스케줄러 구현

### 백엔드 - API (진행 예정)

- [ ] Deal 페이지 API에서 tb_public_subsidy, tb_product_phone JOIN하여 조회
- [ ] API 응답에 releasePrice, publicSubsidy, additionalSubsidy 필드 추가 (monthPrice는 기존 필드 활용)

### 프론트엔드

- [ ] Deal 페이지에서 내부 DB 데이터 사용하도록 변경
- [ ] 외부 Subapi 호출 코드 제거

### 운영 배포

- [ ] 운영 서버에 tb_carrier_rate_plan, tb_public_subsidy 테이블 생성
- [ ] 운영 서버 스케줄러 배포

---

## 참고: 공통 코드

### 통신사

| 코드 | 값 |
| --- | --- |
| 0301001001 | SKT |
| 0301001002 | KT |
| 0301001003 | LGU+ |

### 가입유형

| 코드 | 값 |
| --- | --- |
| 0301007001 | 신규가입 |
| 0301007002 | 번호이동 |
| 0301007003 | 기기변경 |

### 지원유형

| 코드 | 값 |
| --- | --- |
| 0301006001 | 공시지원금 |
| 0301006002 | 선택약정 |
