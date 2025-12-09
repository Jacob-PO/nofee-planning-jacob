package com.nofee.api.test.devicemapping.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.nofee.api.test.devicemapping.dto.DeviceMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Google Sheets 연동 서비스
 *
 * Google Sheets에 시트가 없으면 인메모리 캐시 사용
 */
@Slf4j
@Service
public class GoogleSheetsService {

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    @Value("${google.sheets.mapping-sheet-name}")
    private String sheetName;

    @Value("${google.sheets.credentials-path}")
    private String credentialsPath;

    private Sheets sheetsService;

    // Google Sheets 연동 실패 시 사용하는 인메모리 캐시
    private final List<DeviceMapping> inMemoryMappings = new ArrayList<>();
    private boolean useInMemoryCache = false;

    @PostConstruct
    public void init() {
        if (credentialsPath == null || credentialsPath.isEmpty()) {
            log.warn("⚠️ Google Sheets 설정 없음 - 기능 비활성화");
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
                .setApplicationName("Nofee Device Mapping")
                .build();

            log.info("✅ Google Sheets 서비스 초기화 완료");
        } catch (Exception e) {
            log.error("❌ Google Sheets 초기화 오류: {}", e.getMessage());
        }
    }

    /**
     * 매핑 데이터 저장
     */
    public void saveMappings(List<DeviceMapping> mappings) {
        // 인메모리 캐시 항상 업데이트
        synchronized (inMemoryMappings) {
            inMemoryMappings.clear();
            inMemoryMappings.addAll(mappings);
        }
        log.info("💾 인메모리 캐시에 {}개 매핑 저장", mappings.size());

        if (sheetsService == null || spreadsheetId.isEmpty() || useInMemoryCache) {
            log.warn("⚠️ Google Sheets 미사용 - 인메모리 캐시만 사용");
            return;
        }

        log.info("📊 Google Sheets에 저장 중...");

        try {
            // 헤더
            List<Object> headers = Arrays.asList(
                "노피상품코드", "노피상품명",
                "SKT기기코드", "SKT기기명",
                "KT기기코드", "KT기기명",
                "LGU+기기코드", "LGU+기기명",
                "매핑일시", "신뢰도"
            );

            // 데이터 행
            List<List<Object>> rows = new ArrayList<>();
            rows.add(headers);

            for (DeviceMapping m : mappings) {
                rows.add(Arrays.asList(
                    m.getNofeeProductCode() != null ? m.getNofeeProductCode() : "",
                    m.getNofeeProductName() != null ? m.getNofeeProductName() : "",
                    m.getSktDeviceCode() != null ? m.getSktDeviceCode() : "",
                    m.getSktDeviceName() != null ? m.getSktDeviceName() : "",
                    m.getKtDeviceCode() != null ? m.getKtDeviceCode() : "",
                    m.getKtDeviceName() != null ? m.getKtDeviceName() : "",
                    m.getLguDeviceCode() != null ? m.getLguDeviceCode() : "",
                    m.getLguDeviceName() != null ? m.getLguDeviceName() : "",
                    m.getMappedAt() != null ? m.getMappedAt() : "",
                    m.getConfidence() != null ? m.getConfidence() : ""
                ));
            }

            // 기존 데이터 클리어
            String clearRange = sheetName + "!A:J";
            sheetsService.spreadsheets().values()
                .clear(spreadsheetId, clearRange, new ClearValuesRequest())
                .execute();

            // 새 데이터 쓰기
            String writeRange = sheetName + "!A1:J" + rows.size();
            ValueRange body = new ValueRange().setValues(rows);
            sheetsService.spreadsheets().values()
                .update(spreadsheetId, writeRange, body)
                .setValueInputOption("USER_ENTERED")
                .execute();

            log.info("✅ {}개 매핑 Google Sheets 저장 완료", mappings.size());
            log.info("📎 https://docs.google.com/spreadsheets/d/{}", spreadsheetId);

        } catch (Exception e) {
            log.error("❌ Google Sheets 저장 오류: {} - 인메모리 캐시 사용으로 전환", e.getMessage());
            useInMemoryCache = true;
        }
    }

    /**
     * 매핑 데이터 조회
     */
    public List<DeviceMapping> loadMappings() {
        // 인메모리 캐시 우선
        if (useInMemoryCache || sheetsService == null || spreadsheetId.isEmpty()) {
            synchronized (inMemoryMappings) {
                log.debug("📂 인메모리 캐시에서 {}개 매핑 조회", inMemoryMappings.size());
                return new ArrayList<>(inMemoryMappings);
            }
        }

        try {
            String range = sheetName + "!A2:J1000";
            ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                // Google Sheets가 비어있으면 인메모리 캐시 반환
                synchronized (inMemoryMappings) {
                    return new ArrayList<>(inMemoryMappings);
                }
            }

            List<DeviceMapping> mappings = new ArrayList<>();
            for (List<Object> row : values) {
                mappings.add(DeviceMapping.builder()
                    .nofeeProductCode(getCell(row, 0))
                    .nofeeProductName(getCell(row, 1))
                    .sktDeviceCode(getCell(row, 2))
                    .sktDeviceName(getCell(row, 3))
                    .ktDeviceCode(getCell(row, 4))
                    .ktDeviceName(getCell(row, 5))
                    .lguDeviceCode(getCell(row, 6))
                    .lguDeviceName(getCell(row, 7))
                    .mappedAt(getCell(row, 8))
                    .confidence(getCell(row, 9))
                    .build());
            }

            return mappings;

        } catch (Exception e) {
            log.error("❌ Google Sheets 조회 오류: {} - 인메모리 캐시 사용", e.getMessage());
            useInMemoryCache = true;
            synchronized (inMemoryMappings) {
                return new ArrayList<>(inMemoryMappings);
            }
        }
    }

    private String getCell(List<Object> row, int index) {
        if (row == null || index >= row.size()) {
            return null;
        }
        Object value = row.get(index);
        return value != null ? value.toString() : null;
    }
}
