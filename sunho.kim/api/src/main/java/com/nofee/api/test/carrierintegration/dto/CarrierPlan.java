package com.nofee.api.test.carrierintegration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 통합 요금제 DTO
 *
 * SKT, KT, LGU+ 요금제 정보를 통합한 형태
 * summary-plan 시트에 저장되는 데이터 구조
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrierPlan {

    /** 복합키 (carrier_planCode) */
    private String id;

    /** 통신사 코드 (SKT, KT, LGU) */
    private String carrier;

    /** 요금제 코드 (통신사 API에서 사용하는 코드) */
    private String planCode;

    /** 요금제명 */
    private String planName;

    /** 월정액 (원) */
    private Integer monthlyFee;

    /** 네트워크 유형 (5G, LTE) */
    private String networkType;

    /** 데이터 제공량 */
    private String dataAllowance;

    /** 음성통화 */
    private String voiceAllowance;

    /** 문자 */
    private String smsAllowance;

    /** 요금제 유형 (기본, 프리미엄, 시니어, 청소년 등) */
    private String planType;

    /** 요금제 설명 */
    private String description;

    /** 활성화 여부 (조회 대상 여부) */
    @Builder.Default
    private Boolean active = true;

    /** 등록일시 */
    private LocalDateTime createdAt;

    /** 수정일시 */
    private LocalDateTime updatedAt;

    /**
     * ID 자동 생성 (carrier_planCode)
     */
    public String generateId() {
        return String.format("%s_%s",
            carrier != null ? carrier : "",
            planCode != null ? planCode : "");
    }

    /**
     * 통신사별 가입유형 코드 목록 반환
     * @param carrier 통신사 (SKT, KT, LGU)
     * @return 가입유형 코드 배열
     */
    public static String[] getJoinTypeCodes(String carrier) {
        return switch (carrier) {
            case "SKT" -> new String[]{"10", "20", "30"}; // 신규, 번호이동, 기기변경
            case "KT" -> new String[]{"01", "02", "04"};  // 신규, 번호이동, 기기변경
            case "LGU" -> new String[]{"3", "2", "1"};    // 신규, 번호이동, 기기변경
            default -> new String[]{};
        };
    }

    /**
     * 가입유형 코드 -> 한글 변환
     */
    public static String getJoinTypeKorean(String carrier, String code) {
        return switch (carrier) {
            case "SKT" -> switch (code) {
                case "10" -> "신규";
                case "20" -> "번호이동";
                case "30" -> "기기변경";
                default -> code;
            };
            case "KT" -> switch (code) {
                case "01" -> "신규";
                case "02" -> "번호이동";
                case "04" -> "기기변경";
                default -> code;
            };
            case "LGU" -> switch (code) {
                case "3" -> "신규";
                case "2" -> "번호이동";
                case "1" -> "기기변경";
                default -> code;
            };
            default -> code;
        };
    }
}
