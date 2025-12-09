package com.nofee.api.test.carrierintegration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LGU+ 요금제 DTO
 *
 * LGU+ API 응답 필드 (/uhdc/fo/prdv/mdlbsufu/v1/mdlb-pp-list):
 * - urcMblPpCd: 요금제 코드 (지원금 API에서 사용)
 * - urcMblPpNm: 요금제명
 * - urcPpBasfAmt: 기본 월 요금
 * - lastBasfAmt: 25% 선택약정 할인 후 요금
 * - mm24ChocAgmtDcntAmt: 24개월 선택약정 월 할인액
 * - mm24ChocAgmtDcntTamt: 24개월 선택약정 총 할인액
 * - mblMcnPpDataScrnEposDscr: 데이터 소진 시 안내
 * - nagmPpYn: 무약정 요금제 여부
 * - ppDirtDcntAplyPsblYn: 공시지원금 적용 가능 여부
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LguRatePlan {
    private String urcMblPpCd;              // 요금제 코드
    private String urcMblPpNm;              // 요금제명
    private Integer urcPpBasfAmt;           // 기본 월 요금
    private Integer lastBasfAmt;            // 선택약정 할인 후 요금
    private Integer mm24ChocAgmtDcntAmt;    // 24개월 선택약정 월 할인액
    private Integer mm24ChocAgmtDcntTamt;   // 24개월 선택약정 총 할인액
    private String mblMcnPpDataScrnEposDscr; // 데이터 소진 시 안내
    private Boolean nagmPpYn;               // 무약정 요금제 여부
    private Boolean ppDirtDcntAplyPsblYn;   // 공시지원금 적용 가능 여부
    private String urcTrmPpGrpKwrdCd;       // 기기 종류 코드 (00=5G, 01=LTE)
    private String trmPpGrpNm;              // 요금제 그룹명
}
