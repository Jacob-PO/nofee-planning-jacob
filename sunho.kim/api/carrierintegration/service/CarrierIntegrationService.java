package com.nofee.api.test.carrierintegration.service;

import com.nofee.api.test.carrierintegration.dto.CarrierSubsidy;
import com.nofee.api.test.carrierintegration.dto.UnifiedSubsidyResponse;
import com.nofee.api.test.devicemapping.dto.CarrierDevice;
import com.nofee.api.test.devicemapping.service.carrier.KtDeviceService;
import com.nofee.api.test.devicemapping.service.carrier.LguDeviceService;
import com.nofee.api.test.devicemapping.service.carrier.SktDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        String normalizedCarrier = normalizeCarrier(carrier);
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
        String normalizedCarrier = normalizeCarrier(carrier);
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
     * 통신사명 정규화
     */
    private String normalizeCarrier(String carrier) {
        return switch (carrier.toUpperCase()) {
            case "SKT" -> "SKT";
            case "KT" -> "KT";
            case "LGU", "LGU+", "LG U+", "LGUPLUS" -> "LGU";
            default -> carrier.toUpperCase();
        };
    }

    /**
     * 특정 기기의 통합 공시지원금 조회
     */
    public UnifiedSubsidyResponse fetchSubsidiesByDevice(
            String sktDeviceCode,
            String ktDeviceCode,
            String lguDeviceCode) {

        log.info("🔍 기기별 공시지원금 조회: SKT={}, KT={}, LGU+={}",
            sktDeviceCode, ktDeviceCode, lguDeviceCode);
        long startTime = System.currentTimeMillis();

        try {
            // 먼저 캐시에서 전체 데이터 가져온 후 필터링
            List<CarrierSubsidy> sktSubsidies = new ArrayList<>();
            List<CarrierSubsidy> ktSubsidies = new ArrayList<>();
            List<CarrierSubsidy> lguSubsidies = new ArrayList<>();

            if (sktDeviceCode != null) {
                List<CarrierSubsidy> allSkt = fetchSubsidiesByCarrier("SKT");
                sktSubsidies = allSkt.stream()
                    .filter(s -> sktDeviceCode.equals(s.getDeviceCode()))
                    .toList();
            }

            if (ktDeviceCode != null) {
                List<CarrierSubsidy> allKt = fetchSubsidiesByCarrier("KT");
                ktSubsidies = allKt.stream()
                    .filter(s -> ktDeviceCode.equals(s.getDeviceCode()))
                    .toList();
            }

            if (lguDeviceCode != null) {
                List<CarrierSubsidy> allLgu = fetchSubsidiesByCarrier("LGU");
                lguSubsidies = allLgu.stream()
                    .filter(s -> lguDeviceCode.equals(s.getDeviceCode()))
                    .toList();
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ 기기별 조회 완료: SKT {}, KT {}, LGU+ {} ({}ms)",
                sktSubsidies.size(), ktSubsidies.size(), lguSubsidies.size(), elapsed);

            // 기기명 추출 (첫 번째 결과에서)
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
            log.error("❌ 기기별 조회 실패: {}", e.getMessage());
            long elapsed = System.currentTimeMillis() - startTime;
            return UnifiedSubsidyResponse.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .elapsedMs(elapsed)
                .build();
        }
    }

    /**
     * 노피 상품 코드로 매핑된 통신사 기기 공시지원금 조회
     */
    public UnifiedSubsidyResponse fetchSubsidiesByNofeeProduct(String nofeeProductCode) {
        log.info("🔍 노피 상품 {} 공시지원금 조회...", nofeeProductCode);

        // TODO: device-mapping 서비스에서 매핑 정보 조회 후 각 통신사 코드로 조회
        // 현재는 placeholder - 실제 구현 시 DeviceMappingService 연동 필요

        return UnifiedSubsidyResponse.builder()
            .success(false)
            .errorMessage("노피 상품 코드 매핑 조회는 아직 구현되지 않았습니다. DeviceMappingService 연동 필요.")
            .build();
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
     * 전체 캐시 강제 갱신
     */
    public UnifiedSubsidyResponse refreshAllCache() {
        log.info("🔄 전체 캐시 강제 갱신...");
        return fetchAllSubsidiesWithCache(true);
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
