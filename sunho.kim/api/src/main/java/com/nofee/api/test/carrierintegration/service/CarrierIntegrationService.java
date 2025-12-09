package com.nofee.api.test.carrierintegration.service;

import com.nofee.api.test.carrierintegration.dto.CarrierPlan;
import com.nofee.api.test.carrierintegration.dto.CarrierSubsidy;
import com.nofee.api.test.carrierintegration.dto.UnifiedSubsidyResponse;
import com.nofee.api.test.carrierintegration.util.CarrierCodeUtils;
import com.nofee.api.test.carrierintegration.util.JoinType;
import com.nofee.api.test.devicemapping.dto.CarrierDevice;
import com.nofee.api.test.devicemapping.dto.DeviceMapping;
import com.nofee.api.test.devicemapping.service.GoogleSheetsService;
import com.nofee.api.test.devicemapping.service.carrier.KtDeviceService;
import com.nofee.api.test.devicemapping.service.carrier.LguDeviceService;
import com.nofee.api.test.devicemapping.service.carrier.SktDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 통합 통신사 API 서비스
 *
 * SKT, KT, LGU+ 공시지원금 데이터를 통합하여 제공
 * Lazy Cache 적용 - 첫 조회 시 API 호출, 이후 캐시 사용 (24시간 TTL)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierIntegrationService {

    private final SktSubsidyService sktSubsidyService;
    private final KtSubsidyService ktSubsidyService;
    private final LguSubsidyService lguSubsidyService;
    private final SubsidyCacheService subsidyCacheService;

    // 기기 목록 조회 서비스
    private final SktDeviceService sktDeviceService;
    private final KtDeviceService ktDeviceService;
    private final LguDeviceService lguDeviceService;

    // 요금제 시트 서비스 (모든 요금제 코드 조회용)
    private final PlanSheetService planSheetService;

    // 매핑 조회용 (순환 의존성 방지를 위해 직접 사용)
    private final GoogleSheetsService googleSheetsService;

    /**
     * 모든 통신사 공시지원금 통합 조회 (캐시 우선)
     */
    public UnifiedSubsidyResponse fetchAllSubsidies() {
        return fetchAllSubsidiesWithCache(false);
    }

    /**
     * 모든 통신사 공시지원금 통합 조회 (캐시 옵션)
     * @param forceRefresh true면 캐시 무시하고 API 호출
     */
    public UnifiedSubsidyResponse fetchAllSubsidiesWithCache(boolean forceRefresh) {
        log.info("🚀 통합 공시지원금 조회 시작... (forceRefresh={})", forceRefresh);
        long startTime = System.currentTimeMillis();

        try {
            List<CarrierSubsidy> sktSubsidies;
            List<CarrierSubsidy> ktSubsidies;
            List<CarrierSubsidy> lguSubsidies;

            if (!forceRefresh) {
                // 캐시 확인
                boolean sktCacheValid = subsidyCacheService.isCacheValid("SKT");
                boolean ktCacheValid = subsidyCacheService.isCacheValid("KT");
                boolean lguCacheValid = subsidyCacheService.isCacheValid("LGU");

                log.info("📂 캐시 상태: SKT={}, KT={}, LGU={}", sktCacheValid, ktCacheValid, lguCacheValid);

                // 캐시에서 조회 또는 API 호출
                sktSubsidies = sktCacheValid
                    ? subsidyCacheService.getFromCache("SKT")
                    : fetchAndCacheCarrier("SKT");

                ktSubsidies = ktCacheValid
                    ? subsidyCacheService.getFromCache("KT")
                    : fetchAndCacheCarrier("KT");

                lguSubsidies = lguCacheValid
                    ? subsidyCacheService.getFromCache("LGU")
                    : fetchAndCacheCarrier("LGU");
            } else {
                // 강제 갱신 - 병렬 API 호출
                CompletableFuture<List<CarrierSubsidy>> sktFuture =
                    CompletableFuture.supplyAsync(() -> fetchAndCacheCarrier("SKT"));
                CompletableFuture<List<CarrierSubsidy>> ktFuture =
                    CompletableFuture.supplyAsync(() -> fetchAndCacheCarrier("KT"));
                CompletableFuture<List<CarrierSubsidy>> lguFuture =
                    CompletableFuture.supplyAsync(() -> fetchAndCacheCarrier("LGU"));

                CompletableFuture.allOf(sktFuture, ktFuture, lguFuture).join();

                sktSubsidies = sktFuture.get();
                ktSubsidies = ktFuture.get();
                lguSubsidies = lguFuture.get();
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ 통합 조회 완료: SKT {}, KT {}, LGU+ {} ({}ms)",
                sktSubsidies.size(), ktSubsidies.size(), lguSubsidies.size(), elapsed);

            return UnifiedSubsidyResponse.builder()
                .success(true)
                .sktSubsidies(sktSubsidies)
                .ktSubsidies(ktSubsidies)
                .lguSubsidies(lguSubsidies)
                .elapsedMs(elapsed)
                .build();

        } catch (Exception e) {
            log.error("❌ 통합 조회 실패: {}", e.getMessage());
            long elapsed = System.currentTimeMillis() - startTime;
            return UnifiedSubsidyResponse.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .elapsedMs(elapsed)
                .build();
        }
    }

    /**
     * 특정 통신사 공시지원금 조회 (캐시 우선)
     */
    public List<CarrierSubsidy> fetchSubsidiesByCarrier(String carrier) {
        return fetchSubsidiesByCarrierWithCache(carrier, false);
    }

    /**
     * 특정 통신사 공시지원금 조회 (캐시 옵션)
     */
    public List<CarrierSubsidy> fetchSubsidiesByCarrierWithCache(String carrier, boolean forceRefresh) {
        String normalizedCarrier = CarrierCodeUtils.normalize(carrier);
        log.info("📡 {} 공시지원금 조회... (forceRefresh={})", normalizedCarrier, forceRefresh);

        if (!forceRefresh && subsidyCacheService.isCacheValid(normalizedCarrier)) {
            log.info("📂 캐시에서 {} 데이터 조회", normalizedCarrier);
            return subsidyCacheService.getFromCache(normalizedCarrier);
        }

        return fetchAndCacheCarrier(normalizedCarrier);
    }

    /**
     * API 호출 후 캐시에 저장 (기본 파라미터)
     */
    private List<CarrierSubsidy> fetchAndCacheCarrier(String carrier) {
        return fetchAndCacheCarrier(carrier, null, null);
    }

    /**
     * API 호출 후 캐시에 저장 (파라미터 지정)
     * @param carrier 통신사 (SKT, KT, LGU)
     * @param planCode 요금제 코드 (null이면 기본값 사용)
     * @param joinType 가입유형 코드 (null이면 기본값 사용)
     */
    private List<CarrierSubsidy> fetchAndCacheCarrier(String carrier, String planCode, String joinType) {
        log.info("🌐 {} API 호출... (planCode={}, joinType={})", carrier, planCode, joinType);

        List<CarrierSubsidy> subsidies = switch (carrier) {
            case "SKT" -> sktSubsidyService.fetchAllSubsidies(planCode, joinType);
            case "KT" -> ktSubsidyService.fetchAllSubsidies(planCode, joinType);
            case "LGU" -> lguSubsidyService.fetchAllSubsidies(planCode, joinType);
            default -> new ArrayList<>();
        };

        if (!subsidies.isEmpty()) {
            subsidyCacheService.saveToCache(carrier, subsidies);
        }

        return subsidies;
    }

    /**
     * 특정 통신사 공시지원금 조회 (파라미터 지정, 캐시 우선)
     * @param carrier 통신사
     * @param planCode 요금제 코드
     * @param joinType 가입유형
     * @param planMonthlyFee 요금제 월 금액 (결과 필터링용)
     * @param forceRefresh 강제 갱신 여부
     */
    public List<CarrierSubsidy> fetchSubsidiesByCarrierWithParams(
            String carrier, String planCode, String joinType, Integer planMonthlyFee, boolean forceRefresh) {
        String normalizedCarrier = CarrierCodeUtils.normalize(carrier);
        log.info("📡 {} 공시지원금 조회... (planCode={}, joinType={}, planMonthlyFee={}, forceRefresh={})",
            normalizedCarrier, planCode, joinType, planMonthlyFee, forceRefresh);

        // 파라미터가 지정된 경우 항상 API 호출 (캐시는 기본 파라미터 기준)
        boolean hasCustomParams = (planCode != null && !planCode.isEmpty())
                               || (joinType != null && !joinType.isEmpty());

        List<CarrierSubsidy> subsidies;
        if (!forceRefresh && !hasCustomParams && subsidyCacheService.isCacheValid(normalizedCarrier)) {
            log.info("📂 캐시에서 {} 데이터 조회", normalizedCarrier);
            subsidies = subsidyCacheService.getFromCache(normalizedCarrier);
        } else {
            subsidies = fetchAndCacheCarrier(normalizedCarrier, planCode, joinType);
        }

        // 요금제 월 금액으로 필터링
        if (planMonthlyFee != null && !subsidies.isEmpty()) {
            log.info("🔍 요금제 월 금액 {}원으로 필터링...", planMonthlyFee);
            subsidies = subsidies.stream()
                .filter(s -> planMonthlyFee.equals(s.getPlanMonthlyFee()))
                .toList();
            log.info("📊 필터링 결과: {}건", subsidies.size());
        }

        return subsidies;
    }

    /**
     * 특정 기기의 통합 공시지원금 조회 (조건별 Lazy Cache 적용)
     *
     * 플로우:
     * 1. 캐시키 생성: carrier_deviceCode_joinType_planCode
     * 2. 캐시 확인 → 있으면 바로 반환
     * 3. 없으면 통신사 API 호출 → 캐시에 저장 → 반환
     */
    public UnifiedSubsidyResponse fetchSubsidiesByDevice(
            String sktDeviceCode,
            String ktDeviceCode,
            String lguDeviceCode) {

        return fetchSubsidiesByDeviceWithCondition(sktDeviceCode, ktDeviceCode, lguDeviceCode, null, null);
    }

    /**
     * 조건별 공시지원금 조회 (Lazy Cache)
     *
     * @param sktDeviceCode SKT 기기 코드
     * @param ktDeviceCode KT 기기 코드
     * @param lguDeviceCode LGU+ 기기 코드
     * @param joinType 가입유형 (null이면 전체)
     * @param planCode 요금제 코드 (null이면 전체)
     */
    public UnifiedSubsidyResponse fetchSubsidiesByDeviceWithCondition(
            String sktDeviceCode,
            String ktDeviceCode,
            String lguDeviceCode,
            String joinType,
            String planCode) {

        log.info("🔍 조건별 공시지원금 조회: SKT={}, KT={}, LGU+={}, joinType={}, planCode={}",
            sktDeviceCode, ktDeviceCode, lguDeviceCode, joinType, planCode);
        long startTime = System.currentTimeMillis();

        try {
            List<CarrierSubsidy> sktSubsidies = new ArrayList<>();
            List<CarrierSubsidy> ktSubsidies = new ArrayList<>();
            List<CarrierSubsidy> lguSubsidies = new ArrayList<>();

            // 각 통신사별 조건부 캐시 조회
            if (sktDeviceCode != null && !sktDeviceCode.isEmpty()) {
                sktSubsidies = fetchWithLazyCache("SKT", sktDeviceCode, joinType, planCode);
            }

            if (ktDeviceCode != null && !ktDeviceCode.isEmpty()) {
                ktSubsidies = fetchWithLazyCache("KT", ktDeviceCode, joinType, planCode);
            }

            if (lguDeviceCode != null && !lguDeviceCode.isEmpty()) {
                lguSubsidies = fetchWithLazyCache("LGU", lguDeviceCode, joinType, planCode);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ 조건별 조회 완료: SKT {}, KT {}, LGU+ {} ({}ms)",
                sktSubsidies.size(), ktSubsidies.size(), lguSubsidies.size(), elapsed);

            // 기기명 추출
            String deviceName = null;
            if (!sktSubsidies.isEmpty()) deviceName = sktSubsidies.get(0).getDeviceName();
            else if (!ktSubsidies.isEmpty()) deviceName = ktSubsidies.get(0).getDeviceName();
            else if (!lguSubsidies.isEmpty()) deviceName = lguSubsidies.get(0).getDeviceName();

            return UnifiedSubsidyResponse.builder()
                .success(true)
                .deviceCode(sktDeviceCode != null ? sktDeviceCode : (ktDeviceCode != null ? ktDeviceCode : lguDeviceCode))
                .deviceName(deviceName)
                .sktSubsidies(sktSubsidies)
                .ktSubsidies(ktSubsidies)
                .lguSubsidies(lguSubsidies)
                .elapsedMs(elapsed)
                .build();

        } catch (Exception e) {
            log.error("❌ 조건별 조회 실패: {}", e.getMessage());
            long elapsed = System.currentTimeMillis() - startTime;
            return UnifiedSubsidyResponse.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .elapsedMs(elapsed)
                .build();
        }
    }

    /**
     * Lazy Cache 조회 (조건별 1건씩)
     *
     * 필수 조건: joinType + planMonthlyFee(또는 planCode)가 반드시 있어야 함
     * 1. 캐시키로 시트 검색
     * 2. 있으면 → 시트에서 가져옴
     * 3. 없으면 → API 호출 → 해당 조건으로 필터 → 시트에 1건 저장 → 반환
     *
     * @param carrier 통신사
     * @param deviceCode 기기 코드
     * @param joinType 가입유형 (노피코드: 0301007001=신규, 0301007002=기기변경, 0301007003=번호이동 또는 한글)
     * @param planCodeOrFee 요금제 코드 또는 월정액 (숫자로 전달 시 월정액으로 필터링)
     */
    private List<CarrierSubsidy> fetchWithLazyCache(
            String carrier, String deviceCode, String joinType, String planCodeOrFee) {

        // 조건 검증: joinType, planCodeOrFee 필수
        if (joinType == null || joinType.isEmpty() || planCodeOrFee == null || planCodeOrFee.isEmpty()) {
            log.warn("⚠️ joinType과 planCode(또는 planMonthlyFee)는 필수입니다. carrier={}, deviceCode={}", carrier, deviceCode);
            return new ArrayList<>();
        }

        // 노피 코드 → 한글 가입유형 변환
        String normalizedJoinType = JoinType.toKorean(joinType);
        log.info("🔄 joinType 변환: {} → {}", joinType, normalizedJoinType);

        String cacheKey = subsidyCacheService.buildCacheKey(carrier, deviceCode, normalizedJoinType, planCodeOrFee);
        log.debug("🔑 캐시키: {}", cacheKey);

        // 1. 캐시 확인
        CarrierSubsidy cached = subsidyCacheService.getFromCacheByKey(cacheKey);
        if (cached != null) {
            log.info("⚡ 캐시 히트: {}", cacheKey);
            return Collections.singletonList(cached);
        }

        // 2. 캐시 미스 → API 호출
        log.info("🌐 캐시 미스, {} API 호출: deviceCode={}, joinType={}", carrier, deviceCode, normalizedJoinType);

        // planCodeOrFee에서 월정액 추출 (숫자인 경우)
        Integer targetMonthlyFee = null;
        try {
            targetMonthlyFee = Integer.parseInt(planCodeOrFee);
        } catch (NumberFormatException e) {
            // planCode로 사용
        }

        final Integer monthlyFeeForLgu = targetMonthlyFee;
        List<CarrierSubsidy> apiResults = switch (carrier) {
            case "SKT" -> sktSubsidyService.fetchSubsidiesByDevice(deviceCode);
            case "KT" -> ktSubsidyService.fetchSubsidiesByDevice(deviceCode);
            case "LGU" -> lguSubsidyService.fetchSubsidiesByDevice(deviceCode, normalizedJoinType, monthlyFeeForLgu);
            default -> new ArrayList<>();
        };

        log.info("📊 {} API 결과: {}건 (기기코드: {})", carrier, apiResults.size(), deviceCode);

        // planCodeOrFee가 숫자인지 확인 (월정액으로 필터링할지 결정) - 이미 위에서 파싱함
        final boolean useMonthlyFee = (monthlyFeeForLgu != null);
        final Integer monthlyFee = monthlyFeeForLgu;
        final String finalJoinType = normalizedJoinType;

        List<CarrierSubsidy> filteredResults = apiResults.stream()
            .filter(s -> {
                boolean joinTypeMatch = finalJoinType.equals(s.getJoinType());
                boolean planMatch;
                if (useMonthlyFee) {
                    // 월정액으로 필터링
                    planMatch = monthlyFee.equals(s.getPlanMonthlyFee());
                } else {
                    // planCode로 필터링
                    planMatch = planCodeOrFee.equals(s.getPlanCode());
                }
                return joinTypeMatch && planMatch;
            })
            .toList();

        log.info("🔍 필터링 결과: {}건 (joinType={}, planCodeOrFee={}, useMonthlyFee={})",
            filteredResults.size(), joinType, planCodeOrFee, useMonthlyFee);

        // 정확한 조건 매칭만 사용 (fallback 없음)

        // 3. 결과가 있으면 캐시에 저장 (1건)
        if (!filteredResults.isEmpty()) {
            CarrierSubsidy subsidy = filteredResults.get(0);
            subsidyCacheService.saveToCacheByKey(cacheKey, subsidy);
            log.info("💾 캐시 저장 완료: {}", cacheKey);
        } else {
            // 디버그용: API 결과의 joinType, planMonthlyFee 값 출력
            if (!apiResults.isEmpty()) {
                log.warn("⚠️ API 결과에서 조건에 맞는 데이터 없음: {} (조건: joinType={}, planCodeOrFee={})",
                    cacheKey, joinType, planCodeOrFee);
                log.debug("📝 API 결과 샘플 (처음 3개):");
                apiResults.stream().limit(3).forEach(s ->
                    log.debug("  - joinType={}, planCode={}, planMonthlyFee={}",
                        s.getJoinType(), s.getPlanCode(), s.getPlanMonthlyFee())
                );
            } else {
                log.warn("⚠️ {} API에서 기기코드 {} 결과 없음", carrier, deviceCode);
            }
        }

        return filteredResults;
    }

    /**
     * 노피 상품 코드로 매핑된 통신사 기기 공시지원금 조회
     */
    public UnifiedSubsidyResponse fetchSubsidiesByNofeeProduct(String nofeeProductCode) {
        log.info("🔍 노피 상품 {} 공시지원금 조회...", nofeeProductCode);
        long startTime = System.currentTimeMillis();

        try {
            // 1. Google Sheets에서 매핑 정보 조회
            List<DeviceMapping> allMappings = googleSheetsService.loadMappings();
            log.info("📊 매핑 데이터 {}건 로드됨", allMappings.size());

            // 2. 노피 상품 코드로 매핑 찾기
            DeviceMapping mapping = allMappings.stream()
                .filter(m -> nofeeProductCode.equals(m.getNofeeProductCode()))
                .findFirst()
                .orElse(null);

            if (mapping == null) {
                log.warn("⚠️ 노피 상품 코드 {}에 대한 매핑 없음", nofeeProductCode);
                long elapsed = System.currentTimeMillis() - startTime;
                return UnifiedSubsidyResponse.builder()
                    .success(false)
                    .errorMessage("노피 상품 코드 '" + nofeeProductCode + "'에 대한 통신사 기기 매핑이 없습니다.")
                    .elapsedMs(elapsed)
                    .build();
            }

            log.info("✅ 매핑 발견: {} -> SKT:{}, KT:{}, LGU:{}",
                mapping.getNofeeProductName(),
                mapping.getSktDeviceCode(),
                mapping.getKtDeviceCode(),
                mapping.getLguDeviceCode());

            // 3. 매핑된 통신사 기기 코드로 공시지원금 조회
            UnifiedSubsidyResponse response = fetchSubsidiesByDevice(
                mapping.getSktDeviceCode(),
                mapping.getKtDeviceCode(),
                mapping.getLguDeviceCode()
            );

            // 응답에 노피 상품 정보 추가
            long elapsed = System.currentTimeMillis() - startTime;
            return UnifiedSubsidyResponse.builder()
                .success(response.isSuccess())
                .deviceCode(nofeeProductCode)
                .deviceName(mapping.getNofeeProductName())
                .sktSubsidies(response.getSktSubsidies())
                .ktSubsidies(response.getKtSubsidies())
                .lguSubsidies(response.getLguSubsidies())
                .errorMessage(response.getErrorMessage())
                .elapsedMs(elapsed)
                .build();

        } catch (Exception e) {
            log.error("❌ 노피 상품 공시지원금 조회 실패: {}", e.getMessage(), e);
            long elapsed = System.currentTimeMillis() - startTime;
            return UnifiedSubsidyResponse.builder()
                .success(false)
                .errorMessage("노피 상품 공시지원금 조회 실패: " + e.getMessage())
                .elapsedMs(elapsed)
                .build();
        }
    }

    /**
     * 캐시 상태 조회
     */
    public Map<String, Object> getCacheStatus() {
        return subsidyCacheService.getCacheStatus();
    }

    /**
     * 캐시 초기화
     */
    public void clearCache() {
        subsidyCacheService.clearCache();
    }

    /**
     * 전체 캐시 강제 갱신 (모든 요금제 × 가입유형 조합)
     *
     * summary-plan 시트에 있는 모든 요금제와 가입유형의 조합을 조회
     * - SKT: joinType = 10(신규), 20(번호이동), 30(기기변경)
     * - KT: joinType = 01(신규), 02(번호이동), 04(기기변경)
     * - LGU: joinType = 1(기기변경), 2(번호이동), 3(신규)
     *
     * 순서: SKT 전체 완료 → KT 전체 완료 → LGU 전체 완료 (순차 실행)
     */
    public UnifiedSubsidyResponse refreshAllCache() {
        log.info("🔄 전체 캐시 강제 갱신 (모든 요금제 × 가입유형 조합)...");
        log.info("📋 순서: SKT → KT → LGU+ (각각 완료 후 다음 통신사 조회)");
        long startTime = System.currentTimeMillis();

        List<CarrierSubsidy> sktSubsidies = new ArrayList<>();
        List<CarrierSubsidy> ktSubsidies = new ArrayList<>();
        List<CarrierSubsidy> lguSubsidies = new ArrayList<>();

        // summary-plan 시트에서 통신사별 요금제 목록 조회
        List<CarrierPlan> sktPlans = planSheetService.getActivePlansByCarrier("SKT");
        List<CarrierPlan> ktPlans = planSheetService.getActivePlansByCarrier("KT");
        List<CarrierPlan> lguPlans = planSheetService.getActivePlansByCarrier("LGU");

        log.info("📋 요금제 로드 완료: SKT {}개, KT {}개, LGU+ {}개",
            sktPlans.size(), ktPlans.size(), lguPlans.size());

        // ==================== 1. SKT 조회 (순차) ====================
        log.info("========== [1/3] SKT 조회 시작 ==========");
        long sktStartTime = System.currentTimeMillis();
        try {
            String[] sktJoinTypes = {"10", "20", "30"}; // 신규, 번호이동, 기기변경
            if (sktPlans.isEmpty()) {
                log.warn("⚠️ SKT 요금제 없음 - 기본 요금제로 조회");
                for (String joinType : sktJoinTypes) {
                    List<CarrierSubsidy> results = sktSubsidyService.fetchAllSubsidies(null, joinType);
                    sktSubsidies.addAll(results);
                }
            } else {
                int totalCombinations = sktPlans.size() * sktJoinTypes.length;
                int currentIndex = 0;
                for (CarrierPlan plan : sktPlans) {
                    for (String joinType : sktJoinTypes) {
                        currentIndex++;
                        log.info("📡 SKT [{}/{}] 조회 중... (planCode={}, fee={}원, joinType={})",
                            currentIndex, totalCombinations, plan.getPlanCode(), plan.getMonthlyFee(), joinType);
                        try {
                            List<CarrierSubsidy> results = sktSubsidyService.fetchAllSubsidies(plan.getPlanCode(), joinType);
                            // planMonthlyFee 설정 (API 응답에 없을 수 있음)
                            for (CarrierSubsidy subsidy : results) {
                                if (subsidy.getPlanMonthlyFee() == null || subsidy.getPlanMonthlyFee() == 0) {
                                    subsidy.setPlanMonthlyFee(plan.getMonthlyFee());
                                }
                            }
                            sktSubsidies.addAll(results);
                            log.info("   ✓ SKT planCode={}, joinType={}: {}건 (누적: {}건)",
                                plan.getPlanCode(), joinType, results.size(), sktSubsidies.size());
                        } catch (Exception e) {
                            log.warn("   ⚠️ SKT planCode={}, joinType={} 조회 실패: {}",
                                plan.getPlanCode(), joinType, e.getMessage());
                        }
                    }
                }
            }
            long sktElapsed = System.currentTimeMillis() - sktStartTime;
            log.info("========== [1/3] SKT 조회 완료: {}건 ({}ms) ==========", sktSubsidies.size(), sktElapsed);
        } catch (Exception e) {
            log.error("❌ SKT 전체 조회 실패: {}", e.getMessage());
        }

        // ==================== 2. KT 조회 (순차) ====================
        log.info("========== [2/3] KT 조회 시작 ==========");
        long ktStartTime = System.currentTimeMillis();
        try {
            String[] ktJoinTypes = {"01", "02", "04"}; // 신규, 번호이동, 기기변경
            if (ktPlans.isEmpty()) {
                log.warn("⚠️ KT 요금제 없음 - 기본 요금제로 조회");
                for (String joinType : ktJoinTypes) {
                    List<CarrierSubsidy> results = ktSubsidyService.fetchAllSubsidies(null, joinType);
                    ktSubsidies.addAll(results);
                }
            } else {
                int totalCombinations = ktPlans.size() * ktJoinTypes.length;
                int currentIndex = 0;
                for (CarrierPlan plan : ktPlans) {
                    for (String joinType : ktJoinTypes) {
                        currentIndex++;
                        log.info("📡 KT [{}/{}] 조회 중... (planCode={}, fee={}원, joinType={})",
                            currentIndex, totalCombinations, plan.getPlanCode(), plan.getMonthlyFee(), joinType);
                        try {
                            List<CarrierSubsidy> results = ktSubsidyService.fetchAllSubsidies(plan.getPlanCode(), joinType);
                            // planMonthlyFee 설정
                            for (CarrierSubsidy subsidy : results) {
                                if (subsidy.getPlanMonthlyFee() == null || subsidy.getPlanMonthlyFee() == 0) {
                                    subsidy.setPlanMonthlyFee(plan.getMonthlyFee());
                                }
                            }
                            ktSubsidies.addAll(results);
                            log.info("   ✓ KT planCode={}, joinType={}: {}건 (누적: {}건)",
                                plan.getPlanCode(), joinType, results.size(), ktSubsidies.size());
                        } catch (Exception e) {
                            log.warn("   ⚠️ KT planCode={}, joinType={} 조회 실패: {}",
                                plan.getPlanCode(), joinType, e.getMessage());
                        }
                    }
                }
            }
            long ktElapsed = System.currentTimeMillis() - ktStartTime;
            log.info("========== [2/3] KT 조회 완료: {}건 ({}ms) ==========", ktSubsidies.size(), ktElapsed);
        } catch (Exception e) {
            log.error("❌ KT 전체 조회 실패: {}", e.getMessage());
        }

        // ==================== 3. LGU+ 조회 (순차) ====================
        log.info("========== [3/3] LGU+ 조회 시작 ==========");
        long lguStartTime = System.currentTimeMillis();
        try {
            String[] lguJoinTypes = {"1", "2", "3"}; // 기기변경, 번호이동, 신규
            if (lguPlans.isEmpty()) {
                log.warn("⚠️ LGU+ 요금제 없음 - 기본 요금제로 조회");
                for (String joinType : lguJoinTypes) {
                    List<CarrierSubsidy> results = lguSubsidyService.fetchAllSubsidies(null, joinType);
                    lguSubsidies.addAll(results);
                }
            } else {
                int totalCombinations = lguPlans.size() * lguJoinTypes.length;
                int currentIndex = 0;
                for (CarrierPlan plan : lguPlans) {
                    for (String joinType : lguJoinTypes) {
                        currentIndex++;
                        log.info("📡 LGU+ [{}/{}] 조회 중... (planCode={}, fee={}원, joinType={})",
                            currentIndex, totalCombinations, plan.getPlanCode(), plan.getMonthlyFee(), joinType);
                        try {
                            List<CarrierSubsidy> results = lguSubsidyService.fetchAllSubsidies(plan.getPlanCode(), joinType);
                            // planMonthlyFee 설정
                            for (CarrierSubsidy subsidy : results) {
                                if (subsidy.getPlanMonthlyFee() == null || subsidy.getPlanMonthlyFee() == 0) {
                                    subsidy.setPlanMonthlyFee(plan.getMonthlyFee());
                                }
                            }
                            lguSubsidies.addAll(results);
                            log.info("   ✓ LGU+ planCode={}, joinType={}: {}건 (누적: {}건)",
                                plan.getPlanCode(), joinType, results.size(), lguSubsidies.size());
                        } catch (Exception e) {
                            log.warn("   ⚠️ LGU+ planCode={}, joinType={} 조회 실패: {}",
                                plan.getPlanCode(), joinType, e.getMessage());
                        }
                    }
                }
            }
            long lguElapsed = System.currentTimeMillis() - lguStartTime;
            log.info("========== [3/3] LGU+ 조회 완료: {}건 ({}ms) ==========", lguSubsidies.size(), lguElapsed);
        } catch (Exception e) {
            log.error("❌ LGU+ 전체 조회 실패: {}", e.getMessage());
        }

        // ==================== 4. 캐시 저장 ====================
        List<CarrierSubsidy> allSubsidies = new ArrayList<>();
        allSubsidies.addAll(sktSubsidies);
        allSubsidies.addAll(ktSubsidies);
        allSubsidies.addAll(lguSubsidies);

        int savedCount = 0;
        if (!allSubsidies.isEmpty()) {
            savedCount = subsidyCacheService.saveAllToCache(allSubsidies);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("✅ 전체 캐시 갱신 완료: SKT {}, KT {}, LGU+ {} → 시트에 {}건 저장 (총 {}ms)",
            sktSubsidies.size(), ktSubsidies.size(), lguSubsidies.size(), savedCount, elapsed);

        return UnifiedSubsidyResponse.builder()
            .success(!allSubsidies.isEmpty())
            .sktSubsidies(sktSubsidies)
            .ktSubsidies(ktSubsidies)
            .lguSubsidies(lguSubsidies)
            .elapsedMs(elapsed)
            .build();
    }

    /**
     * 증분 업데이트 (공시일 최근 7일 데이터만 조회)
     *
     * 기존 캐시 데이터가 있는 상태에서 최근 공시일 데이터만 조회해서 업데이트
     * - 처음 실행 시: 전체 데이터 조회 (refreshAllCache와 동일)
     * - 이후 실행 시: 공시일이 최근 7일인 데이터만 조회해서 기존 데이터와 병합
     *
     * 순서: SKT 전체 완료 → KT 전체 완료 → LGU 전체 완료 (순차 실행)
     *
     * @param days 최근 며칠 데이터를 조회할지 (기본 7일)
     */
    public UnifiedSubsidyResponse incrementalUpdate(int days) {
        log.info("🔄 증분 업데이트 시작 (최근 {}일 공시일 데이터)...", days);
        log.info("📋 순서: SKT → KT → LGU+ (각각 완료 후 다음 통신사 조회)");
        long startTime = System.currentTimeMillis();

        // 1. 기존 캐시 데이터 확인
        List<CarrierSubsidy> existingData = subsidyCacheService.getAllFromCache();
        boolean isFirstRun = existingData.isEmpty();

        if (isFirstRun) {
            log.info("📂 기존 캐시 없음 - 전체 데이터 조회 실행");
            return refreshAllCache();
        }

        log.info("📂 기존 캐시 데이터: {}건 - 증분 업데이트 실행", existingData.size());

        // 2. 최근 N일 기준 날짜 계산
        java.time.LocalDate cutoffDate = java.time.LocalDate.now().minusDays(days);
        String cutoffDateStr = cutoffDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("📅 공시일 기준: {} 이후", cutoffDateStr);

        List<CarrierSubsidy> sktRecentSubsidies = new ArrayList<>();
        List<CarrierSubsidy> ktRecentSubsidies = new ArrayList<>();
        List<CarrierSubsidy> lguRecentSubsidies = new ArrayList<>();

        // summary-plan 시트에서 통신사별 요금제 목록 조회
        List<CarrierPlan> sktPlans = planSheetService.getActivePlansByCarrier("SKT");
        List<CarrierPlan> ktPlans = planSheetService.getActivePlansByCarrier("KT");
        List<CarrierPlan> lguPlans = planSheetService.getActivePlansByCarrier("LGU");

        log.info("📋 요금제 로드 완료: SKT {}개, KT {}개, LGU+ {}개",
            sktPlans.size(), ktPlans.size(), lguPlans.size());

        // ==================== 1. SKT 조회 (순차) ====================
        log.info("========== [1/3] SKT 증분 조회 시작 ==========");
        long sktStartTime = System.currentTimeMillis();
        try {
            String[] sktJoinTypes = {"10", "20", "30"};
            int totalCombinations = sktPlans.size() * sktJoinTypes.length;
            int currentIndex = 0;
            for (CarrierPlan plan : sktPlans) {
                for (String joinType : sktJoinTypes) {
                    currentIndex++;
                    log.info("📡 SKT [{}/{}] 조회 중... (planCode={}, fee={}원, joinType={})",
                        currentIndex, totalCombinations, plan.getPlanCode(), plan.getMonthlyFee(), joinType);
                    try {
                        List<CarrierSubsidy> results = sktSubsidyService.fetchAllSubsidies(plan.getPlanCode(), joinType);
                        int recentCount = 0;
                        for (CarrierSubsidy subsidy : results) {
                            if (isRecentAnnounceDate(subsidy.getAnnounceDate(), cutoffDateStr)) {
                                if (subsidy.getPlanMonthlyFee() == null || subsidy.getPlanMonthlyFee() == 0) {
                                    subsidy.setPlanMonthlyFee(plan.getMonthlyFee());
                                }
                                sktRecentSubsidies.add(subsidy);
                                recentCount++;
                            }
                        }
                        log.info("   ✓ SKT planCode={}, joinType={}: {}건 중 최근 {}건 (누적: {}건)",
                            plan.getPlanCode(), joinType, results.size(), recentCount, sktRecentSubsidies.size());
                    } catch (Exception e) {
                        log.warn("   ⚠️ SKT planCode={}, joinType={} 조회 실패: {}",
                            plan.getPlanCode(), joinType, e.getMessage());
                    }
                }
            }
            long sktElapsed = System.currentTimeMillis() - sktStartTime;
            log.info("========== [1/3] SKT 증분 조회 완료: {}건 ({}ms) ==========", sktRecentSubsidies.size(), sktElapsed);
        } catch (Exception e) {
            log.error("❌ SKT 증분 조회 실패: {}", e.getMessage());
        }

        // ==================== 2. KT 조회 (순차) ====================
        log.info("========== [2/3] KT 증분 조회 시작 ==========");
        long ktStartTime = System.currentTimeMillis();
        try {
            String[] ktJoinTypes = {"01", "02", "04"};
            int totalCombinations = ktPlans.size() * ktJoinTypes.length;
            int currentIndex = 0;
            for (CarrierPlan plan : ktPlans) {
                for (String joinType : ktJoinTypes) {
                    currentIndex++;
                    log.info("📡 KT [{}/{}] 조회 중... (planCode={}, fee={}원, joinType={})",
                        currentIndex, totalCombinations, plan.getPlanCode(), plan.getMonthlyFee(), joinType);
                    try {
                        List<CarrierSubsidy> results = ktSubsidyService.fetchAllSubsidies(plan.getPlanCode(), joinType);
                        int recentCount = 0;
                        for (CarrierSubsidy subsidy : results) {
                            if (isRecentAnnounceDate(subsidy.getAnnounceDate(), cutoffDateStr)) {
                                if (subsidy.getPlanMonthlyFee() == null || subsidy.getPlanMonthlyFee() == 0) {
                                    subsidy.setPlanMonthlyFee(plan.getMonthlyFee());
                                }
                                ktRecentSubsidies.add(subsidy);
                                recentCount++;
                            }
                        }
                        log.info("   ✓ KT planCode={}, joinType={}: {}건 중 최근 {}건 (누적: {}건)",
                            plan.getPlanCode(), joinType, results.size(), recentCount, ktRecentSubsidies.size());
                    } catch (Exception e) {
                        log.warn("   ⚠️ KT planCode={}, joinType={} 조회 실패: {}",
                            plan.getPlanCode(), joinType, e.getMessage());
                    }
                }
            }
            long ktElapsed = System.currentTimeMillis() - ktStartTime;
            log.info("========== [2/3] KT 증분 조회 완료: {}건 ({}ms) ==========", ktRecentSubsidies.size(), ktElapsed);
        } catch (Exception e) {
            log.error("❌ KT 증분 조회 실패: {}", e.getMessage());
        }

        // ==================== 3. LGU+ 조회 (순차) ====================
        log.info("========== [3/3] LGU+ 증분 조회 시작 ==========");
        long lguStartTime = System.currentTimeMillis();
        try {
            String[] lguJoinTypes = {"1", "2", "3"};
            int totalCombinations = lguPlans.size() * lguJoinTypes.length;
            int currentIndex = 0;
            for (CarrierPlan plan : lguPlans) {
                for (String joinType : lguJoinTypes) {
                    currentIndex++;
                    log.info("📡 LGU+ [{}/{}] 조회 중... (planCode={}, fee={}원, joinType={})",
                        currentIndex, totalCombinations, plan.getPlanCode(), plan.getMonthlyFee(), joinType);
                    try {
                        List<CarrierSubsidy> results = lguSubsidyService.fetchAllSubsidies(plan.getPlanCode(), joinType);
                        int recentCount = 0;
                        for (CarrierSubsidy subsidy : results) {
                            if (isRecentAnnounceDate(subsidy.getAnnounceDate(), cutoffDateStr)) {
                                if (subsidy.getPlanMonthlyFee() == null || subsidy.getPlanMonthlyFee() == 0) {
                                    subsidy.setPlanMonthlyFee(plan.getMonthlyFee());
                                }
                                lguRecentSubsidies.add(subsidy);
                                recentCount++;
                            }
                        }
                        log.info("   ✓ LGU+ planCode={}, joinType={}: {}건 중 최근 {}건 (누적: {}건)",
                            plan.getPlanCode(), joinType, results.size(), recentCount, lguRecentSubsidies.size());
                    } catch (Exception e) {
                        log.warn("   ⚠️ LGU+ planCode={}, joinType={} 조회 실패: {}",
                            plan.getPlanCode(), joinType, e.getMessage());
                    }
                }
            }
            long lguElapsed = System.currentTimeMillis() - lguStartTime;
            log.info("========== [3/3] LGU+ 증분 조회 완료: {}건 ({}ms) ==========", lguRecentSubsidies.size(), lguElapsed);
        } catch (Exception e) {
            log.error("❌ LGU+ 증분 조회 실패: {}", e.getMessage());
        }

        // ==================== 4. 증분 업데이트 ====================
        List<CarrierSubsidy> recentSubsidies = new ArrayList<>();
        recentSubsidies.addAll(sktRecentSubsidies);
        recentSubsidies.addAll(ktRecentSubsidies);
        recentSubsidies.addAll(lguRecentSubsidies);

        int updatedCount = 0;
        if (!recentSubsidies.isEmpty()) {
            updatedCount = subsidyCacheService.updateCacheIncremental(recentSubsidies);
        }

        // 5. 결과 반환
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("✅ 증분 업데이트 완료: SKT {}건, KT {}건, LGU+ {}건 → {}건 업데이트/추가 (총 {}ms)",
            sktRecentSubsidies.size(), ktRecentSubsidies.size(), lguRecentSubsidies.size(), updatedCount, elapsed);

        // 업데이트된 전체 데이터 반환
        List<CarrierSubsidy> allData = subsidyCacheService.getAllFromCache();
        List<CarrierSubsidy> sktSubsidies = allData.stream().filter(s -> "SKT".equals(s.getCarrier())).toList();
        List<CarrierSubsidy> ktSubsidies = allData.stream().filter(s -> "KT".equals(s.getCarrier())).toList();
        List<CarrierSubsidy> lguSubsidies = allData.stream().filter(s -> "LGU".equals(s.getCarrier())).toList();

        return UnifiedSubsidyResponse.builder()
            .success(true)
            .sktSubsidies(sktSubsidies)
            .ktSubsidies(ktSubsidies)
            .lguSubsidies(lguSubsidies)
            .elapsedMs(elapsed)
            .build();
    }

    /**
     * 공시일이 기준일 이후인지 확인
     * @param announceDate 공시일 (YYYY-MM-DD 형식)
     * @param cutoffDate 기준일 (YYYY-MM-DD 형식)
     */
    private boolean isRecentAnnounceDate(String announceDate, String cutoffDate) {
        if (announceDate == null || announceDate.isEmpty()) {
            // 공시일 정보가 없으면 포함 (안전하게)
            return true;
        }
        try {
            // 문자열 비교로 날짜 비교 (YYYY-MM-DD 형식은 문자열 비교 가능)
            return announceDate.compareTo(cutoffDate) >= 0;
        } catch (Exception e) {
            log.debug("공시일 비교 실패: {} vs {}", announceDate, cutoffDate);
            return true; // 비교 실패 시 포함
        }
    }

    // ==================== 기기 목록 조회 API ====================

    /**
     * SKT 기기 목록 조회
     */
    public List<CarrierDevice> fetchSktDevices() {
        log.info("📱 SKT 기기 목록 조회...");
        return sktDeviceService.fetchDevices();
    }

    /**
     * KT 기기 목록 조회
     */
    public List<CarrierDevice> fetchKtDevices() {
        log.info("📱 KT 기기 목록 조회...");
        return ktDeviceService.fetchDevices();
    }

    /**
     * LGU+ 기기 목록 조회
     */
    public List<CarrierDevice> fetchLguDevices() {
        log.info("📱 LGU+ 기기 목록 조회...");
        return lguDeviceService.fetchDevices();
    }

    /**
     * 모든 통신사 기기 목록 조회 (병렬)
     */
    public DeviceListResponse fetchAllDevices() {
        log.info("📱 전체 통신사 기기 목록 조회...");
        long startTime = System.currentTimeMillis();

        try {
            CompletableFuture<List<CarrierDevice>> sktFuture =
                CompletableFuture.supplyAsync(() -> sktDeviceService.fetchDevices());
            CompletableFuture<List<CarrierDevice>> ktFuture =
                CompletableFuture.supplyAsync(() -> ktDeviceService.fetchDevices());
            CompletableFuture<List<CarrierDevice>> lguFuture =
                CompletableFuture.supplyAsync(() -> lguDeviceService.fetchDevices());

            CompletableFuture.allOf(sktFuture, ktFuture, lguFuture).join();

            List<CarrierDevice> sktDevices = sktFuture.get();
            List<CarrierDevice> ktDevices = ktFuture.get();
            List<CarrierDevice> lguDevices = lguFuture.get();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ 기기 목록 조회 완료: SKT {}, KT {}, LGU+ {} ({}ms)",
                sktDevices.size(), ktDevices.size(), lguDevices.size(), elapsed);

            return new DeviceListResponse(sktDevices, ktDevices, lguDevices, elapsed);

        } catch (Exception e) {
            log.error("❌ 기기 목록 조회 실패: {}", e.getMessage());
            return new DeviceListResponse(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0);
        }
    }

    /**
     * 기기 목록 응답
     */
    public record DeviceListResponse(
        List<CarrierDevice> sktDevices,
        List<CarrierDevice> ktDevices,
        List<CarrierDevice> lguDevices,
        long elapsedMs
    ) {}

}
