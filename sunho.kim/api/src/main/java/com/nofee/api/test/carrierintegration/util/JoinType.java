package com.nofee.api.test.carrierintegration.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 가입유형 enum
 *
 * 노피 시스템, 통신사별 API에서 사용하는 가입유형 코드를 통합 관리합니다.
 *
 * 코드 매핑:
 * - 노피: 0301007001=신규, 0301007002=기기변경, 0301007003=번호이동
 * - SKT: 11=신규가입, 20=번호이동, 31=기기변경
 * - KT:  01=신규, 02=번호이동, 04=기기변경
 * - LGU: 3=신규, 2=번호이동, 1=기기변경
 */
@Getter
@RequiredArgsConstructor
public enum JoinType {

    NEW("신규", "0301007001", "11", "01", "3"),
    DEVICE_CHANGE("기기변경", "0301007002", "31", "04", "1"),
    NUMBER_PORTABILITY("번호이동", "0301007003", "20", "02", "2");

    private final String korean;
    private final String nofeeCode;
    private final String sktCode;
    private final String ktCode;
    private final String lguCode;

    /**
     * 기본값: 번호이동
     */
    public static final JoinType DEFAULT = NUMBER_PORTABILITY;

    /**
     * 어떤 형식이든 JoinType enum으로 변환
     *
     * 지원 입력:
     * - 노피 코드: "0301007001", "0301007002", "0301007003"
     * - 한글: "신규", "기기변경", "번호이동"
     * - SKT 코드: "11", "20", "31"
     * - KT 코드: "01", "02", "04"
     * - LGU 코드: "1", "2", "3"
     *
     * @param code 입력 코드
     * @return JoinType enum, 매칭 안되면 DEFAULT (번호이동)
     */
    public static JoinType from(String code) {
        if (code == null || code.isBlank()) {
            return DEFAULT;
        }

        String trimmed = code.trim();

        for (JoinType type : values()) {
            if (type.korean.equals(trimmed)
                || type.nofeeCode.equals(trimmed)
                || type.sktCode.equals(trimmed)
                || type.ktCode.equals(trimmed)
                || type.lguCode.equals(trimmed)) {
                return type;
            }
        }

        return DEFAULT;
    }

    /**
     * 입력값을 한글 가입유형으로 변환
     *
     * @param code 입력 코드 (노피코드, 통신사코드, 한글)
     * @return 한글 가입유형 ("신규", "기기변경", "번호이동")
     */
    public static String toKorean(String code) {
        return from(code).getKorean();
    }

    /**
     * 입력값을 노피 코드로 변환
     *
     * @param code 입력 코드
     * @return 노피 코드
     */
    public static String toNofeeCode(String code) {
        return from(code).getNofeeCode();
    }

    /**
     * 입력값을 SKT API 코드로 변환
     *
     * @param code 입력 코드
     * @return SKT 코드 (11, 20, 31)
     */
    public static String toSktCode(String code) {
        return from(code).getSktCode();
    }

    /**
     * 입력값을 KT API 코드로 변환
     *
     * @param code 입력 코드
     * @return KT 코드 (01, 02, 04)
     */
    public static String toKtCode(String code) {
        return from(code).getKtCode();
    }

    /**
     * 입력값을 LGU+ API 코드로 변환
     *
     * @param code 입력 코드
     * @return LGU 코드 (1, 2, 3)
     */
    public static String toLguCode(String code) {
        return from(code).getLguCode();
    }

    /**
     * 입력값을 특정 통신사 코드로 변환
     *
     * @param code 입력 코드
     * @param carrier 통신사 (SKT, KT, LGU)
     * @return 해당 통신사 API 코드
     */
    public static String toCarrierCode(String code, String carrier) {
        JoinType type = from(code);
        String normalized = CarrierCodeUtils.normalize(carrier);

        return switch (normalized) {
            case "SKT" -> type.getSktCode();
            case "KT" -> type.getKtCode();
            case "LGU" -> type.getLguCode();
            default -> type.getKorean();
        };
    }
}
