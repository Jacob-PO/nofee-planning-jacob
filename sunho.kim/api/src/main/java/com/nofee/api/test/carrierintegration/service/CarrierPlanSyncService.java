package com.nofee.api.test.carrierintegration.service;

import com.nofee.api.test.carrierintegration.dto.CarrierPlan;
import com.nofee.api.test.carrierintegration.dto.KtRatePlan;
import com.nofee.api.test.carrierintegration.dto.SktRatePlan;
import com.nofee.api.test.carrierintegration.dto.LguRatePlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 통신사 API에서 요금제 목록을 가져와 summary-plan 시트에 동기화하는 서비스
 *
 * 플로우:
 * 1. 각 통신사 API에서 요금제 목록 조회
 * 2. CarrierPlan DTO로 변환
 * 3. summary-plan 시트에 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierPlanSyncService {

    private final KtSubsidyService ktSubsidyService;
    private final SktSubsidyService sktSubsidyService;
    private final LguSubsidyService lguSubsidyService;
    private final PlanSheetService planSheetService;

    /**
     * 모든 통신사 요금제를 API에서 조회하여 summary-plan 시트에 동기화
     */
    public Map<String, Object> syncAllCarrierPlans() {
        log.info("📡 전체 통신사 요금제 동기화 시작");
        long startTime = System.currentTimeMillis();

        List<CarrierPlan> allPlans = new ArrayList<>();
        Map<String, Integer> carrierCounts = new LinkedHashMap<>();

        // 1. KT 요금제 조회
        List<CarrierPlan> ktPlans = fetchKtPlans();
        allPlans.addAll(ktPlans);
        carrierCounts.put("KT", ktPlans.size());
        log.info("✅ KT 요금제 {}개 조회 완료", ktPlans.size());

        // 2. SKT 요금제 조회
        List<CarrierPlan> sktPlans = fetchSktPlans();
        allPlans.addAll(sktPlans);
        carrierCounts.put("SKT", sktPlans.size());
        log.info("✅ SKT 요금제 {}개 조회 완료", sktPlans.size());

        // 3. LGU 요금제 조회
        List<CarrierPlan> lguPlans = fetchLguPlans();
        allPlans.addAll(lguPlans);
        carrierCounts.put("LGU", lguPlans.size());
        log.info("✅ LGU 요금제 {}개 조회 완료", lguPlans.size());

        // 4. summary-plan 시트에 저장
        planSheetService.savePlans(allPlans);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("✅ 전체 통신사 요금제 동기화 완료: {}개 ({}ms)", allPlans.size(), elapsed);

        return Map.of(
            "success", true,
            "totalCount", allPlans.size(),
            "byCarrier", carrierCounts,
            "elapsedMs", elapsed,
            "message", "통신사 API에서 요금제를 조회하여 summary-plan 시트에 동기화했습니다."
        );
    }

    /**
     * KT 요금제 조회
     */
    public List<CarrierPlan> fetchKtPlans() {
        List<CarrierPlan> plans = new ArrayList<>();

        try {
            // 5G 요금제
            List<KtRatePlan> kt5gPlans = ktSubsidyService.fetchRatePlans("5G");
            for (KtRatePlan ktPlan : kt5gPlans) {
                if (ktPlan.getOnfrmCd() == null || ktPlan.getPunoMonthUseChage() == null) {
                    continue;
                }
                CarrierPlan plan = CarrierPlan.builder()
                    .carrier("KT")
                    .planCode(ktPlan.getOnfrmCd())
                    .planName(ktPlan.getPplNm())
                    .monthlyFee(ktPlan.getPunoMonthUseChage())
                    .networkType("5G")
                    .description(ktPlan.getDataBasic())
                    .active(true)
                    .build();
                plan.setId(plan.generateId());
                plans.add(plan);
            }

            // LTE 요금제
            List<KtRatePlan> ktLtePlans = ktSubsidyService.fetchRatePlans("LTE");
            for (KtRatePlan ktPlan : ktLtePlans) {
                if (ktPlan.getOnfrmCd() == null || ktPlan.getPunoMonthUseChage() == null) {
                    continue;
                }
                CarrierPlan plan = CarrierPlan.builder()
                    .carrier("KT")
                    .planCode(ktPlan.getOnfrmCd())
                    .planName(ktPlan.getPplNm())
                    .monthlyFee(ktPlan.getPunoMonthUseChage())
                    .networkType("LTE")
                    .description(ktPlan.getDataBasic())
                    .active(true)
                    .build();
                plan.setId(plan.generateId());
                plans.add(plan);
            }
        } catch (Exception e) {
            log.error("❌ KT 요금제 조회 실패: {}", e.getMessage());
        }

        return plans;
    }

    /**
     * SKT 요금제 조회
     * API: /api/wireless/subscription/list
     * - subcategoryId='H'인 요금제만 공시지원금 대상 (740개)
     */
    public List<CarrierPlan> fetchSktPlans() {
        List<CarrierPlan> plans = new ArrayList<>();

        try {
            // 5G 요금제
            List<SktRatePlan> skt5gPlans = sktSubsidyService.fetchRatePlans("5G");
            for (SktRatePlan sktPlan : skt5gPlans) {
                if (sktPlan.getSubscriptionId() == null || sktPlan.getBasicCharge() == null) {
                    continue;
                }
                CarrierPlan plan = CarrierPlan.builder()
                    .carrier("SKT")
                    .planCode(sktPlan.getSubscriptionId())
                    .planName(sktPlan.getSubscriptionNm())
                    .monthlyFee(sktPlan.getBasicCharge())
                    .networkType("5G")
                    .description(sktPlan.getDataOffer())
                    .active(true)
                    .build();
                plan.setId(plan.generateId());
                plans.add(plan);
            }

            // LTE 요금제
            List<SktRatePlan> sktLtePlans = sktSubsidyService.fetchRatePlans("LTE");
            for (SktRatePlan sktPlan : sktLtePlans) {
                if (sktPlan.getSubscriptionId() == null || sktPlan.getBasicCharge() == null) {
                    continue;
                }
                CarrierPlan plan = CarrierPlan.builder()
                    .carrier("SKT")
                    .planCode(sktPlan.getSubscriptionId())
                    .planName(sktPlan.getSubscriptionNm())
                    .monthlyFee(sktPlan.getBasicCharge())
                    .networkType("LTE")
                    .description(sktPlan.getDataOffer())
                    .active(true)
                    .build();
                plan.setId(plan.generateId());
                plans.add(plan);
            }
        } catch (Exception e) {
            log.error("❌ SKT 요금제 조회 실패: {}", e.getMessage());
        }

        return plans;
    }

    /**
     * LGU 요금제 조회
     * API: /uhdc/fo/prdv/mdlbsufu/v1/mdlb-pp-list
     * - 5G: 74개, LTE: 22개 요금제
     */
    public List<CarrierPlan> fetchLguPlans() {
        List<CarrierPlan> plans = new ArrayList<>();

        try {
            // 5G 요금제
            List<LguRatePlan> lgu5gPlans = lguSubsidyService.fetchRatePlans("5G");
            for (LguRatePlan lguPlan : lgu5gPlans) {
                if (lguPlan.getUrcMblPpCd() == null || lguPlan.getUrcPpBasfAmt() == null) {
                    continue;
                }
                CarrierPlan plan = CarrierPlan.builder()
                    .carrier("LGU")
                    .planCode(lguPlan.getUrcMblPpCd())
                    .planName(lguPlan.getUrcMblPpNm())
                    .monthlyFee(lguPlan.getUrcPpBasfAmt())
                    .networkType("5G")
                    .description(lguPlan.getMblMcnPpDataScrnEposDscr())
                    .active(true)
                    .build();
                plan.setId(plan.generateId());
                plans.add(plan);
            }

            // LTE 요금제
            List<LguRatePlan> lguLtePlans = lguSubsidyService.fetchRatePlans("LTE");
            for (LguRatePlan lguPlan : lguLtePlans) {
                if (lguPlan.getUrcMblPpCd() == null || lguPlan.getUrcPpBasfAmt() == null) {
                    continue;
                }
                CarrierPlan plan = CarrierPlan.builder()
                    .carrier("LGU")
                    .planCode(lguPlan.getUrcMblPpCd())
                    .planName(lguPlan.getUrcMblPpNm())
                    .monthlyFee(lguPlan.getUrcPpBasfAmt())
                    .networkType("LTE")
                    .description(lguPlan.getMblMcnPpDataScrnEposDscr())
                    .active(true)
                    .build();
                plan.setId(plan.generateId());
                plans.add(plan);
            }
        } catch (Exception e) {
            log.error("❌ LGU 요금제 조회 실패: {}", e.getMessage());
        }

        return plans;
    }

    /**
     * KT 요금제만 동기화
     */
    public Map<String, Object> syncKtPlans() {
        log.info("📡 KT 요금제 동기화 시작");
        long startTime = System.currentTimeMillis();

        List<CarrierPlan> ktPlans = fetchKtPlans();

        // 기존 요금제에서 KT만 제거하고 새로 추가
        List<CarrierPlan> existingPlans = planSheetService.getAllPlans();
        List<CarrierPlan> nonKtPlans = existingPlans.stream()
            .filter(p -> !"KT".equals(p.getCarrier()))
            .toList();

        List<CarrierPlan> allPlans = new ArrayList<>(nonKtPlans);
        allPlans.addAll(ktPlans);

        planSheetService.savePlans(allPlans);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("✅ KT 요금제 동기화 완료: {}개 ({}ms)", ktPlans.size(), elapsed);

        return Map.of(
            "success", true,
            "carrier", "KT",
            "count", ktPlans.size(),
            "elapsedMs", elapsed,
            "message", "KT API에서 요금제를 조회하여 summary-plan 시트에 동기화했습니다."
        );
    }
}
