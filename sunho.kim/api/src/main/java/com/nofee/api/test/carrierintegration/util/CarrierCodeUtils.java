package com.nofee.api.test.carrierintegration.util;

/**
 * 통신사 코드 변환 유틸리티
 *
 * 노피 시스템에서 사용되는 통신사 코드와 각 통신사 API에서 사용되는 코드를 변환합니다.
 *
 * 코드 매핑:
 * - SKT: 0301001001
 * - KT:  0301001002
 * - LGU: 0301001003
 */
public final class CarrierCodeUtils {

    // 노피 시스템 통신사 코드
    public static final String NOFEE_CODE_SKT = "0301001001";
    public static final String NOFEE_CODE_KT = "0301001002";
    public static final String NOFEE_CODE_LGU = "0301001003";

    // 정규화된 통신사명
    public static final String CARRIER_SKT = "SKT";
    public static final String CARRIER_KT = "KT";
    public static final String CARRIER_LGU = "LGU";

    private CarrierCodeUtils() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }

    /**
     * 통신사 코드/이름을 정규화된 통신사명으로 변환
     *
     * 입력 가능 값:
     * - SKT: "SKT", "skt", "0301001001"
     * - KT: "KT", "kt", "0301001002"
     * - LGU: "LGU", "LGU+", "LG U+", "LGUPLUS", "lgu", "0301001003"
     *
     * @param carrier 통신사 코드 또는 이름
     * @return 정규화된 통신사명 (SKT, KT, LGU) 또는 입력값을 대문자로 변환한 값
     */
    public static String normalize(String carrier) {
        if (carrier == null || carrier.isBlank()) {
            return null;
        }

        String upperCarrier = carrier.toUpperCase().trim();

        return switch (upperCarrier) {
            case "SKT", "0301001001" -> CARRIER_SKT;
            case "KT", "0301001002" -> CARRIER_KT;
            case "LGU", "LGU+", "LG U+", "LGUPLUS", "0301001003" -> CARRIER_LGU;
            default -> upperCarrier;
        };
    }

    /**
     * 정규화된 통신사명을 노피 시스템 코드로 변환
     *
     * @param carrier 통신사명 (SKT, KT, LGU)
     * @return 노피 시스템 코드 (0301001001, 0301001002, 0301001003)
     */
    public static String toNofeeCode(String carrier) {
        if (carrier == null) {
            return null;
        }

        String normalized = normalize(carrier);
        return switch (normalized) {
            case CARRIER_SKT -> NOFEE_CODE_SKT;
            case CARRIER_KT -> NOFEE_CODE_KT;
            case CARRIER_LGU -> NOFEE_CODE_LGU;
            default -> null;
        };
    }

    /**
     * 노피 시스템 코드를 정규화된 통신사명으로 변환
     *
     * @param nofeeCode 노피 시스템 코드
     * @return 정규화된 통신사명 또는 null
     */
    public static String fromNofeeCode(String nofeeCode) {
        if (nofeeCode == null) {
            return null;
        }

        return switch (nofeeCode) {
            case NOFEE_CODE_SKT -> CARRIER_SKT;
            case NOFEE_CODE_KT -> CARRIER_KT;
            case NOFEE_CODE_LGU -> CARRIER_LGU;
            default -> null;
        };
    }

    /**
     * 유효한 통신사 코드인지 확인
     *
     * @param carrier 통신사 코드 또는 이름
     * @return 유효한 통신사인 경우 true
     */
    public static boolean isValidCarrier(String carrier) {
        String normalized = normalize(carrier);
        return CARRIER_SKT.equals(normalized)
            || CARRIER_KT.equals(normalized)
            || CARRIER_LGU.equals(normalized);
    }
}
