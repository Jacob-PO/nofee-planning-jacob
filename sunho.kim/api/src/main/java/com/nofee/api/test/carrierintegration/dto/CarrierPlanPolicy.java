package com.nofee.api.test.carrierintegration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 통신사 요금제 정책 DTO
 * tb_carrier_plan_phone 테이블 조회 결과
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrierPlanPolicy {
    private String carrierCode;             // 통신사 코드 (SKT, KT, LGU)
    private Integer minMonthFee;            // 최저 월정액 (원)
    private Integer basicMonthFee;          // 기본 월정액 (원)
    private Integer publicSupportDays;      // 공시지원금 약정기간 (개월)
    private Integer optionalContractDays;   // 선택약정 약정기간 (개월)
    private BigDecimal optionalDiscountRate; // 선택약정 할인율 (%)
}
