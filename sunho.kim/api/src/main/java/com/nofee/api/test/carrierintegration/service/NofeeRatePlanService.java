package com.nofee.api.test.carrierintegration.service;

import com.nofee.api.test.carrierintegration.dto.*;
import com.nofee.api.test.carrierintegration.mapper.NofeeRatePlanMapper;
import com.nofee.api.test.carrierintegration.util.CarrierCodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 노피 DB 요금제 조회 서비스
 *
 * 노피 DB에서 판매중인 요금제 목록을 조회하고 summary-plan 시트에 저장
 * - 임의 코드 생성 없음
 * - 노피 DB의 rate_plan_code를 그대로 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NofeeRatePlanService {

    private final NofeeRatePlanMapper ratePlanMapper;
    private final PlanSheetService planSheetService;

    /**
     * 노피 DB에서 전체 요금제 목록 조회
     */
    public List<NofeeRatePlan> getAllRatePlans() {
        return ratePlanMapper.selectAllRatePlans();
    }

    /**
     * 노피 DB에서 통신사별 요금제 목록 조회
     */
    public List<NofeeRatePlan> getRatePlansByCarrier(String carrierCode) {
        return ratePlanMapper.selectRatePlansByCarrier(carrierCode);
    }

    /**
     * 판매중인 요금제 금액 목록 (중복 제거)
     */
    public List<NofeeRatePlanSummary> getDistinctMonthlyFees() {
        return ratePlanMapper.selectDistinctMonthlyFees();
    }

    /**
     * 통신사 기본 정책 조회
     */
    public List<CarrierPlanPolicy> getCarrierPlanPolicy() {
        return ratePlanMapper.selectCarrierPlanPolicy();
    }

    /**
     * 노피 DB 요금제를 기반으로 summary-plan 시트 업데이트
     * - 임의 코드 생성 없음
     * - 노피 DB의 rate_plan_code를 planCode로 사용
     *
     * @return 업데이트된 요금제 수
     */
    public Map<String, Object> syncPlanSheetFromNofeeDb() {
        log.info("📡 노피 DB 요금제 -> summary-plan 시트 동기화 시작");

        List<NofeeRatePlan> nofeeRatePlans = getAllRatePlans();
        log.info("📊 노피 DB에서 {}개 요금제 조회됨", nofeeRatePlans.size());

        List<CarrierPlan> carrierPlans = new ArrayList<>();
        Map<String, Integer> carrierCounts = new HashMap<>();

        for (NofeeRatePlan nofeePlan : nofeeRatePlans) {
            String carrier = CarrierCodeUtils.normalize(nofeePlan.getCarrierCode());
            Integer monthFee = nofeePlan.getMonthFee();

            if (carrier == null || monthFee == null || monthFee <= 0) {
                continue;
            }

            // 노피 DB의 rate_plan_code를 그대로 사용 (임의 코드 생성 안함)
            String planCode = nofeePlan.getRatePlanCode();
            String planName = nofeePlan.getRatePlanNm();

            // 네트워크 타입 결정 (월정액 기준)
            String networkType = determineNetworkType(monthFee);

            CarrierPlan plan = CarrierPlan.builder()
                .carrier(carrier)
                .planCode(planCode)  // 노피 DB 코드 그대로 사용
                .planName(planName)  // 노피 DB 이름 그대로 사용
                .monthlyFee(monthFee)
                .networkType(networkType)
                .description(nofeePlan.getDescription())
                .active(true)
                .build();
            plan.setId(plan.generateId());

            carrierPlans.add(plan);
            carrierCounts.merge(carrier, 1, Integer::sum);
        }

        // summary-plan 시트에 저장
        planSheetService.savePlans(carrierPlans);

        log.info("✅ summary-plan 시트에 {}개 요금제 저장 완료", carrierPlans.size());

        return Map.of(
            "success", true,
            "totalCount", carrierPlans.size(),
            "byCarrier", carrierCounts,
            "message", "노피 DB 요금제를 summary-plan 시트에 동기화했습니다."
        );
    }

    /**
     * 네트워크 타입 결정 (월정액 기준)
     */
    private String determineNetworkType(Integer monthFee) {
        // 일반적으로 5만원 이상이면 5G, 미만이면 LTE
        return monthFee >= 50000 ? "5G" : "LTE";
    }

    /**
     * 요금제 정보 조회 (통신사별)
     */
    public Map<String, Object> getPlanMappingInfo() {
        List<NofeeRatePlan> nofeeRatePlans = getAllRatePlans();

        Map<String, List<Map<String, Object>>> byCarrier = new HashMap<>();

        for (NofeeRatePlan plan : nofeeRatePlans) {
            String carrier = CarrierCodeUtils.normalize(plan.getCarrierCode());
            if (carrier == null) continue;

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("ratePlanCode", plan.getRatePlanCode());
            info.put("ratePlanNm", plan.getRatePlanNm());
            info.put("monthFee", plan.getMonthFee());
            info.put("networkType", determineNetworkType(plan.getMonthFee()));
            info.put("description", plan.getDescription());

            byCarrier
                .computeIfAbsent(carrier, k -> new ArrayList<>())
                .add(info);
        }

        return Map.of(
            "source", "노피 DB (tb_rate_plan_phone)",
            "byCarrier", byCarrier
        );
    }
}
