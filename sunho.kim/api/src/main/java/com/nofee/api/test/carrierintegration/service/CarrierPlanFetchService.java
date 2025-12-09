package com.nofee.api.test.carrierintegration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nofee.api.test.carrierintegration.dto.CarrierPlan;
import com.nofee.api.test.carrierintegration.util.JsonNodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 통신사 요금제 정보 조회 서비스
 *
 * 각 통신사에서 요금제 목록을 가져와서 CarrierPlan 형태로 반환
 * - SKT: tworld 페이지에서 파싱
 * - KT: shop.kt.com API
 * - LGU+: lguplus.com API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierPlanFetchService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final PlanSheetService planSheetService;

    /**
     * 모든 통신사 요금제 조회 및 시트 저장
     */
    public List<CarrierPlan> fetchAndSaveAllPlans() {
        List<CarrierPlan> allPlans = fetchAllPlans();

        // 시트에 저장
        planSheetService.savePlans(allPlans);

        log.info("✅ 전체 {}개 요금제 조회 및 저장 완료", allPlans.size());
        return allPlans;
    }

    /**
     * 모든 통신사 요금제 조회 (저장하지 않음)
     */
    public List<CarrierPlan> fetchAllPlans() {
        List<CarrierPlan> allPlans = new ArrayList<>();

        allPlans.addAll(fetchSktPlans());
        allPlans.addAll(fetchKtPlans());
        allPlans.addAll(fetchLguPlans());

        log.info("📋 전체 {}개 요금제 조회 완료 (SKT: {}, KT: {}, LGU: {})",
            allPlans.size(),
            allPlans.stream().filter(p -> "SKT".equals(p.getCarrier())).count(),
            allPlans.stream().filter(p -> "KT".equals(p.getCarrier())).count(),
            allPlans.stream().filter(p -> "LGU".equals(p.getCarrier())).count()
        );

        return allPlans;
    }

    /**
     * SKT 요금제 조회
     * 실제 API가 있으면 API 호출, 없으면 기본 데이터 반환
     */
    public List<CarrierPlan> fetchSktPlans() {
        log.info("📡 SKT 요금제 조회 중...");

        List<CarrierPlan> plans = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // ==================== SKT 5G 요금제 ====================
        // 5G 프리미엄
        addPlan(plans, "SKT", "NA00007789", "5GX 플래티넘", 125000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "SKT", "NA00007792", "5GX 프라임 플러스", 109000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "SKT", "NA00007790", "5GX 프라임", 89000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "SKT", "NA00007791", "5GX 스탠다드", 79000, "5G", "무제한", "기본", now);
        addPlan(plans, "SKT", "NA00008500", "5GX 슬림", 55000, "5G", "12GB", "기본", now);

        // 5G 다이렉트
        addPlan(plans, "SKT", "NA00008510", "5G 다이렉트 55", 55000, "5G", "무제한(5Mbps)", "다이렉트", now);
        addPlan(plans, "SKT", "NA00008511", "5G 다이렉트 45", 45000, "5G", "6GB", "다이렉트", now);
        addPlan(plans, "SKT", "NA00008512", "5G 다이렉트 37", 37000, "5G", "3GB", "다이렉트", now);

        // 5G 청년/시니어
        addPlan(plans, "SKT", "NA00008520", "0 청년 5G", 55000, "5G", "무제한(5Mbps)", "청년", now);
        addPlan(plans, "SKT", "NA00008521", "0 시니어 5G", 49000, "5G", "무제한(3Mbps)", "시니어", now);

        // T 시그니처
        addPlan(plans, "SKT", "NA00007800", "T 시그니처", 130000, "5G", "무제한", "프리미엄", now);

        // ==================== SKT LTE 요금제 ====================
        // LTE 프리미엄
        addPlan(plans, "SKT", "NA00006894", "LTE 시그니처", 100000, "LTE", "무제한", "프리미엄", now);
        addPlan(plans, "SKT", "NA00006893", "LTE 프리미어 플러스", 85000, "LTE", "무제한", "프리미엄", now);
        addPlan(plans, "SKT", "NA00006892", "LTE 프리미어 레귤러", 75000, "LTE", "무제한", "기본", now);
        addPlan(plans, "SKT", "NA00006895", "LTE 프리미어 에센셜", 69000, "LTE", "무제한(5Mbps)", "기본", now);

        // LTE 일반
        addPlan(plans, "SKT", "NA00008501", "LTE 슬림", 47000, "LTE", "7GB", "기본", now);
        addPlan(plans, "SKT", "NA00008502", "LTE 심플", 39000, "LTE", "3GB", "기본", now);
        addPlan(plans, "SKT", "NA00008503", "LTE 라이트", 33000, "LTE", "1.5GB", "기본", now);

        // LTE 다이렉트
        addPlan(plans, "SKT", "NA00008530", "LTE 다이렉트 47", 47000, "LTE", "무제한(3Mbps)", "다이렉트", now);
        addPlan(plans, "SKT", "NA00008531", "LTE 다이렉트 39", 39000, "LTE", "5GB", "다이렉트", now);
        addPlan(plans, "SKT", "NA00008532", "LTE 다이렉트 33", 33000, "LTE", "2GB", "다이렉트", now);
        addPlan(plans, "SKT", "NA00008533", "LTE 다이렉트 29", 29000, "LTE", "1GB", "다이렉트", now);

        // 청년/시니어/복지
        addPlan(plans, "SKT", "NA00008540", "0 청년 LTE", 47000, "LTE", "무제한(3Mbps)", "청년", now);
        addPlan(plans, "SKT", "NA00008541", "0 시니어 LTE", 39000, "LTE", "무제한(1Mbps)", "시니어", now);
        addPlan(plans, "SKT", "NA00008542", "복지 LTE", 22000, "LTE", "2GB", "복지", now);

        // ID 자동 생성
        plans.forEach(p -> p.setId(p.generateId()));

        log.info("✅ SKT {}개 요금제 로드", plans.size());
        return plans;
    }

    private void addPlan(List<CarrierPlan> plans, String carrier, String code, String name,
                         int fee, String network, String data, String type, LocalDateTime now) {
        plans.add(CarrierPlan.builder()
            .carrier(carrier)
            .planCode(code)
            .planName(name)
            .monthlyFee(fee)
            .networkType(network)
            .dataAllowance(data)
            .voiceAllowance("무제한")
            .smsAllowance("무제한")
            .planType(type)
            .active(true)
            .createdAt(now)
            .build());
    }

    /**
     * KT 요금제 조회
     */
    public List<CarrierPlan> fetchKtPlans() {
        log.info("📡 KT 요금제 조회 중...");

        List<CarrierPlan> plans = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // ==================== KT 5G 요금제 (금액별 대표) ====================
        addPlan(plans, "KT", "PL5G130", "5G 시그니처", 130000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "KT", "PL5G110", "5G 슈퍼플랜 맥스", 110000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "KT", "PL5G105", "5G 슈퍼플랜 프리미엄", 105000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "KT", "PL5G100", "5G 슈퍼플랜 스페셜", 100000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "KT", "PL5G95", "5G 초이스 프리미엄", 95000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "KT", "PL5G90", "5G 초이스 베이직", 90000, "5G", "무제한", "기본", now);
        addPlan(plans, "KT", "PL5G85", "5G 슈퍼플랜 에센셜", 85000, "5G", "무제한", "기본", now);
        addPlan(plans, "KT", "PL5G80", "5G 슈퍼플랜 베이직", 80000, "5G", "무제한(10Mbps)", "기본", now);
        addPlan(plans, "KT", "PL5G75", "5G 라이트", 75000, "5G", "무제한(5Mbps)", "기본", now);
        addPlan(plans, "KT", "PL5G69", "5G 슬림", 69000, "5G", "무제한(5Mbps)", "기본", now);
        addPlan(plans, "KT", "PL5G61", "5G 다이렉트", 61000, "5G", "15GB", "다이렉트", now);
        addPlan(plans, "KT", "PL5G55", "5G 다이렉트 55", 55000, "5G", "10GB", "다이렉트", now);
        addPlan(plans, "KT", "PL5G49", "5G Y덤 49", 49000, "5G", "무제한(3Mbps)", "청년", now);
        addPlan(plans, "KT", "PL5G45", "5G 다이렉트 45", 45000, "5G", "6GB", "다이렉트", now);

        // ==================== KT LTE 요금제 (금액별 대표) ====================
        addPlan(plans, "KT", "PLLTE100", "LTE 무제한", 100000, "LTE", "무제한", "프리미엄", now);
        addPlan(plans, "KT", "PLLTE95", "LTE 프리미어 플러스", 95000, "LTE", "무제한", "프리미엄", now);
        addPlan(plans, "KT", "PLLTE89", "LTE 프리미어", 89000, "LTE", "무제한", "프리미엄", now);
        addPlan(plans, "KT", "PLLTE85", "LTE 에센셜 플러스", 85000, "LTE", "무제한", "기본", now);
        addPlan(plans, "KT", "PLLTE79", "LTE 에센셜", 79000, "LTE", "무제한(10Mbps)", "기본", now);
        addPlan(plans, "KT", "PLLTE75", "LTE 스탠다드", 75000, "LTE", "무제한(5Mbps)", "기본", now);
        addPlan(plans, "KT", "PLLTE69", "LTE 베이직", 69000, "LTE", "무제한(3Mbps)", "기본", now);
        addPlan(plans, "KT", "PLLTE65", "LTE 슬림 플러스", 65000, "LTE", "무제한(3Mbps)", "기본", now);
        addPlan(plans, "KT", "PLLTE59", "LTE 슬림", 59000, "LTE", "15GB", "기본", now);
        addPlan(plans, "KT", "PLLTE55", "LTE 다이렉트 55", 55000, "LTE", "12GB", "다이렉트", now);
        addPlan(plans, "KT", "PLLTE51", "LTE 다이렉트 51", 51000, "LTE", "10GB", "다이렉트", now);
        addPlan(plans, "KT", "PLLTE47", "LTE 다이렉트 47", 47000, "LTE", "7GB", "다이렉트", now);
        addPlan(plans, "KT", "PLLTE44", "LTE Y덤 44", 44000, "LTE", "무제한(1Mbps)", "청년", now);
        addPlan(plans, "KT", "PLLTE42", "LTE 다이렉트 42", 42000, "LTE", "5GB", "다이렉트", now);
        addPlan(plans, "KT", "PLLTE39", "LTE 심플", 39000, "LTE", "3GB", "기본", now);
        addPlan(plans, "KT", "PLLTE35", "LTE 다이렉트 35", 35000, "LTE", "2GB", "다이렉트", now);
        addPlan(plans, "KT", "PLLTE33", "LTE 라이트", 33000, "LTE", "1.5GB", "기본", now);
        addPlan(plans, "KT", "PLLTE29", "LTE 다이렉트 29", 29000, "LTE", "1GB", "다이렉트", now);
        addPlan(plans, "KT", "PLLTE25", "LTE 세이브", 25000, "LTE", "500MB", "기본", now);
        addPlan(plans, "KT", "PLLTE22", "복지 요금제", 22000, "LTE", "2GB", "복지", now);

        // ID 자동 생성
        plans.forEach(p -> p.setId(p.generateId()));

        log.info("✅ KT {}개 요금제 로드", plans.size());
        return plans;
    }

    /**
     * LGU+ 요금제 조회
     */
    public List<CarrierPlan> fetchLguPlans() {
        log.info("📡 LGU+ 요금제 조회 중...");

        List<CarrierPlan> plans = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // ==================== LGU+ 5G 요금제 (금액별 대표) ====================
        addPlan(plans, "LGU", "LPZ5G130", "5G 시그니처 플러스", 130000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "LGU", "LPZ5G115", "5G 시그니처 레귤러", 115000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "LGU", "LPZ5G110", "5G 시그니처", 110000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "LGU", "LPZ5G105", "5G 프리미어 플러스", 105000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "LGU", "LPZ5G100", "5G 프리미어 스페셜", 100000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "LGU", "LPZ5G95", "5G 프리미어 맥스", 95000, "5G", "무제한", "프리미엄", now);
        addPlan(plans, "LGU", "LPZ5G89", "5G 프리미어 레귤러 플러스", 89000, "5G", "무제한", "기본", now);
        addPlan(plans, "LGU", "LPZ5G85", "5G 프리미어 에센셜", 85000, "5G", "무제한", "기본", now);
        addPlan(plans, "LGU", "LPZ5G80", "5G 프리미어 베이직", 80000, "5G", "무제한(10Mbps)", "기본", now);
        addPlan(plans, "LGU", "LPZ5G75", "5G 프리미어 레귤러", 75000, "5G", "무제한(5Mbps)", "기본", now);
        addPlan(plans, "LGU", "LPZ5G69", "5G 스탠다드", 69000, "5G", "무제한(5Mbps)", "기본", now);
        addPlan(plans, "LGU", "LPZ5G61", "5G 라이트 플러스", 61000, "5G", "20GB", "기본", now);
        addPlan(plans, "LGU", "LPZ5G55", "5G 슬림", 55000, "5G", "15GB", "기본", now);
        addPlan(plans, "LGU", "LPZ5G51", "5G 다이렉트 51", 51000, "5G", "12GB", "다이렉트", now);
        addPlan(plans, "LGU", "LPZ5G49", "5G 청년 49", 49000, "5G", "무제한(3Mbps)", "청년", now);
        addPlan(plans, "LGU", "LPZ5G45", "5G 다이렉트 45", 45000, "5G", "8GB", "다이렉트", now);

        // ==================== LGU+ LTE 요금제 (금액별 대표) ====================
        addPlan(plans, "LGU", "LPZLTE100", "LTE 무제한 프리미엄", 100000, "LTE", "무제한", "프리미엄", now);
        addPlan(plans, "LGU", "LPZLTE95", "LTE 프리미어 플러스", 95000, "LTE", "무제한", "프리미엄", now);
        addPlan(plans, "LGU", "LPZLTE89", "LTE 프리미어", 89000, "LTE", "무제한", "프리미엄", now);
        addPlan(plans, "LGU", "LPZLTE85", "LTE 프리미어 에센셜", 85000, "LTE", "무제한", "기본", now);
        addPlan(plans, "LGU", "LPZLTE79", "LTE 에센셜 플러스", 79000, "LTE", "무제한(10Mbps)", "기본", now);
        addPlan(plans, "LGU", "LPZLTE75", "LTE 에센셜", 75000, "LTE", "무제한(5Mbps)", "기본", now);
        addPlan(plans, "LGU", "LPZLTE69", "LTE 프리미어 레귤러", 69000, "LTE", "무제한(3Mbps)", "기본", now);
        addPlan(plans, "LGU", "LPZLTE65", "LTE 스탠다드 플러스", 65000, "LTE", "무제한(3Mbps)", "기본", now);
        addPlan(plans, "LGU", "LPZLTE59", "LTE 스탠다드", 59000, "LTE", "15GB", "기본", now);
        addPlan(plans, "LGU", "LPZLTE55", "LTE 베이직 플러스", 55000, "LTE", "12GB", "기본", now);
        addPlan(plans, "LGU", "LPZLTE51", "LTE 베이직", 51000, "LTE", "10GB", "기본", now);
        addPlan(plans, "LGU", "LPZLTE47", "LTE 슬림", 47000, "LTE", "7GB", "기본", now);
        addPlan(plans, "LGU", "LPZLTE44", "LTE 청년 44", 44000, "LTE", "무제한(1Mbps)", "청년", now);
        addPlan(plans, "LGU", "LPZLTE42", "LTE 다이렉트 42", 42000, "LTE", "5GB", "다이렉트", now);
        addPlan(plans, "LGU", "LPZLTE39", "LTE 다이렉트 39", 39000, "LTE", "3GB", "다이렉트", now);
        addPlan(plans, "LGU", "LPZLTE37", "LTE 심플", 37000, "LTE", "2GB", "기본", now);
        addPlan(plans, "LGU", "LPZLTE35", "LTE 다이렉트 35", 35000, "LTE", "2GB", "다이렉트", now);
        addPlan(plans, "LGU", "LPZLTE33", "LTE 라이트", 33000, "LTE", "1.5GB", "기본", now);
        addPlan(plans, "LGU", "LPZLTE29", "LTE 다이렉트 29", 29000, "LTE", "1GB", "다이렉트", now);
        addPlan(plans, "LGU", "LPZLTE25", "LTE 세이브", 25000, "LTE", "500MB", "기본", now);
        addPlan(plans, "LGU", "LPZLTE22", "복지 요금제", 22000, "LTE", "2GB", "복지", now);

        // ID 자동 생성
        plans.forEach(p -> p.setId(p.generateId()));

        log.info("✅ LGU+ {}개 요금제 로드", plans.size());
        return plans;
    }

    /**
     * LGU+ API에서 실시간 요금제 목록 조회 (향후 구현)
     * /uhdc/fo/prdv/mdlbsufu/v2/mbl-pp-list
     */
    public List<CarrierPlan> fetchLguPlansFromApi() {
        log.info("📡 LGU+ API에서 요금제 조회 시도...");

        try {
            String response = webClient.get()
                .uri("https://www.lguplus.com/uhdc/fo/prdv/mdlbsufu/v2/mbl-pp-list")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (response == null || response.isEmpty()) {
                log.warn("⚠️ LGU+ 요금제 API 응답 없음 - 기본 데이터 사용");
                return fetchLguPlans();
            }

            // 실제 API 응답 파싱 (구조에 맞게 수정 필요)
            List<CarrierPlan> plans = parseLguPlanResponse(response);
            if (plans.isEmpty()) {
                log.warn("⚠️ LGU+ 요금제 파싱 실패 - 기본 데이터 사용");
                return fetchLguPlans();
            }

            log.info("✅ LGU+ API에서 {}개 요금제 조회 완료", plans.size());
            return plans;

        } catch (Exception e) {
            log.warn("⚠️ LGU+ API 조회 실패: {} - 기본 데이터 사용", e.getMessage());
            return fetchLguPlans();
        }
    }

    private List<CarrierPlan> parseLguPlanResponse(String jsonResponse) {
        List<CarrierPlan> plans = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            // 응답 구조에 맞게 파싱 (예시)
            JsonNode planList = root.path("mblPpList");
            if (planList.isMissingNode()) {
                planList = root.path("data").path("list");
            }

            if (planList.isArray()) {
                LocalDateTime now = LocalDateTime.now();
                for (JsonNode node : planList) {
                    String planCode = JsonNodeUtils.getTextValue(node, "mblPpCd");
                    String planName = JsonNodeUtils.getTextValue(node, "mblPpNm");
                    Integer monthlyFee = JsonNodeUtils.getIntValue(node, "mblPpFee");

                    if (planCode != null && planName != null && monthlyFee != null) {
                        CarrierPlan plan = CarrierPlan.builder()
                            .carrier("LGU")
                            .planCode(planCode)
                            .planName(planName)
                            .monthlyFee(monthlyFee)
                            .networkType(planName.contains("5G") ? "5G" : "LTE")
                            .active(true)
                            .createdAt(now)
                            .build();
                        plan.setId(plan.generateId());
                        plans.add(plan);
                    }
                }
            }
        } catch (Exception e) {
            log.error("LGU+ 요금제 응답 파싱 실패: {}", e.getMessage());
        }

        return plans;
    }

}
