package com.nofee.api.test.carrierintegration.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 지원유형 enum
 *
 * 노피 시스템에서 사용하는 지원유형 코드를 통합 관리합니다.
 *
 * 코드 매핑:
 * - 노피: 0301006001=공시지원금, 0301006002=선택약정
 */
@Getter
@RequiredArgsConstructor
public enum SupportType {

    CARRIER_SUBSIDY("공시지원금", "0301006001"),
    OPTIONAL_CONTRACT("선택약정", "0301006002");

    private final String korean;
    private final String nofeeCode;

    /**
     * 기본값: 공시지원금
     */
    public static final SupportType DEFAULT = CARRIER_SUBSIDY;

    /**
     * 어떤 형식이든 SupportType enum으로 변환
     *
     * 지원 입력:
     * - 노피 코드: "0301006001", "0301006002"
     * - 한글: "공시지원금", "선택약정"
     *
     * @param code 입력 코드
     * @return SupportType enum, 매칭 안되면 DEFAULT (공시지원금)
     */
    public static SupportType from(String code) {
        if (code == null || code.isBlank()) {
            return DEFAULT;
        }

        String trimmed = code.trim();

        for (SupportType type : values()) {
            if (type.korean.equals(trimmed) || type.nofeeCode.equals(trimmed)) {
                return type;
            }
        }

        return DEFAULT;
    }

    /**
     * 입력값을 한글 지원유형으로 변환
     *
     * @param code 입력 코드 (노피코드, 한글)
     * @return 한글 지원유형 ("공시지원금", "선택약정")
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
     * 공시지원금 타입인지 확인
     */
    public boolean isCarrierSubsidy() {
        return this == CARRIER_SUBSIDY;
    }

    /**
     * 선택약정 타입인지 확인
     */
    public boolean isOptionalContract() {
        return this == OPTIONAL_CONTRACT;
    }
}
