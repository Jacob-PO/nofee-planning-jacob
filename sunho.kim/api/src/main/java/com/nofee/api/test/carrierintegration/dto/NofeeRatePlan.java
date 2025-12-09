package com.nofee.api.test.carrierintegration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 노피 DB 요금제 정보 DTO
 * tb_rate_plan_phone 테이블 조회 결과
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NofeeRatePlan {
    private String ratePlanCode;    // 요금제 코드
    private String ratePlanNm;      // 요금제명
    private String description;     // 요금제 설명
    private String carrierCode;     // 통신사 코드 (SKT, KT, LGU)
    private Integer monthFee;       // 월정액 (원)
}
