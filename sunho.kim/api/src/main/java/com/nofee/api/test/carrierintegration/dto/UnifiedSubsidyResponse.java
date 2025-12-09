package com.nofee.api.test.carrierintegration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 통합 공시지원금 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedSubsidyResponse {

    /** 조회 성공 여부 */
    private boolean success;

    /** 기기 코드 */
    private String deviceCode;

    /** 기기명 */
    private String deviceName;

    /** SKT 공시지원금 목록 */
    private List<CarrierSubsidy> sktSubsidies;

    /** KT 공시지원금 목록 */
    private List<CarrierSubsidy> ktSubsidies;

    /** LGU+ 공시지원금 목록 */
    private List<CarrierSubsidy> lguSubsidies;

    /** 오류 메시지 (있는 경우) */
    private String errorMessage;

    /** 조회 소요시간 (ms) */
    private Long elapsedMs;
}
