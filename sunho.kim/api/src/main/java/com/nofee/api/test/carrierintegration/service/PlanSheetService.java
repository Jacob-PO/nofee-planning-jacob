package com.nofee.api.test.carrierintegration.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.nofee.api.test.carrierintegration.dto.CarrierPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 요금제 시트 관리 서비스
 *
 * summary-plan 시트에 통신사별 요금제 정보를 관리
 * - 요금제 코드, 이름, 월정액, 네트워크 유형 등
 * - 월정액으로 요금제 코드를 찾는 기능 제공
 */
@Slf4j
@Service
public class PlanSheetService {

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    @Value("${google.sheets.credentials-path}")
    private String credentialsPath;

    private static final String PLAN_SHEET_NAME = "summary-plan";

    private Sheets sheetsService;

    // 메모리 캐시: carrier -> monthlyFee -> planCode
    private final Map<String, Map<Integer, String>> planCodeCache = new ConcurrentHashMap<>();

    // 메모리 캐시: 전체 요금제 목록
    private final List<CarrierPlan> planListCache = Collections.synchronizedList(new ArrayList<>());

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 헤더 정의
    private static final List<Object> HEADERS = Arrays.asList(
        "id", "carrier", "planCode", "planName", "monthlyFee", "networkType",
        "dataAllowance", "voiceAllowance", "smsAllowance", "planType",
        "description", "active", "createdAt", "updatedAt"
    );

    @PostConstruct
    public void init() {
        if (credentialsPath == null || credentialsPath.isEmpty()) {
            log.warn("⚠️ Google Sheets 설정 없음 - 요금제 시트 기능 비활성화");
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
                .setApplicationName("Nofee Plan Sheet")
                .build();

            log.info("✅ 요금제 시트 서비스 초기화 완료");

            // 시트에서 요금제 정보 로드
            loadPlansFromSheet();

        } catch (Exception e) {
            log.error("❌ Google Sheets 초기화 오류: {}", e.getMessage());
        }
    }

    /**
     * 월정액으로 요금제 코드 조회
     *
     * @param carrier 통신사 (SKT, KT, LGU)
     * @param monthlyFee 월정액 (원)
     * @return 요금제 코드 (없으면 null)
     */
    public String getPlanCodeByMonthlyFee(String carrier, Integer monthlyFee) {
        if (carrier == null || monthlyFee == null) return null;

        Map<Integer, String> carrierPlans = planCodeCache.get(carrier.toUpperCase());
        if (carrierPlans == null) {
            log.warn("⚠️ {} 요금제 캐시 없음", carrier);
            return null;
        }

        String planCode = carrierPlans.get(monthlyFee);
        if (planCode == null) {
            log.warn("⚠️ {} 월정액 {}원 요금제 없음", carrier, monthlyFee);
        }
        return planCode;
    }

    /**
     * 월정액 + 네트워크 유형으로 요금제 코드 조회
     *
     * @param carrier 통신사 (SKT, KT, LGU)
     * @param monthlyFee 월정액 (원)
     * @param networkType 네트워크 유형 (5G 또는 LTE)
     * @return 요금제 코드 (없으면 null)
     */
    public String getPlanCodeByMonthlyFeeAndNetwork(String carrier, Integer monthlyFee, String networkType) {
        if (carrier == null || monthlyFee == null) return null;

        // 네트워크 유형까지 고려해서 검색
        String normalizedNetwork = (networkType != null) ? networkType.toUpperCase() : "5G";

        // 먼저 네트워크 유형이 일치하는 요금제 찾기
        for (CarrierPlan plan : planListCache) {
            if (carrier.equalsIgnoreCase(plan.getCarrier()) &&
                monthlyFee.equals(plan.getMonthlyFee()) &&
                normalizedNetwork.equalsIgnoreCase(plan.getNetworkType())) {
                log.debug("✅ 요금제 찾음: {} {} {}원 -> {}", carrier, normalizedNetwork, monthlyFee, plan.getPlanCode());
                return plan.getPlanCode();
            }
        }

        // 네트워크 유형 없이 월정액만으로 찾기 (폴백)
        log.warn("⚠️ {} {} {}원 요금제 없음, 월정액만으로 검색", carrier, normalizedNetwork, monthlyFee);
        return getPlanCodeByMonthlyFee(carrier, monthlyFee);
    }

    /**
     * 요금제 정보 조회
     *
     * @param carrier 통신사
     * @param planCode 요금제 코드
     * @return 요금제 정보 (없으면 null)
     */
    public CarrierPlan getPlan(String carrier, String planCode) {
        return planListCache.stream()
            .filter(p -> carrier.equalsIgnoreCase(p.getCarrier()) &&
                        planCode.equals(p.getPlanCode()))
            .findFirst()
            .orElse(null);
    }

    /**
     * 통신사별 활성화된 요금제 목록 조회
     */
    public List<CarrierPlan> getActivePlansByCarrier(String carrier) {
        return planListCache.stream()
            .filter(p -> carrier.equalsIgnoreCase(p.getCarrier()) &&
                        Boolean.TRUE.equals(p.getActive()))
            .toList();
    }

    /**
     * 전체 요금제 목록 조회
     */
    public List<CarrierPlan> getAllPlans() {
        return new ArrayList<>(planListCache);
    }

    /**
     * 요금제 목록 저장 (전체 덮어쓰기)
     */
    public synchronized void savePlans(List<CarrierPlan> plans) {
        // 메모리 캐시 업데이트
        planListCache.clear();
        planListCache.addAll(plans);

        // 월정액 -> 요금제 코드 캐시 재구성
        planCodeCache.clear();
        for (CarrierPlan plan : plans) {
            if (plan.getCarrier() != null && plan.getMonthlyFee() != null && plan.getPlanCode() != null) {
                planCodeCache
                    .computeIfAbsent(plan.getCarrier().toUpperCase(), k -> new ConcurrentHashMap<>())
                    .put(plan.getMonthlyFee(), plan.getPlanCode());
            }
        }

        log.info("💾 메모리 캐시에 {}개 요금제 저장", plans.size());

        // Google Sheets에 저장
        if (sheetsService == null || spreadsheetId == null || spreadsheetId.isEmpty()) {
            log.warn("⚠️ Google Sheets 미설정 - 메모리 캐시만 사용");
            return;
        }

        try {
            String now = LocalDateTime.now().format(DATE_FORMATTER);

            // 데이터 행 생성
            List<List<Object>> rows = new ArrayList<>();
            rows.add(HEADERS);

            for (CarrierPlan plan : plans) {
                rows.add(Arrays.asList(
                    nvl(plan.getId() != null ? plan.getId() : plan.generateId()),
                    nvl(plan.getCarrier()),
                    nvl(plan.getPlanCode()),
                    nvl(plan.getPlanName()),
                    plan.getMonthlyFee() != null ? plan.getMonthlyFee() : "",
                    nvl(plan.getNetworkType()),
                    nvl(plan.getDataAllowance()),
                    nvl(plan.getVoiceAllowance()),
                    nvl(plan.getSmsAllowance()),
                    nvl(plan.getPlanType()),
                    nvl(plan.getDescription()),
                    plan.getActive() != null ? plan.getActive() : true,
                    plan.getCreatedAt() != null ? plan.getCreatedAt().format(DATE_FORMATTER) : now,
                    now
                ));
            }

            // 기존 데이터 클리어
            String clearRange = PLAN_SHEET_NAME + "!A:N";
            sheetsService.spreadsheets().values()
                .clear(spreadsheetId, clearRange, new ClearValuesRequest())
                .execute();

            // 새 데이터 쓰기
            String writeRange = PLAN_SHEET_NAME + "!A1:N" + rows.size();
            ValueRange body = new ValueRange().setValues(rows);
            sheetsService.spreadsheets().values()
                .update(spreadsheetId, writeRange, body)
                .setValueInputOption("USER_ENTERED")
                .execute();

            log.info("✅ {}개 요금제 Google Sheets 저장 완료", plans.size());
            log.info("📎 시트: summary-plan");

        } catch (Exception e) {
            log.error("❌ Google Sheets 저장 오류: {}", e.getMessage());
        }
    }

    /**
     * 시트에서 요금제 정보 로드
     */
    public void loadPlansFromSheet() {
        if (sheetsService == null || spreadsheetId == null || spreadsheetId.isEmpty()) {
            log.warn("⚠️ Google Sheets 미설정 - 시트 로드 불가");
            return;
        }

        try {
            String range = PLAN_SHEET_NAME + "!A2:N1000";
            ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                log.info("📂 summary-plan 시트가 비어있음");
                return;
            }

            planListCache.clear();
            planCodeCache.clear();

            for (List<Object> row : values) {
                CarrierPlan plan = rowToPlan(row);
                if (plan != null) {
                    planListCache.add(plan);

                    // 월정액 -> 코드 캐시
                    if (plan.getCarrier() != null && plan.getMonthlyFee() != null && plan.getPlanCode() != null) {
                        planCodeCache
                            .computeIfAbsent(plan.getCarrier().toUpperCase(), k -> new ConcurrentHashMap<>())
                            .put(plan.getMonthlyFee(), plan.getPlanCode());
                    }
                }
            }

            log.info("✅ summary-plan 시트에서 {}개 요금제 로드 완료", planListCache.size());

            // 통신사별 현황 로그
            for (Map.Entry<String, Map<Integer, String>> entry : planCodeCache.entrySet()) {
                log.info("   {} : {}개 요금제", entry.getKey(), entry.getValue().size());
            }

        } catch (Exception e) {
            log.error("❌ 시트 로드 오류: {}", e.getMessage());
        }
    }

    /**
     * 캐시 상태 조회
     */
    public Map<String, Object> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", sheetsService != null);
        status.put("sheetName", PLAN_SHEET_NAME);
        status.put("totalPlans", planListCache.size());

        Map<String, Integer> carrierCounts = new HashMap<>();
        for (Map.Entry<String, Map<Integer, String>> entry : planCodeCache.entrySet()) {
            carrierCounts.put(entry.getKey(), entry.getValue().size());
        }
        status.put("plansByCarrier", carrierCounts);

        return status;
    }

    /**
     * 시트 행 -> CarrierPlan 변환
     */
    private CarrierPlan rowToPlan(List<Object> row) {
        try {
            return CarrierPlan.builder()
                .id(getCell(row, 0))
                .carrier(getCell(row, 1))
                .planCode(getCell(row, 2))
                .planName(getCell(row, 3))
                .monthlyFee(getIntCell(row, 4))
                .networkType(getCell(row, 5))
                .dataAllowance(getCell(row, 6))
                .voiceAllowance(getCell(row, 7))
                .smsAllowance(getCell(row, 8))
                .planType(getCell(row, 9))
                .description(getCell(row, 10))
                .active(getBoolCell(row, 11))
                .build();
        } catch (Exception e) {
            log.debug("요금제 행 파싱 실패: {}", e.getMessage());
            return null;
        }
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

    private Boolean getBoolCell(List<Object> row, int index) {
        String value = getCell(row, index);
        if (value == null || value.isEmpty()) return true;
        return "true".equalsIgnoreCase(value) || "TRUE".equals(value) || "1".equals(value);
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
