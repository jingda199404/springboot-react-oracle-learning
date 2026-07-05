package com.example.employee.auth;

import com.example.employee.common.TextUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserExcelService {

    private static final String[] HEADERS = {"ユーザー名", "パスワード", "権限コード"};

    private final AuthService authService;

    public UserExcelService(AuthService authService) {
        this.authService = authService;
    }

    public byte[] createTemplate() {
        try (Workbook workbook = createBaseWorkbook("ユーザー登録テンプレート")) {
            Sheet sheet = workbook.getSheetAt(0);
            writeUserRow(sheet.createRow(1), "sample_user", "password123", "ACCOUNTING");
            writeUserRow(sheet.createRow(2), "sample_admin", "password123", "ACCOUNTING,PERMISSION_SETTING");
            sheet.createFreezePane(0, 1);
            return toBytes(workbook);
        } catch (IOException exception) {
            throw new IllegalStateException("ユーザーテンプレートの作成に失敗しました", exception);
        }
    }

    @Transactional
    public UserImportResponse importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("アップロードする Excel ファイルを選択してください");
        }

        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (isBlank(row, formatter)) {
                    continue;
                }

                try {
                    String username = text(row, 0, formatter);
                    String password = text(row, 1, formatter);
                    List<String> permissions = permissionCodes(text(row, 2, formatter));
                    authService.createManagedUser(username, password, permissions);
                    imported++;
                } catch (RuntimeException exception) {
                    skipped++;
                    errors.add("行 " + (i + 1) + ": " + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Excel ファイルを読み込めませんでした", exception);
        }

        return new UserImportResponse(imported, skipped, errors);
    }

    private Workbook createBaseWorkbook(String sheetName) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, i == 2 ? 42 * 256 : 22 * 256);
        }
        return workbook;
    }

    private void writeUserRow(Row row, String username, String password, String permissions) {
        row.createCell(0).setCellValue(username);
        row.createCell(1).setCellValue(password);
        row.createCell(2).setCellValue(permissions);
    }

    private List<String> permissionCodes(String value) {
        if (value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.replace("、", ",").split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .distinct()
                .toList();
    }

    private String text(Row row, int column, DataFormatter formatter) {
        Cell cell = row.getCell(column);
        return cell == null ? "" : TextUtils.trimToEmpty(formatter.formatCellValue(cell));
    }

    private boolean isBlank(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < HEADERS.length; i++) {
            if (!text(row, i, formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
