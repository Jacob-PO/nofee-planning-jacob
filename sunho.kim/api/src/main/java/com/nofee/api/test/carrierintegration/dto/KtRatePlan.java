package com.nofee.api.test.carrierintegration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KT 요금제 DTO
 *
 * KT API 응답 필드:
 * - onfrmCd: 요금제 코드 (prdcCd로 사용)
 * - pplNm: 요금제명
 * - punoMonthUseChage: 월 요금
 * - punoMonthUseDcChage: 선택약정 월 할인액 (요금의 25%)
 * - pplGb: 요금제 구분 (5G, LTE)
 * - pplGrpCd: 요금제 그룹 코드
 * - pplId: 요금제 ID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KtRatePlan {
    private String onfrmCd;              // 요금제 코드 (API 조회용)
    private String pplNm;                // 요금제명
    private Integer punoMonthUseChage;   // 월 요금
    private Integer punoMonthUseDcChage; // 선택약정 월 할인액
    private String pplGb;                // 요금제 구분 (5G, LTE)
    private String pplGrpCd;             // 요금제 그룹 코드
    private String pplId;                // 요금제 ID
    private String dataBasic;            // 기본 데이터
    private String tlkBasic;             // 기본 통화
    private String charBasic;            // 기본 문자
}
