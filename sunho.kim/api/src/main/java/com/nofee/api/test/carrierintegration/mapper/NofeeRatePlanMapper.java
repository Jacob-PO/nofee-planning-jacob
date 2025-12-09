package com.nofee.api.test.carrierintegration.mapper;

import com.nofee.api.test.carrierintegration.dto.CarrierPlanPolicy;
import com.nofee.api.test.carrierintegration.dto.NofeeRatePlan;
import com.nofee.api.test.carrierintegration.dto.NofeeRatePlanSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 노피 DB 요금제 조회 Mapper
 */
@Mapper
public interface NofeeRatePlanMapper {

    /**
     * 통신사별 요금제 목록 조회
     * @param carrierCode 통신사 코드 (SKT, KT, LGU) - null이면 전체 조회
     */
    List<NofeeRatePlan> selectRatePlansByCarrier(@Param("carrierCode") String carrierCode);

    /**
     * 전체 요금제 목록 조회
     */
    List<NofeeRatePlan> selectAllRatePlans();

    /**
     * 판매중인 상품의 요금제 금액 목록 (중복 제거)
     */
    List<NofeeRatePlanSummary> selectDistinctMonthlyFees();

    /**
     * 통신사 기본 정책 조회
     */
    List<CarrierPlanPolicy> selectCarrierPlanPolicy();
}
