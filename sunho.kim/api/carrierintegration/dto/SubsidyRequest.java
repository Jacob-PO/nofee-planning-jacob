package com.nofee.api.test.carrierintegration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공시지원금 조회 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubsidyRequest {

    /** 통신사 (SKT, KT, LGU, ALL) */
    private String carrier;

    /** 기기 코드 (통신사별 코드) */
    private String deviceCode;

    /** 가입유형 (신규: NEW, 기변: CHANGE, 번호이동: MNP) */
    private String joinType;

    /** 할인유형 (공시지원: SUBSIDY, 선택약정: DISCOUNT) */
    private String discountType;

    /** 요금제 코드 (선택, 없으면 전체 요금제 조회) */
    private String planCode;
}
