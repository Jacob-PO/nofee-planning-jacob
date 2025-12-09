package com.nofee.api.test.carrierintegration.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 통합 공시지원금 DTO
 *
 * SKT, KT, LGU+ 공시지원금 데이터를 통합한 형태
 * 프론트엔드 형식에 맞춤
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrierSubsidy {

    /** 복합키 (carrier-joinType-deviceCode-planCode) */
    private String id;

    /** 통신사 코드 (SKT, KT, LGU+) */
    private String carrier;

    /** 가입유형 (신규, 기기변경, 번호이동) */
    private String joinType;

    /** 할인유형 (공시지원, 선택약정) */
    private String discountType;

    /** 지원유형 (공시지원금, 선택약정24 등) */
    private String supportType;

    /** 기기명 */
    private String deviceName;

    /** 기기 코드 */
    private String deviceCode;

    /** 저장용량 */
    private String storage;

    /** 색상 */
    private String color;

    /** 요금제명 */
    private String planName;

    /** 요금제 코드 */
    private String planCode;

    /** 월정액 (원) */
    private Integer planMonthlyFee;

    /** 약정기간 (개월) */
    @Builder.Default
    private Integer planMaintainMonth = 6;

    /** 출고가 (원) */
    private Integer msrp;

    /** 공시지원금 (원) */
    private Integer carrierSubsidy;

    /** 추가지원금 (원) */
    private Integer additionalSubsidy;

    /** 할부원금 (출고가 - 공시지원금 - 추가지원금) */
    private Integer installmentPrice;

    /** 공시일 (YYYY-MM-DD 형식) - 부분 업데이트의 기준 */
    private String announceDate;

    /** 캐시 시간 */
    private LocalDateTime cachedAt;

    /** 제조사 (내부용) */
    @JsonIgnore
    private String manufacturer;

    /** 원본 데이터 (디버그용) */
    @JsonIgnore
    private Object rawData;

    /**
     * ID 자동 생성
     */
    public String generateId() {
        return String.format("%s-%s-%s-%s",
            carrier != null ? carrier : "",
            joinType != null ? joinType : "",
            deviceCode != null ? deviceCode : "",
            planCode != null ? planCode : ""
        );
    }

    /**
     * 할부원금 계산 (msrp - carrierSubsidy - additionalSubsidy)
     */
    public Integer calculateInstallmentPrice() {
        if (msrp == null) return null;
        int subsidy = (carrierSubsidy != null ? carrierSubsidy : 0);
        int additional = (additionalSubsidy != null ? additionalSubsidy : 0);
        return msrp - subsidy - additional;
    }

    /**
     * 가입유형 코드를 한글로 변환
     */
    public static String convertJoinTypeToKorean(String code, String carrier) {
        if (code == null) return null;

        // SKT: 11=신규가입, 20=번호이동, 31=기기변경
        if ("SKT".equals(carrier)) {
            return switch (code) {
                case "11" -> "신규";
                case "20" -> "번호이동";
                case "31" -> "기기변경";
                default -> code;
            };
        }

        // KT
        if ("KT".equals(carrier)) {
            return switch (code) {
                case "01" -> "신규";
                case "02" -> "번호이동";
                case "04" -> "기기변경";
                default -> code;
            };
        }

        // LGU+
        if ("LGU".equals(carrier) || "LGU+".equals(carrier)) {
            return switch (code) {
                case "1" -> "기기변경";
                case "2" -> "번호이동";
                case "3" -> "신규";
                default -> code;
            };
        }

        return code;
    }
}
