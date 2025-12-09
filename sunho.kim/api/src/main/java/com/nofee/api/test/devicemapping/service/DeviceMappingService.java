package com.nofee.api.test.devicemapping.service;

import com.nofee.api.test.carrierintegration.dto.UnifiedSubsidyResponse;
import com.nofee.api.test.carrierintegration.service.CarrierIntegrationService;
import com.nofee.api.test.devicemapping.dto.CarrierDevice;
import com.nofee.api.test.devicemapping.dto.DeviceMapping;
import com.nofee.api.test.devicemapping.dto.ModelInfo;
import com.nofee.api.test.devicemapping.dto.NofeeProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 기기 매핑 서비스
 *
 * 1. 노피 DB에서 활성 상품 조회
 * 2. 각 통신사 API에서 기기 리스트 가져오기
 * 3. Rule-based 매핑 결정
 * 4. Google Sheets에 저장/조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceMappingService {

    private final JdbcTemplate jdbcTemplate;
    private final GoogleSheetsService googleSheetsService;
    private final CarrierIntegrationService carrierIntegrationService;

    /**
     * 노피 DB에서 활성 상품 조회
     */
    public List<NofeeProduct> fetchNofeeProducts() {
        log.info("📦 노피 DB에서 상품 조회 중...");

        String sql = """
            SELECT
                product_group_code as productGroupCode,
                product_group_nm as productGroupNm,
                manufacturer_code as manufacturerCode
            FROM tb_product_group_phone
            WHERE state_code = '0204002'
              AND deleted_yn = 'N'
            ORDER BY created_at DESC
            """;

        List<NofeeProduct> products = jdbcTemplate.query(sql, (rs, rowNum) ->
            NofeeProduct.builder()
                .productGroupCode(rs.getString("productGroupCode"))
                .productGroupNm(rs.getString("productGroupNm"))
                .manufacturerCode(rs.getString("manufacturerCode"))
                .build()
        );

        log.info("✅ {}개 상품 조회 완료", products.size());
        return products;
    }

    /**
     * 정확한 모델 매칭 기반 후보 검색
     */
    private List<CarrierDevice> findCandidates(String productName, List<CarrierDevice> devices) {
        ModelInfo productInfo = ModelInfo.extract(productName);

        // 브랜드가 OTHER이거나 시리즈 추출 실패 시 키워드 매칭 fallback
        if (productInfo.getBrand() == ModelInfo.Brand.OTHER || productInfo.getSeries() == null) {
            String lower = productName.toLowerCase()
                .replaceAll("갤럭시|아이폰", "")
                .trim();
            return devices.stream()
                .filter(d -> d.getDeviceName().toLowerCase().contains(lower))
                .collect(Collectors.toList());
        }

        // 정확한 모델 매칭
        return devices.stream()
            .filter(d -> {
                ModelInfo candidateInfo = ModelInfo.extract(d.getDeviceName());
                return productInfo.isExactMatch(candidateInfo);
            })
            .collect(Collectors.toList());
    }

    /**
     * 저장용량 기준 오름차순 정렬
     */
    private List<CarrierDevice> sortByLowestStorage(List<CarrierDevice> devices) {
        return devices.stream()
            .sorted(Comparator.comparingInt(d -> CarrierDevice.parseStorageToGB(d.getStorage())))
            .collect(Collectors.toList());
    }

    /**
     * Rule-based 매핑 수행
     */
    public List<DeviceMapping> matchWithRules(
            List<NofeeProduct> nofeeProducts,
            List<CarrierDevice> sktDevices,
            List<CarrierDevice> ktDevices,
            List<CarrierDevice> lguDevices) {

        log.info("🔧 Rule-based 매핑 중...");
        List<DeviceMapping> mappings = new ArrayList<>();
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        for (NofeeProduct product : nofeeProducts) {
            // 각 통신사별로 후보 찾기 + 최저 용량 정렬
            List<CarrierDevice> sktCandidates = sortByLowestStorage(findCandidates(product.getProductGroupNm(), sktDevices));
            List<CarrierDevice> ktCandidates = sortByLowestStorage(findCandidates(product.getProductGroupNm(), ktDevices));
            List<CarrierDevice> lguCandidates = sortByLowestStorage(findCandidates(product.getProductGroupNm(), lguDevices));

            // 첫 번째 후보 선택 (최저 용량)
            CarrierDevice sktMatch = sktCandidates.isEmpty() ? null : sktCandidates.get(0);
            CarrierDevice ktMatch = ktCandidates.isEmpty() ? null : ktCandidates.get(0);
            CarrierDevice lguMatch = lguCandidates.isEmpty() ? null : lguCandidates.get(0);

            // 신뢰도 결정
            int matchCount = (sktMatch != null ? 1 : 0) + (ktMatch != null ? 1 : 0) + (lguMatch != null ? 1 : 0);
            String confidence;
            if (matchCount == 3) {
                confidence = "high";
            } else if (matchCount >= 1) {
                confidence = "medium";
            } else {
                confidence = "low";
            }

            mappings.add(DeviceMapping.builder()
                .nofeeProductCode(product.getProductGroupCode())
                .nofeeProductName(product.getProductGroupNm())
                .sktDeviceCode(sktMatch != null ? sktMatch.getDeviceCode() : null)
                .sktDeviceName(sktMatch != null ? sktMatch.getDeviceName() : null)
                .ktDeviceCode(ktMatch != null ? ktMatch.getDeviceCode() : null)
                .ktDeviceName(ktMatch != null ? ktMatch.getDeviceName() : null)
                .lguDeviceCode(lguMatch != null ? lguMatch.getDeviceCode() : null)
                .lguDeviceName(lguMatch != null ? lguMatch.getDeviceName() : null)
                .mappedAt(now)
                .confidence(confidence)
                .build());
        }

        log.info("✅ {}개 매핑 완료", mappings.size());
        return mappings;
    }

    /**
     * 전체 동기화 수행
     */
    public SyncResult syncMappings() {
        log.info("🚀 동기화 시작");
        long startTime = System.currentTimeMillis();

        // 1. 노피 상품 조회
        List<NofeeProduct> nofeeProducts = fetchNofeeProducts();
        if (nofeeProducts.isEmpty()) {
            return new SyncResult(0, 0);
        }

        // 2. 통신사 기기 조회 (CarrierIntegrationService를 통해 조회)
        CarrierIntegrationService.DeviceListResponse deviceListResponse = carrierIntegrationService.fetchAllDevices();
        List<CarrierDevice> sktDevices = deviceListResponse.sktDevices();
        List<CarrierDevice> ktDevices = deviceListResponse.ktDevices();
        List<CarrierDevice> lguDevices = deviceListResponse.lguDevices();

        log.info("📊 조회 결과: 노피 {}, SKT {}, KT {}, LGU+ {}",
            nofeeProducts.size(), sktDevices.size(), ktDevices.size(), lguDevices.size());

        // 3. Rule-based 매핑
        List<DeviceMapping> mappings = matchWithRules(nofeeProducts, sktDevices, ktDevices, lguDevices);

        // 4. Google Sheets에 저장
        googleSheetsService.saveMappings(mappings);

        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
        log.info("✅ 동기화 완료! ({} 초)", String.format("%.1f", elapsed));

        return new SyncResult(mappings.size(), elapsed);
    }

    /**
     * 모든 매핑 조회
     */
    public List<DeviceMapping> getMappings() {
        return googleSheetsService.loadMappings();
    }

    /**
     * 특정 상품 코드로 매핑 조회
     */
    public DeviceMapping getMappingByCode(String nofeeCode) {
        return googleSheetsService.loadMappings().stream()
            .filter(m -> m.getNofeeProductCode().equals(nofeeCode))
            .findFirst()
            .orElse(null);
    }

    /**
     * 매핑 직접 추가 (테스트용)
     */
    public void addMapping(DeviceMapping mapping) {
        log.info("📝 매핑 추가: {} -> SKT:{}, KT:{}, LGU:{}",
            mapping.getNofeeProductCode(),
            mapping.getSktDeviceCode(),
            mapping.getKtDeviceCode(),
            mapping.getLguDeviceCode());

        // 기존 매핑 로드
        List<DeviceMapping> existingMappings = googleSheetsService.loadMappings();

        // 중복 제거 (같은 nofeeProductCode가 있으면 덮어씀)
        List<DeviceMapping> updatedMappings = existingMappings.stream()
            .filter(m -> !m.getNofeeProductCode().equals(mapping.getNofeeProductCode()))
            .collect(Collectors.toList());

        // 매핑 시간 설정
        if (mapping.getMappedAt() == null) {
            mapping.setMappedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        updatedMappings.add(mapping);

        // 저장
        googleSheetsService.saveMappings(updatedMappings);
        log.info("✅ 매핑 저장 완료 (총 {}건)", updatedMappings.size());
    }

    /**
     * 동기화 결과
     */
    public record SyncResult(int count, double elapsed) {}

    /**
     * 노피 상품 코드로 매핑된 통신사 기기의 공시지원금 조회
     */
    public UnifiedSubsidyResponse getSubsidiesByNofeeProductCode(String nofeeProductCode) {
        log.info("🔍 노피 상품 {} 공시지원금 조회...", nofeeProductCode);

        // 매핑 정보 조회
        DeviceMapping mapping = getMappingByCode(nofeeProductCode);
        if (mapping == null) {
            log.warn("⚠️ 매핑 정보 없음: {}", nofeeProductCode);
            return UnifiedSubsidyResponse.builder()
                .success(false)
                .errorMessage("매핑 정보를 찾을 수 없습니다: " + nofeeProductCode)
                .build();
        }

        log.info("📋 매핑 정보: SKT={}, KT={}, LGU+={}",
            mapping.getSktDeviceCode(), mapping.getKtDeviceCode(), mapping.getLguDeviceCode());

        // CarrierIntegrationService를 통해 공시지원금 조회
        UnifiedSubsidyResponse response = carrierIntegrationService.fetchSubsidiesByDevice(
            mapping.getSktDeviceCode(),
            mapping.getKtDeviceCode(),
            mapping.getLguDeviceCode()
        );

        // 응답에 노피 상품 정보 추가
        if (response.isSuccess()) {
            response.setDeviceCode(nofeeProductCode);
            response.setDeviceName(mapping.getNofeeProductName());
        }

        return response;
    }

    /**
     * 전체 통신사 공시지원금 조회 (캐시 우선)
     */
    public UnifiedSubsidyResponse getAllSubsidies() {
        return carrierIntegrationService.fetchAllSubsidies();
    }

    /**
     * 전체 통신사 공시지원금 조회 (캐시 강제 갱신)
     */
    public UnifiedSubsidyResponse refreshAllSubsidies() {
        return carrierIntegrationService.refreshAllCache();
    }
}
