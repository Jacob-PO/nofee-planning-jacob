package com.nofee.api.test.carrierintegration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 요금제 금액 요약 DTO
 * 통신사별 월정액 목록 (중복 제거)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NofeeRatePlanSummary {
    private String carrierCode;     // 통신사 코드 (SKT, KT, LGU)
    private Integer monthFee;       // 월정액 (원)
}
