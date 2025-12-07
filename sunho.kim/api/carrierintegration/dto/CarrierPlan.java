package com.nofee.api.test.carrierintegration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 통합 요금제 DTO
 *
 * SKT, KT, LGU+ 요금제 정보를 통합한 형태
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrierPlan {

    /** 통신사 코드 (SKT, KT, LGU) */
    private String carrier;

    /** 요금제 코드 */
    private String planCode;

    /** 요금제명 */
    private String planName;

    /** 월정액 (원) */
    private Integer monthlyFee;

    /** 데이터 제공량 */
    private String dataAllowance;

    /** 음성통화 */
    private String voiceAllowance;

    /** 문자 */
    private String smsAllowance;

    /** 요금제 유형 (5G, LTE 등) */
    private String planType;

    /** 요금제 설명 */
    private String description;
}
