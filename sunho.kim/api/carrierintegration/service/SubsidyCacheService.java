package com.nofee.api.test.carrierintegration.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.nofee.api.test.carrierintegration.dto.CarrierSubsidy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 공시지원금 캐시 서비스
 *
 * Google Sheets를 임시 DB로 활용한 Lazy Cache
 * - 첫 조회 시 API 호출 후 스프레드시트에 저장
 * - 동일 조건 조회 시 스프레드시트에서 가져옴
 * - 24시간마다 자동 갱신
 */
@Slf4j
@Service
public class SubsidyCacheService {

    @Value("${google.sheets.spreadsheet-id:1ftWmcEBku_il3V50HQv33H-n6j3K1epVVwpbugAu2zU}")
    private String spreadsheetId;

    @Value("${google.sheets.subsidy-sheet-name:summary-new}")
    private String sheetName;

    @Value("${google.sheets.credentials-path:/Users/jacob/Desktop/dev/config/google_api_key.json}")
    private String credentialsPath;

    @Value("${google.sheets.cache-ttl-hours:24}")
    private int cacheTtlHours;

    private Sheets sheetsService;

    // 메모리 캐시 (마지막 갱신 시간 추적)
    private final Map<String, LocalDateTime> lastUpdateMap = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 헤더 정의 (프론트엔드 형식에 맞춤)
    private static final List<Object> HEADERS = Arrays.asList(
        "id", "carrier", "joinType", "discountType", "deviceName", "deviceCode",
        "storage", "color", "planName", "planCode", "planMonthlyFee", "planMaintainMonth",
        "msrp", "carrierSubsidy", "additionalSubsidy", "installmentPrice", "cachedAt"
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
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Nofee Subsidy Cache")
                .build();

            log.info("✅ 공시지원금 캐시 서비스 초기화 완료 (TTL: {}시간)", cacheTtlHours);
        } catch (Exception e) {
            log.error("❌ Google Sheets 초기화 오류: {}", e.getMessage());
        }
    }

    /**
     * 캐시 유효성 확인
     */
    public boolean isCacheValid(String carrier) {
        if (sheetsService == null) return false;

        LocalDateTime lastUpdate = lastUpdateMap.get(carrier);
        if (lastUpdate == null) {
            // 스프레드시트에서 마지막 조회일시 확인
            lastUpdate = getLastUpdateFromSheet(carrier);
            if (lastUpdate != null) {
                lastUpdateMap.put(carrier, lastUpdate);
            }
        }

        if (lastUpdate == null) return false;

        long hoursSinceUpdate = ChronoUnit.HOURS.between(lastUpdate, LocalDateTime.now());
        boolean valid = hoursSinceUpdate < cacheTtlHours;

        log.debug("캐시 유효성 확인 [{}]: {}시간 경과, 유효={}", carrier, hoursSinceUpdate, valid);
        return valid;
    }

    /**
     * 캐시에서 데이터 조회
     */
    public List<CarrierSubsidy> getFromCache(String carrier) {
        if (sheetsService == null || spreadsheetId.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("📂 캐시에서 {} 데이터 조회...", carrier);

        try {
            String range = sheetName + "!A2:Q10000";
            ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return new ArrayList<>();
            }

            List<CarrierSubsidy> subsidies = new ArrayList<>();
            for (List<Object> row : values) {
                String rowCarrier = getCell(row, 1);  // carrier는 인덱스 1
                if (carrier.equalsIgnoreCase(rowCarrier)) {
                    subsidies.add(rowToSubsidy(row));
                }
            }

            log.info("✅ 캐시에서 {}개 조회 완료 [{}]", subsidies.size(), carrier);
            return subsidies;

        } catch (Exception e) {
            log.error("❌ 캐시 조회 오류: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 캐시에 데이터 저장 (특정 통신사)
     * synchronized로 동시 저장 시 race condition 방지
     */
    public synchronized void saveToCache(String carrier, List<CarrierSubsidy> subsidies) {
        if (sheetsService == null || spreadsheetId.isEmpty()) {
            log.warn("⚠️ Google Sheets 미설정 - 캐시 저장 건너뜀");
            return;
        }

        log.info("💾 {} 데이터 {} 건 캐시 저장 중...", carrier, subsidies.size());

        try {
            // 기존 해당 통신사 데이터 삭제 후 새로 저장
            List<CarrierSubsidy> existingData = getAllFromCache();
            List<CarrierSubsidy> otherCarrierData = existingData.stream()
                .filter(s -> !carrier.equalsIgnoreCase(s.getCarrier()))
                .toList();

            // 새 데이터와 기존 다른 통신사 데이터 병합
            List<CarrierSubsidy> allData = new ArrayList<>(otherCarrierData);
            allData.addAll(subsidies);

            // 전체 저장
            saveAllToCache(allData);

            lastUpdateMap.put(carrier, LocalDateTime.now());
            log.info("✅ {} 캐시 저장 완료", carrier);

        } catch (Exception e) {
            log.error("❌ 캐시 저장 오류: {}", e.getMessage());
        }
    }

    /**
     * 전체 데이터 캐시에 저장
     */
    public void saveAllToCache(List<CarrierSubsidy> subsidies) {
        if (sheetsService == null || spreadsheetId.isEmpty()) {
            return;
        }

        try {
            String now = LocalDateTime.now().format(DATE_FORMATTER);

            // 데이터 행 생성
            List<List<Object>> rows = new ArrayList<>();
            rows.add(HEADERS);

            for (CarrierSubsidy s : subsidies) {
                rows.add(Arrays.asList(
                    nvl(s.getId()),
                    nvl(s.getCarrier()),
                    nvl(s.getJoinType()),
                    nvl(s.getDiscountType()),
                    nvl(s.getDeviceName()),
                    nvl(s.getDeviceCode()),
                    nvl(s.getStorage()),
                    nvl(s.getColor()),
                    nvl(s.getPlanName()),
                    nvl(s.getPlanCode()),
                    s.getPlanMonthlyFee() != null ? s.getPlanMonthlyFee() : "",
                    s.getPlanMaintainMonth() != null ? s.getPlanMaintainMonth() : 6,
                    s.getMsrp() != null ? s.getMsrp() : "",
                    s.getCarrierSubsidy() != null ? s.getCarrierSubsidy() : "",
                    s.getAdditionalSubsidy() != null ? s.getAdditionalSubsidy() : "",
                    s.getInstallmentPrice() != null ? s.getInstallmentPrice() : "",
                    now
                ));
            }

            // 기존 데이터 클리어
            String clearRange = sheetName + "!A:Q";
            sheetsService.spreadsheets().values()
                .clear(spreadsheetId, clearRange, new ClearValuesRequest())
                .execute();

            // 새 데이터 쓰기
            String writeRange = sheetName + "!A1:Q" + rows.size();
            ValueRange body = new ValueRange().setValues(rows);
            sheetsService.spreadsheets().values()
                .update(spreadsheetId, writeRange, body)
                .setValueInputOption("USER_ENTERED")
                .execute();

            log.info("✅ 전체 {} 건 캐시 저장 완료", subsidies.size());
            log.info("📎 https://docs.google.com/spreadsheets/d/{}", spreadsheetId);

        } catch (Exception e) {
            log.error("❌ 전체 캐시 저장 오류: {}", e.getMessage());
        }
    }

    /**
     * 전체 캐시 데이터 조회
     */
    public List<CarrierSubsidy> getAllFromCache() {
        if (sheetsService == null || spreadsheetId.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            String range = sheetName + "!A2:Q10000";
            ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return new ArrayList<>();
            }

            List<CarrierSubsidy> subsidies = new ArrayList<>();
            for (List<Object> row : values) {
                subsidies.add(rowToSubsidy(row));
            }

            return subsidies;

        } catch (Exception e) {
            log.error("❌ 전체 캐시 조회 오류: {}", e.getMessage());
            return new ArrayList<>();
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

        Map<String, Object> carriers = new HashMap<>();
        for (String carrier : Arrays.asList("SKT", "KT", "LGU")) {
            Map<String, Object> carrierStatus = new HashMap<>();
            carrierStatus.put("valid", isCacheValid(carrier));
            carrierStatus.put("lastUpdate", lastUpdateMap.get(carrier));
            carrierStatus.put("count", getFromCache(carrier).size());
            carriers.put(carrier, carrierStatus);
        }
        status.put("carriers", carriers);

        return status;
    }

    /**
     * 캐시 강제 초기화
     */
    public void clearCache() {
        if (sheetsService == null || spreadsheetId.isEmpty()) {
            return;
        }

        try {
            String clearRange = sheetName + "!A2:Q10000";
            sheetsService.spreadsheets().values()
                .clear(spreadsheetId, clearRange, new ClearValuesRequest())
                .execute();

            lastUpdateMap.clear();
            log.info("✅ 캐시 초기화 완료");

        } catch (Exception e) {
            log.error("❌ 캐시 초기화 오류: {}", e.getMessage());
        }
    }

    /**
     * 스프레드시트에서 마지막 업데이트 시간 조회
     */
    private LocalDateTime getLastUpdateFromSheet(String carrier) {
        try {
            String range = sheetName + "!A2:Q10000";
            ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return null;
            }

            for (List<Object> row : values) {
                String rowCarrier = getCell(row, 1);  // carrier는 인덱스 1
                if (carrier.equalsIgnoreCase(rowCarrier)) {
                    String dateStr = getCell(row, 16); // cachedAt 컬럼 (인덱스 16)
                    if (dateStr != null && !dateStr.isEmpty()) {
                        return LocalDateTime.parse(dateStr, DATE_FORMATTER);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("마지막 업데이트 시간 조회 실패: {}", e.getMessage());
        }
        return null;
    }

    private CarrierSubsidy rowToSubsidy(List<Object> row) {
        return CarrierSubsidy.builder()
            .id(getCell(row, 0))
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
