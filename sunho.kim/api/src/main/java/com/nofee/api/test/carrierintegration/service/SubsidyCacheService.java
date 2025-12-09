package com.nofee.api.test.carrierintegration.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.nofee.api.test.carrierintegration.dto.CarrierSubsidy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 공시지원금 캐시 서비스 (조건별 Lazy Cache)
 *
 * Google Sheets를 임시 DB로 활용
 * - 조회 조건: carrier + deviceCode + joinType + planCode
 * - 첫 조회 시 API 호출 후 해당 1건만 시트에 추가 (append)
 * - 동일 조건 재조회 시 시트에서 가져옴
 * - 24시간 경과 시 해당 조건만 갱신
 */
@Slf4j
@Service
public class SubsidyCacheService {

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    @Value("${google.sheets.subsidy-sheet-name}")
    private String sheetName;

    @Value("${google.sheets.credentials-path}")
    private String credentialsPath;

    @Value("${google.sheets.cache-ttl-hours}")
    private int cacheTtlHours;

    private Sheets sheetsService;

    // 메모리 캐시: 조건별 캐시 시간 (carrier_deviceCode_joinType_planCode -> 캐시 시간)
    private final Map<String, LocalDateTime> cacheTimeMap = new ConcurrentHashMap<>();

    // 메모리 캐시: 조건별 데이터 (빠른 조회용)
    private final Map<String, CarrierSubsidy> memoryCacheMap = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 헤더 정의 (cacheKey, announceDate 컬럼 추가)
    private static final List<Object> HEADERS = Arrays.asList(
        "cacheKey", "carrier", "joinType", "discountType", "deviceName", "deviceCode",
        "storage", "color", "planName", "planCode", "planMonthlyFee", "planMaintainMonth",
        "msrp", "carrierSubsidy", "additionalSubsidy", "installmentPrice", "announceDate", "cachedAt"
    );

    @PostConstruct
    public void init() {
        if (credentialsPath == null || credentialsPath.isEmpty()) {
            log.warn("⚠️ Google Sheets 설정 없음 - 캐시 기능 비활성화");
            return;
        }

        try {
            GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(credentialsPath))
                .createScoped(Collections.singletonList(SheetsScopes.SPREADSHEETS));

            sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Nofee Subsidy Cache")
                .build();

            log.info("✅ 공시지원금 캐시 서비스 초기화 완료 (TTL: {}시간)", cacheTtlHours);
        } catch (Exception e) {
            log.error("❌ Google Sheets 초기화 오류: {}", e.getMessage());
        }
    }

    // ==================== 조건별 Lazy Cache 메서드 ====================

    /**
     * 캐시 키 생성
     * @param carrier 통신사 (SKT, KT, LGU)
     * @param deviceCode 기기 코드
     * @param joinType 가입유형 (신규, 기기변경, 번호이동)
     * @param planCode 요금제 코드
     */
    public String buildCacheKey(String carrier, String deviceCode, String joinType, String planCode) {
        return String.format("%s_%s_%s_%s",
            nvl(carrier), nvl(deviceCode), nvl(joinType), nvl(planCode));
    }

    /**
     * 조건별 캐시 유효성 확인
     */
    public boolean isCacheValidByKey(String cacheKey) {
        if (sheetsService == null) return false;

        // 1. 메모리 캐시 확인
        LocalDateTime cachedAt = cacheTimeMap.get(cacheKey);
        if (cachedAt != null) {
            long hours = ChronoUnit.HOURS.between(cachedAt, LocalDateTime.now());
            if (hours < cacheTtlHours) {
                log.debug("📂 메모리 캐시 유효: {} ({}시간 경과)", cacheKey, hours);
                return true;
            }
        }

        // 2. 시트에서 확인
        CarrierSubsidy cached = getFromCacheByKey(cacheKey);
        if (cached != null) {
            memoryCacheMap.put(cacheKey, cached);
            cacheTimeMap.put(cacheKey, LocalDateTime.now());
            return true;
        }

        return false;
    }

    /**
     * 조건별 캐시에서 1건 조회
     */
    public CarrierSubsidy getFromCacheByKey(String cacheKey) {
        // 1. 메모리 캐시 우선
        CarrierSubsidy memoryCached = memoryCacheMap.get(cacheKey);
        if (memoryCached != null) {
            log.debug("⚡ 메모리 캐시 히트: {}", cacheKey);
            return memoryCached;
        }

        // 2. 시트에서 조회
        if (sheetsService == null || spreadsheetId.isEmpty()) {
            return null;
        }

        try {
            String range = sheetName + "!A2:R10000";  // 18컬럼
            ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return null;
            }

            for (List<Object> row : values) {
                String rowKey = getCell(row, 0);  // cacheKey는 인덱스 0
                if (cacheKey.equals(rowKey)) {
                    CarrierSubsidy subsidy = rowToSubsidy(row);
                    // 메모리 캐시에 저장
                    memoryCacheMap.put(cacheKey, subsidy);
                    cacheTimeMap.put(cacheKey, LocalDateTime.now());
                    log.info("📂 시트 캐시 히트: {}", cacheKey);
                    return subsidy;
                }
            }

            return null;

        } catch (Exception e) {
            log.error("❌ 캐시 조회 오류 [{}]: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    /**
     * deviceCode 기반으로 해당 기기의 모든 캐시 데이터 조회
     * (joinType, planCode가 없을 때 사용)
     *
     * @param carrier 통신사
     * @param deviceCode 기기 코드
     * @return 해당 기기의 캐시된 모든 공시지원금 목록
     */
    public List<CarrierSubsidy> getFromCacheByDevice(String carrier, String deviceCode) {
        String keyPrefix = String.format("%s_%s_", nvl(carrier), nvl(deviceCode));
        List<CarrierSubsidy> results = new ArrayList<>();

        // 1. 메모리 캐시에서 먼저 조회
        for (Map.Entry<String, CarrierSubsidy> entry : memoryCacheMap.entrySet()) {
            if (entry.getKey().startsWith(keyPrefix)) {
                results.add(entry.getValue());
            }
        }

        if (!results.isEmpty()) {
            log.debug("⚡ 메모리 캐시 히트 (기기별): {} → {}건", keyPrefix, results.size());
            return results;
        }

        // 2. 시트에서 조회
        if (sheetsService == null || spreadsheetId.isEmpty()) {
            return results;
        }

        try {
            String range = sheetName + "!A2:R10000";  // 18컬럼
            ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return results;
            }

            for (List<Object> row : values) {
                String rowKey = getCell(row, 0);
                if (rowKey != null && rowKey.startsWith(keyPrefix)) {
                    CarrierSubsidy subsidy = rowToSubsidy(row);
                    results.add(subsidy);
                    // 메모리 캐시에도 저장
                    memoryCacheMap.put(rowKey, subsidy);
                    cacheTimeMap.put(rowKey, LocalDateTime.now());
                }
            }

            if (!results.isEmpty()) {
                log.info("📂 시트 캐시 히트 (기기별): {} → {}건", keyPrefix, results.size());
            }

            return results;

        } catch (Exception e) {
            log.error("❌ 기기별 캐시 조회 오류 [{}]: {}", keyPrefix, e.getMessage());
            return results;
        }
    }

    /**
     * 조건별 캐시에 1건 저장 (append)
     */
    public synchronized void saveToCacheByKey(String cacheKey, CarrierSubsidy subsidy) {
        // 메모리 캐시에 먼저 저장
        memoryCacheMap.put(cacheKey, subsidy);
        cacheTimeMap.put(cacheKey, LocalDateTime.now());

        if (sheetsService == null || spreadsheetId.isEmpty()) {
            log.warn("⚠️ Google Sheets 미설정 - 메모리 캐시만 사용");
            return;
        }

        log.info("💾 캐시 저장: {}", cacheKey);

        try {
            String now = LocalDateTime.now().format(DATE_FORMATTER);

            // 1건 데이터 행 생성 (announceDate 컬럼 추가)
            List<Object> row = Arrays.asList(
                cacheKey,
                nvl(subsidy.getCarrier()),
                nvl(subsidy.getJoinType()),
                nvl(subsidy.getDiscountType()),
                nvl(subsidy.getDeviceName()),
                nvl(subsidy.getDeviceCode()),
                nvl(subsidy.getStorage()),
                nvl(subsidy.getColor()),
                nvl(subsidy.getPlanName()),
                nvl(subsidy.getPlanCode()),
                subsidy.getPlanMonthlyFee() != null ? subsidy.getPlanMonthlyFee() : "",
                subsidy.getPlanMaintainMonth() != null ? subsidy.getPlanMaintainMonth() : 6,
                subsidy.getMsrp() != null ? subsidy.getMsrp() : "",
                subsidy.getCarrierSubsidy() != null ? subsidy.getCarrierSubsidy() : "",
                subsidy.getAdditionalSubsidy() != null ? subsidy.getAdditionalSubsidy() : "",
                subsidy.getInstallmentPrice() != null ? subsidy.getInstallmentPrice() : "",
                nvl(subsidy.getAnnounceDate()),  // 공시일
                now
            );

            // 시트에 append (기존 데이터 유지하고 끝에 추가) - 18컬럼
            String appendRange = sheetName + "!A:R";
            ValueRange body = new ValueRange().setValues(Collections.singletonList(row));

            AppendValuesResponse result = sheetsService.spreadsheets().values()
                .append(spreadsheetId, appendRange, body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute();

            log.info("✅ 캐시 저장 완료: {} (행 {})", cacheKey, result.getUpdates().getUpdatedRows());

        } catch (Exception e) {
            log.error("❌ 캐시 저장 오류 [{}]: {}", cacheKey, e.getMessage());
        }
    }

    // ==================== 기존 호환성 유지 메서드 (deprecated) ====================

    /**
     * @deprecated 조건별 캐시 사용 권장 - isCacheValidByKey() 사용
     */
    @Deprecated
    public boolean isCacheValid(String carrier) {
        return false; // 기존 방식은 더 이상 사용하지 않음
    }

    /**
     * @deprecated 조건별 캐시 사용 권장 - getFromCacheByKey() 사용
     */
    @Deprecated
    public List<CarrierSubsidy> getFromCache(String carrier) {
        return new ArrayList<>();
    }

    /**
     * 증분 업데이트 (공시일 기준)
     *
     * 기존 시트 데이터를 읽어서 공시일이 최근인 데이터만 업데이트/추가
     * - 동일 cacheKey가 있으면 → 업데이트 (덮어쓰기)
     * - 동일 cacheKey가 없으면 → 추가
     *
     * @param newSubsidies 새로 조회한 공시지원금 목록 (최근 7일 공시일 데이터)
     * @return 업데이트된 건수
     */
    public int updateCacheIncremental(List<CarrierSubsidy> newSubsidies) {
        if (newSubsidies == null || newSubsidies.isEmpty()) {
            log.info("ℹ️ 업데이트할 데이터 없음");
            return 0;
        }

        log.info("🔄 증분 업데이트 시작: {}건", newSubsidies.size());

        // 1. 기존 시트 데이터 조회
        List<CarrierSubsidy> existingData = getAllFromCache();
        log.info("📂 기존 캐시 데이터: {}건", existingData.size());

        // 2. 기존 데이터를 Map으로 변환 (cacheKey -> subsidy)
        Map<String, CarrierSubsidy> existingMap = new HashMap<>();
        for (CarrierSubsidy subsidy : existingData) {
            String cacheKey = buildCacheKey(
                subsidy.getCarrier(),
                subsidy.getDeviceCode(),
                subsidy.getJoinType(),
                subsidy.getPlanCode()
            );
            existingMap.put(cacheKey, subsidy);
        }

        // 3. 새 데이터로 업데이트/추가
        int updatedCount = 0;
        int addedCount = 0;
        for (CarrierSubsidy newSubsidy : newSubsidies) {
            String cacheKey = buildCacheKey(
                newSubsidy.getCarrier(),
                newSubsidy.getDeviceCode(),
                newSubsidy.getJoinType(),
                newSubsidy.getPlanCode()
            );

            if (existingMap.containsKey(cacheKey)) {
                // 기존 데이터 업데이트
                existingMap.put(cacheKey, newSubsidy);
                updatedCount++;
            } else {
                // 새 데이터 추가
                existingMap.put(cacheKey, newSubsidy);
                addedCount++;
            }

            // 메모리 캐시도 업데이트
            memoryCacheMap.put(cacheKey, newSubsidy);
            cacheTimeMap.put(cacheKey, LocalDateTime.now());
        }

        log.info("📊 업데이트: {}건, 추가: {}건", updatedCount, addedCount);

        // 4. 전체 데이터를 시트에 저장
        List<CarrierSubsidy> mergedData = new ArrayList<>(existingMap.values());
        int savedCount = saveAllToCache(mergedData);

        log.info("✅ 증분 업데이트 완료: 총 {}건 저장", savedCount);
        return updatedCount + addedCount;
    }

    /**
     * 전체 공시지원금 데이터 일괄 저장 (시트 덮어쓰기)
     *
     * @param subsidies 저장할 공시지원금 목록
     * @return 저장된 건수
     */
    public int saveAllToCache(List<CarrierSubsidy> subsidies) {
        if (subsidies == null || subsidies.isEmpty()) {
            log.warn("⚠️ 저장할 데이터 없음");
            return 0;
        }

        // 메모리 캐시 업데이트
        memoryCacheMap.clear();
        cacheTimeMap.clear();

        for (CarrierSubsidy subsidy : subsidies) {
            String cacheKey = buildCacheKey(
                subsidy.getCarrier(),
                subsidy.getDeviceCode(),
                subsidy.getJoinType(),
                subsidy.getPlanCode()
            );
            memoryCacheMap.put(cacheKey, subsidy);
            cacheTimeMap.put(cacheKey, LocalDateTime.now());
        }

        log.info("💾 메모리 캐시에 {}건 저장 완료", subsidies.size());

        // 시트에 저장
        if (sheetsService == null || spreadsheetId.isEmpty()) {
            log.warn("⚠️ Google Sheets 미설정 - 메모리 캐시만 사용");
            return subsidies.size();
        }

        try {
            String now = LocalDateTime.now().format(DATE_FORMATTER);

            // 데이터 행 생성
            List<List<Object>> rows = new ArrayList<>();
            rows.add(HEADERS);  // 헤더 추가

            for (CarrierSubsidy subsidy : subsidies) {
                String cacheKey = buildCacheKey(
                    subsidy.getCarrier(),
                    subsidy.getDeviceCode(),
                    subsidy.getJoinType(),
                    subsidy.getPlanCode()
                );

                rows.add(Arrays.asList(
                    cacheKey,
                    nvl(subsidy.getCarrier()),
                    nvl(subsidy.getJoinType()),
                    nvl(subsidy.getDiscountType()),
                    nvl(subsidy.getDeviceName()),
                    nvl(subsidy.getDeviceCode()),
                    nvl(subsidy.getStorage()),
                    nvl(subsidy.getColor()),
                    nvl(subsidy.getPlanName()),
                    nvl(subsidy.getPlanCode()),
                    subsidy.getPlanMonthlyFee() != null ? subsidy.getPlanMonthlyFee() : "",
                    subsidy.getPlanMaintainMonth() != null ? subsidy.getPlanMaintainMonth() : 6,
                    subsidy.getMsrp() != null ? subsidy.getMsrp() : "",
                    subsidy.getCarrierSubsidy() != null ? subsidy.getCarrierSubsidy() : "",
                    subsidy.getAdditionalSubsidy() != null ? subsidy.getAdditionalSubsidy() : "",
                    subsidy.getInstallmentPrice() != null ? subsidy.getInstallmentPrice() : "",
                    nvl(subsidy.getAnnounceDate()),
                    now
                ));
            }

            // 기존 데이터 클리어
            com.google.api.services.sheets.v4.model.ClearValuesRequest clearReq =
                new com.google.api.services.sheets.v4.model.ClearValuesRequest();
            String clearRange = sheetName + "!A:R";
            sheetsService.spreadsheets().values()
                .clear(spreadsheetId, clearRange, clearReq)
                .execute();

            // 새 데이터 쓰기
            String writeRange = sheetName + "!A1:R" + rows.size();
            ValueRange body = new ValueRange().setValues(rows);
            sheetsService.spreadsheets().values()
                .update(spreadsheetId, writeRange, body)
                .setValueInputOption("USER_ENTERED")
                .execute();

            log.info("✅ {} 시트에 {}건 저장 완료 (헤더 포함)", sheetName, rows.size());
            return subsidies.size();

        } catch (Exception e) {
            log.error("❌ 시트 저장 오류: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * @deprecated 조건별 캐시 사용 권장 - saveToCacheByKey() 사용
     */
    @Deprecated
    public void saveToCache(String carrier, List<CarrierSubsidy> subsidies) {
        // 기존 방식은 더 이상 사용하지 않음
        log.warn("⚠️ saveToCache(carrier) deprecated - saveAllToCache() 또는 saveToCacheByKey() 사용 권장");
    }

    /**
     * 전체 캐시 데이터 조회 (통신사별)
     */
    public List<CarrierSubsidy> getAllFromCache() {
        if (sheetsService == null || spreadsheetId.isEmpty()) {
            return new ArrayList<>(memoryCacheMap.values());
        }

        try {
            String range = sheetName + "!A2:R10000";  // 18컬럼
            ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return new ArrayList<>(memoryCacheMap.values());
            }

            List<CarrierSubsidy> subsidies = new ArrayList<>();
            for (List<Object> row : values) {
                subsidies.add(rowToSubsidy(row));
            }

            return subsidies;

        } catch (Exception e) {
            log.error("❌ 전체 캐시 조회 오류: {}", e.getMessage());
            return new ArrayList<>(memoryCacheMap.values());
        }
    }

    /**
     * 캐시 상태 조회
     */
    public Map<String, Object> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", sheetsService != null);
        status.put("spreadsheetId", spreadsheetId);
        status.put("sheetName", sheetName);
        status.put("ttlHours", cacheTtlHours);
        status.put("memoryCacheSize", memoryCacheMap.size());
        status.put("cacheType", "조건별 Lazy Cache");
        status.put("scheduledCleanup", "1시간마다 자동 정리");

        // 만료 예정 캐시 수 계산
        LocalDateTime now = LocalDateTime.now();
        long expiredCount = cacheTimeMap.values().stream()
            .filter(cachedAt -> ChronoUnit.HOURS.between(cachedAt, now) >= cacheTtlHours)
            .count();
        status.put("expiredCacheCount", expiredCount);

        // 메모리 캐시 키 목록 (최근 10개)
        List<String> recentKeys = cacheTimeMap.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .map(Map.Entry::getKey)
            .toList();
        status.put("recentCacheKeys", recentKeys);

        return status;
    }

    /**
     * 캐시 강제 초기화
     */
    public void clearCache() {
        // 메모리 캐시 초기화
        memoryCacheMap.clear();
        cacheTimeMap.clear();
        log.info("✅ 메모리 캐시 초기화 완료");

        // 시트 초기화는 선택적
        if (sheetsService != null && !spreadsheetId.isEmpty()) {
            try {
                com.google.api.services.sheets.v4.model.ClearValuesRequest req =
                    new com.google.api.services.sheets.v4.model.ClearValuesRequest();
                String clearRange = sheetName + "!A2:R10000";  // 18컬럼
                sheetsService.spreadsheets().values()
                    .clear(spreadsheetId, clearRange, req)
                    .execute();
                log.info("✅ 시트 캐시 초기화 완료");
            } catch (Exception e) {
                log.error("❌ 시트 캐시 초기화 오류: {}", e.getMessage());
            }
        }
    }

    // ==================== 캐시 클린업 스케줄러 ====================

    /**
     * 만료된 캐시 항목 정리 스케줄러
     *
     * 1시간마다 실행하여 TTL이 지난 메모리 캐시 항목을 제거합니다.
     * 메모리 누수를 방지하고 캐시 효율성을 유지합니다.
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 (60 * 60 * 1000ms)
    public void cleanupExpiredCache() {
        if (cacheTimeMap.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        AtomicInteger removedCount = new AtomicInteger(0);
        int beforeSize = memoryCacheMap.size();

        // 만료된 항목 찾아서 제거
        cacheTimeMap.entrySet().removeIf(entry -> {
            long hours = ChronoUnit.HOURS.between(entry.getValue(), now);
            if (hours >= cacheTtlHours) {
                String key = entry.getKey();
                memoryCacheMap.remove(key);
                removedCount.incrementAndGet();
                return true;
            }
            return false;
        });

        if (removedCount.get() > 0) {
            log.info("🧹 캐시 클린업 완료: {}건 제거 ({}건 → {}건)",
                removedCount.get(), beforeSize, memoryCacheMap.size());
        }
    }

    /**
     * 캐시 클린업 수동 실행 (테스트/관리용)
     *
     * @return 제거된 캐시 항목 수
     */
    public int runCacheCleanup() {
        LocalDateTime now = LocalDateTime.now();
        AtomicInteger removedCount = new AtomicInteger(0);

        cacheTimeMap.entrySet().removeIf(entry -> {
            long hours = ChronoUnit.HOURS.between(entry.getValue(), now);
            if (hours >= cacheTtlHours) {
                memoryCacheMap.remove(entry.getKey());
                removedCount.incrementAndGet();
                return true;
            }
            return false;
        });

        log.info("🧹 수동 캐시 클린업: {}건 제거", removedCount.get());
        return removedCount.get();
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * 시트 행 → CarrierSubsidy 변환 (새 컬럼 구조, announceDate 포함)
     * 컬럼: cacheKey, carrier, joinType, discountType, deviceName, deviceCode,
     *       storage, color, planName, planCode, planMonthlyFee, planMaintainMonth,
     *       msrp, carrierSubsidy, additionalSubsidy, installmentPrice, announceDate, cachedAt
     */
    private CarrierSubsidy rowToSubsidy(List<Object> row) {
        return CarrierSubsidy.builder()
            .id(getCell(row, 0))  // cacheKey를 id로 사용
            .carrier(getCell(row, 1))
            .joinType(getCell(row, 2))
            .discountType(getCell(row, 3))
            .deviceName(getCell(row, 4))
            .deviceCode(getCell(row, 5))
            .storage(getCell(row, 6))
            .color(getCell(row, 7))
            .planName(getCell(row, 8))
            .planCode(getCell(row, 9))
            .planMonthlyFee(getIntCell(row, 10))
            .planMaintainMonth(getIntCell(row, 11))
            .msrp(getIntCell(row, 12))
            .carrierSubsidy(getIntCell(row, 13))
            .additionalSubsidy(getIntCell(row, 14))
            .installmentPrice(getIntCell(row, 15))
            .announceDate(getCell(row, 16))  // 공시일
            .build();
    }

    private String getCell(List<Object> row, int index) {
        if (row == null || index >= row.size()) return null;
        Object value = row.get(index);
        return value != null ? value.toString() : null;
    }

    private Integer getIntCell(List<Object> row, int index) {
        String value = getCell(row, index);
        if (value == null || value.isEmpty()) return null;
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
