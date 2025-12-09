package com.nofee.api.test.carrierintegration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKT 요금제 DTO
 *
 * SKT API 응답 필드 (/api/wireless/subscription/list):
 * - subscriptionId: 요금제 ID (prodId로 사용)
 * - subscriptionNm: 요금제명
 * - basicCharge: 월 기본료
 * - dataOffer: 데이터 제공량
 * - callOffer: 통화 제공량
 * - smsOffer: 문자 제공량
 * - subcategoryId: 서브카테고리 (H=공시지원금 대상)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SktRatePlan {
    private String subscriptionId;      // 요금제 코드 (prodId로 사용)
    private String subscriptionNm;      // 요금제명
    private Integer basicCharge;        // 월 기본료
    private String dataOffer;           // 데이터 제공량
    private String callOffer;           // 통화 제공량
    private String smsOffer;            // 문자 제공량
    private String subcategoryId;       // 서브카테고리 (H=공시지원금 대상)
    private String categoryId;          // 카테고리 ID
    private String displayYn;           // 표시 여부
}
