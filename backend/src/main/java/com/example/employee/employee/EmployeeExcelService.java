package com.example.employee.employee;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
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
public class EmployeeExcelService {

    private static final String[] HEADERS = {"氏名", "メールアドレス", "部署", "月給", "入社日"};
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final EmployeeService employeeService;
    private final EmployeeRepository repository;

    public EmployeeExcelService(EmployeeService employeeService, EmployeeRepository repository) {
        this.employeeService = employeeService;
        this.repository = repository;
    }

    public byte[] createTemplate() {
        try (Workbook workbook = createBaseWorkbook("社員アップロードテンプレート")) {
            Sheet sheet = workbook.getSheetAt(0);
            writeEmployeeRow(sheet.createRow(1), "佐藤花子", "sato@example.com", "営業部",
                    BigDecimal.valueOf(300000), LocalDate.now());
            sheet.createFreezePane(0, 1);
            return toBytes(workbook);
        } catch (IOException exception) {
            throw new IllegalStateException("テンプレートの作成に失敗しました", exception);
        }
    }

    public byte[] exportEmployees(List<Employee> employees) {
        try (Workbook workbook = createBaseWorkbook("社員一覧")) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowIndex = 1;
            for (Employee employee : employees) {
                writeEmployeeRow(sheet.createRow(rowIndex++), employee.getName(), employee.getEmail(),
                        employee.getDepartment(), employee.getSalary(), employee.getHireDate());
            }
            sheet.createFreezePane(0, 1);
            return toBytes(workbook);
        } catch (IOException exception) {
            throw new IllegalStateException("社員一覧の出力に失敗しました", exception);
        }
    }

    @Transactional
    public EmployeeImportResponse importEmployees(MultipartFile file) {
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
                    EmployeeRequest request = readRequest(row, formatter);
                    if (repository.existsByEmailIgnoreCase(request.email())) {
                        skipped++;
                        errors.add("行 " + (i + 1) + ": メールアドレスは登録済みです（" + request.email() + "）");
                        continue;
                    }
                    employeeService.create(request);
                    imported++;
                } catch (RuntimeException exception) {
                    skipped++;
                    errors.add("行 " + (i + 1) + ": " + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Excel ファイルを読み込めませんでした", exception);
        }

        return new EmployeeImportResponse(imported, skipped, errors);
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
            sheet.setColumnWidth(i, switch (i) {
                case 0, 2 -> 18 * 256;
                case 1 -> 30 * 256;
                case 3, 4 -> 14 * 256;
                default -> 16 * 256;
            });
        }
        return workbook;
    }

    private void writeEmployeeRow(Row row, String name, String email, String department, BigDecimal salary,
                                  LocalDate hireDate) {
        Workbook workbook = row.getSheet().getWorkbook();
        CellStyle moneyStyle = workbook.createCellStyle();
        moneyStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0"));
        CellStyle dateStyle = workbook.createCellStyle();
        CreationHelper helper = workbook.getCreationHelper();
        dateStyle.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));

        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(email);
        row.createCell(2).setCellValue(department);
        Cell salaryCell = row.createCell(3);
        salaryCell.setCellValue(salary.doubleValue());
        salaryCell.setCellStyle(moneyStyle);
        Cell dateCell = row.createCell(4);
        dateCell.setCellValue(java.sql.Date.valueOf(hireDate));
        dateCell.setCellStyle(dateStyle);
    }

    private EmployeeRequest readRequest(Row row, DataFormatter formatter) {
        String name = text(row, 0, formatter);
        String email = text(row, 1, formatter);
        String department = text(row, 2, formatter);
        BigDecimal salary = decimal(row, 3, formatter);
        LocalDate hireDate = date(row, 4, formatter);
        if (name.isBlank() || email.isBlank() || department.isBlank()) {
            throw new IllegalArgumentException("氏名、メールアドレス、部署は必須です");
        }
        if (salary.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("月給は 0 以上で入力してください");
        }
        return new EmployeeRequest(name, email, department, salary, hireDate);
    }

    private String text(Row row, int column, DataFormatter formatter) {
        Cell cell = row.getCell(column);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private BigDecimal decimal(Row row, int column, DataFormatter formatter) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            throw new IllegalArgumentException("月給は必須です");
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        try {
            return new BigDecimal(formatter.formatCellValue(cell).replace(",", "").trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("月給は数値で入力してください");
        }
    }

    private LocalDate date(Row row, int column, DataFormatter formatter) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            throw new IllegalArgumentException("入社日は必須です");
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        String value = formatter.formatCellValue(cell).trim();
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("入社日は yyyy-MM-dd 形式で入力してください");
        }
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
