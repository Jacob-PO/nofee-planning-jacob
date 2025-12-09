package com.nofee.api.test.carrierintegration.service;

import com.nofee.api.test.carrierintegration.dto.CarrierPlan;
import com.nofee.api.test.carrierintegration.dto.CarrierSubsidy;
import com.nofee.api.test.carrierintegration.dto.UnifiedSubsidyResponse;
import com.nofee.api.test.carrierintegration.util.CarrierCodeUtils;
import com.nofee.api.test.carrierintegration.util.JoinType;
import com.nofee.api.test.carrierintegration.util.SupportType;
import com.nofee.api.test.devicemapping.dto.DeviceMapping;
import com.nofee.api.test.devicemapping.service.GoogleSheetsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 직접 조회 서비스 (캐시 없이 실시간 통신사 API 호출)
 *
 * 플로우:
 * 1. summary-mapping 시트에서 노피 상품코드 → 통신사별 기기코드 조회
 * 2. summary-plan 시트에서 월정액 → 통신사별 요금제코드 조회
 * 3. 통신사 API 직접 호출 (SKT, KT, LGU+)
 * 4. 결과 필터링 후 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirectSubsidyService {

    private final SktSubsidyService sktSubsidyService;
    private final KtSubsidyService ktSubsidyService;
    private final LguSubsidyService lguSubsidyService;
    private final PlanSheetService planSheetService;
    private final GoogleSheetsService googleSheetsService;

    /**
     * 노피 상품코드 + 통신사로 공시지원금 직접 조회 (단일 통신사)
     *
     * @param nofeeProductCode 노피 상품코드 (예: "SM-ZP-7")
     * @param carrier 통신사 (SKT, KT, LGU)
     * @param joinType 가입유형 (노피코드: 0301007001=신규, 0301007002=기기변경, 0301007003=번호이동)
     * @param planMonthlyFee 요금제 월정액 (예: 89000)
     */
    public UnifiedSubsidyResponse fetchByNofeeProductAndCarrier(
            String nofeeProductCode, String carrier, String joinType, Integer planMonthlyFee) {

        log.info("📡 단일 통신사 조회: nofeeProductCode={}, carrier={}, joinType={}, planMonthlyFee={}",
            nofeeProductCode, carrier, joinType, planMonthlyFee);
        long startTime = System.currentTimeMillis();

        try {
            // 1. summary-mapping에서 통신사별 기기코드 조회
            DeviceMapping mapping = findMappingByNofeeCode(nofeeProductCode);
            if (mapping == null) {
                return errorResponse("노피 상품코드 '" + nofeeProductCode + "'에 대한 매핑이 없습니다.", startTime);
            }

            // 2. 통신사 정규화
            String normalizedCarrier = CarrierCodeUtils.normalize(carrier);

            // 3. 해당 통신사의 기기코드 추출
            String deviceCode = switch (normalizedCarrier) {
                case "SKT" -> mapping.getSktDeviceCode();
                case "KT" -> mapping.getKtDeviceCode();
                case "LGU" -> mapping.getLguDeviceCode();
                default -> null;
            };

            if (deviceCode == null || deviceCode.isEmpty()) {
                return errorResponse("통신사 '" + carrier + "'에 대한 기기코드 매핑이 없습니다.", startTime);
            }

            log.info("✅ 매핑 조회: {} -> {}:{}", mapping.getNofeeProductName(), normalizedCarrier, deviceCode);

            // 4. 한글 가입유형 변환
            String joinTypeKorean = JoinType.toKorean(joinType);

            // 5. 해당 통신사만 조회
            List<CarrierSubsidy> sktSubsidies = new ArrayList<>();
            List<CarrierSubsidy> ktSubsidies = new ArrayList<>();
            List<CarrierSubsidy> lguSubsidies = new ArrayList<>();

            // 기본값 5G로 조회
            switch (normalizedCarrier) {
                case "SKT" -> sktSubsidies = fetchFromSkt(deviceCode, joinTypeKorean, planMonthlyFee, "5G");
                case "KT" -> ktSubsidies = fetchFromKt(deviceCode, joinTypeKorean, planMonthlyFee, "5G");
                case "LGU" -> lguSubsidies = fetchFromLgu(deviceCode, joinTypeKorean, planMonthlyFee, "5G");
            }

            long elapsed = System.currentTimeMillis() - startTime;
            int resultCount = sktSubsidies.size() + ktSubsidies.size() + lguSubsidies.size();
            log.info("✅ {} 조회 완료: {}건 ({}ms)", normalizedCarrier, resultCount, elapsed);

            return UnifiedSubsidyResponse.builder()
                .success(true)
                .deviceCode(nofeeProductCode)
                .deviceName(mapping.getNofeeProductName())
                .sktSubsidies(sktSubsidies)
                .ktSubsidies(ktSubsidies)
                .lguSubsidies(lguSubsidies)
                .elapsedMs(elapsed)
                .build();

        } catch (Exception e) {
            log.error("❌ 직접 조회 실패: {}", e.getMessage());
            return errorResponse(e.getMessage(), startTime);
        }
    }

    /**
     * 노피 상품코드로 공시지원금 직접 조회 (전체 통신사)
     *
     * @param nofeeProductCode 노피 상품코드 (예: "AP-E-16")
     * @param joinType 가입유형 (노피코드: 0301007001=신규, 0301007002=기기변경, 0301007003=번호이동)
     * @param planMonthlyFee 요금제 월정액 (예: 85000)
     */
    public UnifiedSubsidyResponse fetchByNofeeProduct(
            String nofeeProductCode, String joinType, Integer planMonthlyFee) {

        log.info("📡 직접 조회 시작: nofeeProductCode={}, joinType={}, planMonthlyFee={}",
            nofeeProductCode, joinType, planMonthlyFee);
        long startTime = System.currentTimeMillis();

        try {
            // 1. summary-mapping에서 통신사별 기기코드 조회
            DeviceMapping mapping = findMappingByNofeeCode(nofeeProductCode);
            if (mapping == null) {
                return errorResponse("노피 상품코드 '" + nofeeProductCode + "'에 대한 매핑이 없습니다.", startTime);
            }

            log.info("✅ 매핑 조회 완료: {} -> SKT:{}, KT:{}, LGU:{}",
                mapping.getNofeeProductName(),
                mapping.getSktDeviceCode(),
                mapping.getKtDeviceCode(),
                mapping.getLguDeviceCode());

            // 2. 한글 가입유형 변환
            String joinTypeKorean = JoinType.toKorean(joinType);

            // 3. 각 통신사별 조회 (기본값 5G)
            List<CarrierSubsidy> sktSubsidies = fetchFromSkt(
                mapping.getSktDeviceCode(), joinTypeKorean, planMonthlyFee, "5G");
            List<CarrierSubsidy> ktSubsidies = fetchFromKt(
                mapping.getKtDeviceCode(), joinTypeKorean, planMonthlyFee, "5G");
            List<CarrierSubsidy> lguSubsidies = fetchFromLgu(
                mapping.getLguDeviceCode(), joinTypeKorean, planMonthlyFee, "5G");

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ 직접 조회 완료: SKT {}, KT {}, LGU+ {} ({}ms)",
                sktSubsidies.size(), ktSubsidies.size(), lguSubsidies.size(), elapsed);

            return UnifiedSubsidyResponse.builder()
                .success(true)
                .deviceCode(nofeeProductCode)
                .deviceName(mapping.getNofeeProductName())
                .sktSubsidies(sktSubsidies)
                .ktSubsidies(ktSubsidies)
                .lguSubsidies(lguSubsidies)
                .elapsedMs(elapsed)
                .build();

        } catch (Exception e) {
            log.error("❌ 직접 조회 실패: {}", e.getMessage());
            return errorResponse(e.getMessage(), startTime);
        }
    }

    /**
     * 상품명 + 통신사로 공시지원금 직접 조회 (프론트엔드용 메인 API)
     *
     * @param productGroupNm 상품명 (예: "갤럭시 S24 울트라", "갤럭시 Z 플립 7")
     * @param carrier 통신사 노피코드 (0301001001=SKT, 0301001002=KT, 0301001003=LGU)
     * @param joinType 가입유형 노피코드 (0301007001=신규, 0301007002=기기변경, 0301007003=번호이동)
     * @param planMonthlyFee 요금제 월정액 (예: 69000)
     * @param networkType 네트워크 유형 (5G 또는 LTE)
     * @param supportType 지원유형 (공시지원금, 선택약정)
     */
    public UnifiedSubsidyResponse fetchByProductGroupNm(
            String productGroupNm, String carrier, String joinType, Integer planMonthlyFee, String networkType, String supportType) {

        log.info("📡 상품명 조회: productGroupNm={}, carrier={}, joinType={}, planMonthlyFee={}, networkType={}, supportType={}",
            productGroupNm, carrier, joinType, planMonthlyFee, networkType, supportType);
        long startTime = System.currentTimeMillis();

        try {
            // 1. summary-mapping에서 상품명으로 매핑 조회
            DeviceMapping mapping = findMappingByProductName(productGroupNm);
            if (mapping == null) {
                return errorResponse("상품명 '" + productGroupNm + "'에 대한 매핑이 없습니다.", startTime);
            }

            // 2. 통신사 정규화
            String normalizedCarrier = CarrierCodeUtils.normalize(carrier);

            // 3. 해당 통신사의 기기코드 추출
            String deviceCode = switch (normalizedCarrier) {
                case "SKT" -> mapping.getSktDeviceCode();
                case "KT" -> mapping.getKtDeviceCode();
                case "LGU" -> mapping.getLguDeviceCode();
                default -> null;
            };

            if (deviceCode == null || deviceCode.isEmpty()) {
                return errorResponse("통신사 '" + normalizedCarrier + "'에 대한 기기코드 매핑이 없습니다. (상품: " + productGroupNm + ")", startTime);
            }

            log.info("✅ 매핑 조회: {} -> {}:{}", mapping.getNofeeProductName(), normalizedCarrier, deviceCode);

            // 4. 한글 가입유형/지원유형 변환
            String joinTypeKorean = JoinType.toKorean(joinType);
            String supportTypeKorean = SupportType.toKorean(supportType);
            String effectiveNetworkType = (networkType != null && !networkType.isEmpty()) ? networkType : "5G";

            // 5. 해당 통신사만 조회
            List<CarrierSubsidy> sktSubsidies = new ArrayList<>();
            List<CarrierSubsidy> ktSubsidies = new ArrayList<>();
            List<CarrierSubsidy> lguSubsidies = new ArrayList<>();

            switch (normalizedCarrier) {
                case "SKT" -> sktSubsidies = fetchFromSkt(deviceCode, joinTypeKorean, planMonthlyFee, effectiveNetworkType);
                case "KT" -> ktSubsidies = fetchFromKt(deviceCode, joinTypeKorean, planMonthlyFee, effectiveNetworkType);
                case "LGU" -> lguSubsidies = fetchFromLgu(deviceCode, joinTypeKorean, planMonthlyFee, effectiveNetworkType);
            }

            // 6. 선택약정인 경우 supportType 변경, 공시지원금/추가지원금 0원 처리
            if ("선택약정".equals(supportTypeKorean)) {
                applySupportTypeForSelectDiscount(sktSubsidies);
                applySupportTypeForSelectDiscount(ktSubsidies);
                applySupportTypeForSelectDiscount(lguSubsidies);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            int resultCount = sktSubsidies.size() + ktSubsidies.size() + lguSubsidies.size();
            log.info("✅ {} 조회 완료: {}건 ({}ms)", normalizedCarrier, resultCount, elapsed);

            return UnifiedSubsidyResponse.builder()
                .success(true)
                .deviceCode(deviceCode)
                .deviceName(mapping.getNofeeProductName())
                .sktSubsidies(sktSubsidies)
                .ktSubsidies(ktSubsidies)
                .lguSubsidies(lguSubsidies)
                .elapsedMs(elapsed)
                .build();

        } catch (Exception e) {
            log.error("❌ 상품명 조회 실패: {}", e.getMessage());
            return errorResponse(e.getMessage(), startTime);
        }
    }

    /**
     * 통신사별 기기코드로 직접 조회
     *
     * @param sktCode SKT 기기코드
     * @param ktCode KT 기기코드
     * @param lguCode LGU+ 기기코드
     * @param joinType 가입유형
     * @param planMonthlyFee 요금제 월정액
     * @param networkType 네트워크 유형 (5G 또는 LTE)
     */
    public UnifiedSubsidyResponse fetchByDeviceCodes(
            String sktCode, String ktCode, String lguCode,
            String joinType, Integer planMonthlyFee, String networkType) {

        log.info("📡 기기코드 직접 조회: SKT={}, KT={}, LGU={}, joinType={}, fee={}, networkType={}",
            sktCode, ktCode, lguCode, joinType, planMonthlyFee, networkType);
        long startTime = System.currentTimeMillis();

        try {
            String joinTypeKorean = JoinType.toKorean(joinType);
            String effectiveNetworkType = (networkType != null && !networkType.isEmpty()) ? networkType : "5G";

            List<CarrierSubsidy> sktSubsidies = new ArrayList<>();
            List<CarrierSubsidy> ktSubsidies = new ArrayList<>();
            List<CarrierSubsidy> lguSubsidies = new ArrayList<>();

            if (sktCode != null && !sktCode.isEmpty()) {
                sktSubsidies = fetchFromSkt(sktCode, joinTypeKorean, planMonthlyFee, effectiveNetworkType);
            }
            if (ktCode != null && !ktCode.isEmpty()) {
                ktSubsidies = fetchFromKt(ktCode, joinTypeKorean, planMonthlyFee, effectiveNetworkType);
            }
            if (lguCode != null && !lguCode.isEmpty()) {
                lguSubsidies = fetchFromLgu(lguCode, joinTypeKorean, planMonthlyFee, effectiveNetworkType);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ 기기코드 조회 완료: SKT {}, KT {}, LGU+ {} ({}ms)",
                sktSubsidies.size(), ktSubsidies.size(), lguSubsidies.size(), elapsed);

            String deviceName = extractDeviceName(sktSubsidies, ktSubsidies, lguSubsidies);

            return UnifiedSubsidyResponse.builder()
                .success(true)
                .deviceCode(sktCode != null ? sktCode : (ktCode != null ? ktCode : lguCode))
                .deviceName(deviceName)
                .sktSubsidies(sktSubsidies)
                .ktSubsidies(ktSubsidies)
                .lguSubsidies(lguSubsidies)
                .elapsedMs(elapsed)
                .build();

        } catch (Exception e) {
            log.error("❌ 기기코드 조회 실패: {}", e.getMessage());
            return errorResponse(e.getMessage(), startTime);
        }
    }

    // ==================== Private Methods ====================

    /**
     * SKT 공시지원금 조회
     */
    private List<CarrierSubsidy> fetchFromSkt(String deviceCode, String joinTypeKorean, Integer planMonthlyFee, String networkType) {
        if (deviceCode == null || deviceCode.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // summary-plan에서 요금제 코드 조회 (networkType 고려)
            String planCode = planSheetService.getPlanCodeByMonthlyFeeAndNetwork("SKT", planMonthlyFee, networkType);
            CarrierPlan plan = planCode != null ? planSheetService.getPlan("SKT", planCode) : null;

            // SKT joinType 코드 변환 (한글 → SKT 코드)
            String sktJoinType = JoinType.toSktCode(joinTypeKorean);

            log.debug("📡 SKT 조회: device={}, plan={}, joinType={}, networkType={}", deviceCode, planCode, sktJoinType, networkType);

            // API 호출 (요금제 코드 + 가입유형 + 네트워크 유형으로)
            List<CarrierSubsidy> allSubsidies = sktSubsidyService.fetchAllSubsidies(planCode, sktJoinType, networkType);

            // 기기코드로 필터링
            List<CarrierSubsidy> filtered = allSubsidies.stream()
                .filter(s -> deviceCode.equals(s.getDeviceCode()))
                .toList();

            // planMonthlyFee 설정 (API 응답에 없을 경우)
            if (plan != null) {
                for (CarrierSubsidy s : filtered) {
                    if (s.getPlanMonthlyFee() == null || s.getPlanMonthlyFee() == 0) {
                        s.setPlanMonthlyFee(plan.getMonthlyFee());
                    }
                }
            }

            log.info("📊 SKT 결과: {}건 (전체 {}건에서 필터)", filtered.size(), allSubsidies.size());
            return filtered;

        } catch (Exception e) {
            log.warn("⚠️ SKT 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * KT 공시지원금 조회
     *
     * KT API는 요금제 코드가 필수이므로, 월정액에 맞는 요금제 코드로 조회
     * - 130,000원 이상: PL244N943 (티빙/지니/밀리 초이스 프리미엄)
     * - 100,000원 이상: PL244N944 (티빙/지니/밀리 초이스 스페셜)
     * - 그 외: PL244N945 (티빙/지니/밀리 초이스 베이직)
     */
    private List<CarrierSubsidy> fetchFromKt(String deviceCode, String joinTypeKorean, Integer planMonthlyFee, String networkType) {
        if (deviceCode == null || deviceCode.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 월정액에 맞는 KT 요금제 코드 선택 (API 조회용)
            String planCode = getKtPlanCodeForQuery(planMonthlyFee);

            // KT joinType 코드 변환 (한글 → KT 코드)
            String ktJoinType = JoinType.toKtCode(joinTypeKorean);

            log.debug("📡 KT 조회: device={}, planCode={} (조회용), joinType={}, networkType={}",
                deviceCode, planCode, ktJoinType, networkType);

            // API 호출 - 결과에는 KT API가 반환하는 실제 요금제 정보(pplId, pplNm)가 포함됨
            List<CarrierSubsidy> allSubsidies = ktSubsidyService.fetchAllSubsidies(planCode, ktJoinType, networkType);

            // 기기코드로 필터링
            List<CarrierSubsidy> filtered = allSubsidies.stream()
                .filter(s -> deviceCode.equals(s.getDeviceCode()))
                .toList();

            // KT API 응답에는 월정액 정보가 없으므로, 요청 파라미터로 설정
            // (KT 공시지원금 API의 pplId는 내부코드로, 요금제 목록 API의 onfrmCd와 다름)
            if (planMonthlyFee != null) {
                for (CarrierSubsidy s : filtered) {
                    if (s.getPlanMonthlyFee() == null) {
                        s.setPlanMonthlyFee(planMonthlyFee);
                    }
                }
            }

            log.info("📊 KT 결과: {}건 (전체 {}건에서 필터)", filtered.size(), allSubsidies.size());
            return filtered;

        } catch (Exception e) {
            log.warn("⚠️ KT 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 월정액에 맞는 KT 요금제 코드 반환 (API 조회용)
     * KT API는 요금제 코드 없이 조회 불가하므로 적절한 요금제 선택
     */
    private String getKtPlanCodeForQuery(Integer planMonthlyFee) {
        if (planMonthlyFee == null) {
            return "PL244N945"; // 기본값: 베이직
        }
        if (planMonthlyFee >= 130000) {
            return "PL244N943"; // 프리미엄 (130,000원)
        } else if (planMonthlyFee >= 100000) {
            return "PL244N944"; // 스페셜 (110,000원)
        } else {
            return "PL244N945"; // 베이직 (90,000원)
        }
    }

    /**
     * LGU+ 공시지원금 조회
     */
    private List<CarrierSubsidy> fetchFromLgu(String deviceCode, String joinTypeKorean, Integer planMonthlyFee, String networkType) {
        if (deviceCode == null || deviceCode.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // summary-plan에서 요금제 코드 조회 (networkType 고려)
            String planCode = planSheetService.getPlanCodeByMonthlyFeeAndNetwork("LGU", planMonthlyFee, networkType);
            CarrierPlan plan = planCode != null ? planSheetService.getPlan("LGU", planCode) : null;

            // LGU joinType 코드 변환 (한글 → LGU 코드)
            String lguJoinType = JoinType.toLguCode(joinTypeKorean);

            log.debug("📡 LGU+ 조회: device={}, plan={}, joinType={}, networkType={}", deviceCode, planCode, lguJoinType, networkType);

            // API 호출 (networkType 전달)
            List<CarrierSubsidy> allSubsidies = lguSubsidyService.fetchAllSubsidies(planCode, lguJoinType, networkType);

            // 기기코드로 필터링
            List<CarrierSubsidy> filtered = allSubsidies.stream()
                .filter(s -> deviceCode.equals(s.getDeviceCode()))
                .toList();

            // planMonthlyFee 설정
            if (plan != null) {
                for (CarrierSubsidy s : filtered) {
                    if (s.getPlanMonthlyFee() == null || s.getPlanMonthlyFee() == 0) {
                        s.setPlanMonthlyFee(plan.getMonthlyFee());
                    }
                }
            }

            log.info("📊 LGU+ 결과: {}건 (전체 {}건에서 필터)", filtered.size(), allSubsidies.size());
            return filtered;

        } catch (Exception e) {
            log.warn("⚠️ LGU+ 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * summary-mapping 시트에서 노피 상품코드로 매핑 조회
     */
    private DeviceMapping findMappingByNofeeCode(String nofeeProductCode) {
        List<DeviceMapping> allMappings = googleSheetsService.loadMappings();
        return allMappings.stream()
            .filter(m -> nofeeProductCode.equals(m.getNofeeProductCode()))
            .findFirst()
            .orElse(null);
    }

    /**
     * summary-mapping 시트에서 상품명으로 매핑 조회
     * 상품명이 정확히 일치하거나, 포함되어 있으면 매칭
     */
    private DeviceMapping findMappingByProductName(String productGroupNm) {
        if (productGroupNm == null || productGroupNm.isEmpty()) {
            return null;
        }

        List<DeviceMapping> allMappings = googleSheetsService.loadMappings();
        String searchName = normalizeProductName(productGroupNm);

        // 1. 정확히 일치하는 경우
        for (DeviceMapping m : allMappings) {
            if (m.getNofeeProductName() != null) {
                String mappingName = normalizeProductName(m.getNofeeProductName());
                if (searchName.equals(mappingName)) {
                    log.debug("✅ 상품명 정확 매칭: {} -> {}", productGroupNm, m.getNofeeProductName());
                    return m;
                }
            }
        }

        // 2. 부분 매칭 (상품명이 포함되어 있는 경우)
        for (DeviceMapping m : allMappings) {
            if (m.getNofeeProductName() != null) {
                String mappingName = normalizeProductName(m.getNofeeProductName());
                if (mappingName.contains(searchName) || searchName.contains(mappingName)) {
                    log.debug("✅ 상품명 부분 매칭: {} -> {}", productGroupNm, m.getNofeeProductName());
                    return m;
                }
            }
        }

        log.warn("⚠️ 상품명 매핑 없음: {}", productGroupNm);
        return null;
    }

    /**
     * 상품명 정규화 (공백, 특수문자 제거)
     */
    private String normalizeProductName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
            .replaceAll("\\s+", "")  // 공백 제거
            .replaceAll("[^a-z0-9가-힣]", "");  // 특수문자 제거
    }

    /**
     * 결과에서 기기명 추출
     */
    private String extractDeviceName(List<CarrierSubsidy> skt, List<CarrierSubsidy> kt, List<CarrierSubsidy> lgu) {
        if (!skt.isEmpty()) return skt.get(0).getDeviceName();
        if (!kt.isEmpty()) return kt.get(0).getDeviceName();
        if (!lgu.isEmpty()) return lgu.get(0).getDeviceName();
        return null;
    }

    /**
     * 선택약정 지원유형 적용
     * - supportType: "선택약정"
     * - carrierSubsidy: 0
     * - additionalSubsidy: 0
     * - installmentPrice: msrp (출고가 그대로)
     */
    private void applySupportTypeForSelectDiscount(List<CarrierSubsidy> subsidies) {
        for (CarrierSubsidy s : subsidies) {
            s.setSupportType("선택약정");
            s.setCarrierSubsidy(0);
            s.setAdditionalSubsidy(0);
            s.setInstallmentPrice(s.getMsrp()); // 출고가 그대로
        }
    }

    /**
     * 에러 응답 생성
     */
    private UnifiedSubsidyResponse errorResponse(String message, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        return UnifiedSubsidyResponse.builder()
            .success(false)
            .errorMessage(message)
            .elapsedMs(elapsed)
            .build();
    }
}
