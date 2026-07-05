package com.example.employee.stock;

import com.example.employee.common.TextUtils;
import com.example.employee.error.ResourceNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StockTradeService {

    private static final Logger log = LoggerFactory.getLogger(StockTradeService.class);
    private static final Pattern STOCK_DATE_PATTERN = Pattern.compile("^(.+?)(\\d{1,2})[./．。](\\d{1,2})$");
    private static final Pattern LAST_MULTIPLIER_PATTERN = Pattern.compile("\\*\\s*([0-9]+(?:\\.[0-9]+)?)\\s*$");
    private static final BigDecimal DEFAULT_BUY_MULTIPLIER = new BigDecimal("1.00032");
    private static final BigDecimal DEFAULT_SELL_MULTIPLIER = new BigDecimal("0.99868");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final StockTradeRepository repository;

    public StockTradeService(StockTradeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public StockTradePageResponse findAll(Long userId, Integer year) {
        List<StockTrade> trades = year == null
                ? repository.findByUserId(userId)
                : repository.findByUserIdAndTradeYear(userId, year);
        return new StockTradePageResponse(summary(trades), trades);
    }

    @Transactional
    public StockTradeImportResponse importExcel(MultipartFile file, Integer year, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传する Excel 文件を选择してください");
        }
        if (year == null || year < 1900 || year > 2100) {
            throw new IllegalArgumentException("年份请输入 1900 到 2100 之间的数字");
        }

        List<String> errors = new ArrayList<>();
        List<StockTrade> trades = new ArrayList<>();
        int skipped = 0;

        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int lastRow = sheet.getLastRowNum();
            for (int i = 0; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (isBlank(row, formatter)) {
                    continue;
                }
                try {
                    trades.add(parseRow(row, formatter, year, userId, i + 1));
                } catch (RuntimeException exception) {
                    skipped++;
                    errors.add("第 " + (i + 1) + " 行：" + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Excel 文件读取失败", exception);
        }

        repository.deleteByUserIdAndTradeYear(userId, year);
        repository.saveAll(trades);
        log.info("Stock trades imported. userId={}, year={}, imported={}, skipped={}", userId, year, trades.size(), skipped);
        return new StockTradeImportResponse(trades.size(), skipped, errors);
    }

    @Transactional
    public StockTrade create(StockTradeRequest request, Long userId) {
        LocalDate tradeDate = parseTradeDate(request.tradeDate());
        String stockName = TextUtils.trimToEmpty(request.stockName());
        if (stockName.isBlank()) {
            throw new IllegalArgumentException("銘柄名を入力してください");
        }

        BigDecimal buyQuantity = value(request.buyQuantity());
        BigDecimal buyPrice = value(request.buyPrice());
        BigDecimal sellQuantity = value(request.sellQuantity());
        BigDecimal sellPrice = value(request.sellPrice());
        if (isZero(buyQuantity) && isZero(sellQuantity)) {
            throw new IllegalArgumentException("買付数量または売却数量を入力してください");
        }

        BigDecimal buyMultiplier = request.buyMultiplier() == null ? DEFAULT_BUY_MULTIPLIER : request.buyMultiplier();
        BigDecimal sellMultiplier = request.sellMultiplier() == null ? DEFAULT_SELL_MULTIPLIER : request.sellMultiplier();
        StockTrade trade = buildTrade(
                userId,
                tradeDate.getYear(),
                tradeDate,
                stockName,
                stockName + tradeDate.getMonthValue() + "." + tradeDate.getDayOfMonth(),
                buyQuantity,
                buyPrice,
                sellQuantity,
                sellPrice,
                buyMultiplier,
                sellMultiplier,
                0
        );
        StockTrade saved = repository.save(trade);
        log.info("Stock trade created manually. userId={}, tradeId={}, stockName={}", userId, saved.getId(), stockName);
        return saved;
    }

    @Transactional
    public void delete(Long id, Long userId) {
        StockTrade trade = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("股票交易记录不存在"));
        repository.delete(trade);
        log.info("Stock trade deleted. userId={}, tradeId={}", userId, id);
    }

    @Transactional
    public void deleteUserData(Long userId) {
        repository.deleteByUserId(userId);
    }

    private StockTrade parseRow(Row row, DataFormatter formatter, int year, Long userId, int rowNumber) {
        String sourceLabel = text(row, 7, formatter);
        StockDate stockDate = parseStockDate(sourceLabel, year);

        BigDecimal buyQuantity = number(row, 0, formatter);
        BigDecimal buyPrice = number(row, 1, formatter);
        BigDecimal sellQuantity = number(row, 3, formatter);
        BigDecimal sellPrice = number(row, 4, formatter);
        if (isZero(buyQuantity) && isZero(sellQuantity)) {
            throw new IllegalArgumentException("买入数量和卖出数量都为空");
        }

        return buildTrade(
                userId,
                year,
                stockDate.tradeDate(),
                stockDate.stockName(),
                sourceLabel,
                buyQuantity,
                buyPrice,
                sellQuantity,
                sellPrice,
                multiplier(row, 2, DEFAULT_BUY_MULTIPLIER),
                multiplier(row, 5, DEFAULT_SELL_MULTIPLIER),
                rowNumber
        );
    }

    private StockTrade buildTrade(
            Long userId,
            Integer year,
            LocalDate tradeDate,
            String stockName,
            String sourceLabel,
            BigDecimal buyQuantity,
            BigDecimal buyPrice,
            BigDecimal sellQuantity,
            BigDecimal sellPrice,
            BigDecimal buyMultiplier,
            BigDecimal sellMultiplier,
            Integer sourceRowNumber
    ) {
        BigDecimal buyGross = amount(buyQuantity.multiply(buyPrice));
        BigDecimal buyTotal = amount(buyGross.multiply(buyMultiplier));
        BigDecimal buyFee = amount(buyTotal.subtract(buyGross));
        BigDecimal sellGross = amount(sellQuantity.multiply(sellPrice));
        BigDecimal sellNet = amount(sellGross.multiply(sellMultiplier));
        BigDecimal sellFee = amount(sellGross.subtract(sellNet));
        BigDecimal profitLoss = amount(sellNet.subtract(buyTotal));

        return new StockTrade(
                userId,
                year,
                tradeDate,
                stockName,
                sourceLabel,
                buyQuantity,
                buyPrice,
                buyGross,
                buyFee,
                buyTotal,
                sellQuantity,
                sellPrice,
                sellGross,
                sellFee,
                sellNet,
                profitLoss,
                buyMultiplier,
                sellMultiplier,
                sourceRowNumber
        );
    }

    private StockTradeSummary summary(List<StockTrade> trades) {
        BigDecimal buyTotalAmount = ZERO;
        BigDecimal sellNetAmount = ZERO;
        BigDecimal buyFee = ZERO;
        BigDecimal sellFee = ZERO;
        BigDecimal profitLoss = ZERO;
        int winCount = 0;
        int lossCount = 0;
        for (StockTrade trade : trades) {
            buyTotalAmount = buyTotalAmount.add(value(trade.getBuyTotalAmount()));
            sellNetAmount = sellNetAmount.add(value(trade.getSellNetAmount()));
            buyFee = buyFee.add(value(trade.getBuyFee()));
            sellFee = sellFee.add(value(trade.getSellFee()));
            profitLoss = profitLoss.add(value(trade.getProfitLoss()));
            if (value(trade.getProfitLoss()).compareTo(BigDecimal.ZERO) > 0) {
                winCount++;
            } else if (value(trade.getProfitLoss()).compareTo(BigDecimal.ZERO) < 0) {
                lossCount++;
            }
        }
        return new StockTradeSummary(
                trades.size(),
                winCount,
                lossCount,
                amount(buyTotalAmount),
                amount(sellNetAmount),
                amount(buyFee),
                amount(sellFee),
                amount(profitLoss)
        );
    }

    private StockDate parseStockDate(String sourceLabel, int year) {
        if (sourceLabel.isBlank()) {
            throw new IllegalArgumentException("H列的股票名+日期为空");
        }
        Matcher matcher = STOCK_DATE_PATTERN.matcher(sourceLabel);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("H列格式需要类似：东兴6.15");
        }
        String stockName = TextUtils.trimToEmpty(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));
        try {
            return new StockDate(stockName, LocalDate.of(year, month, day));
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("H列日期无效：" + month + "." + day);
        }
    }

    private LocalDate parseTradeDate(String value) {
        try {
            return LocalDate.parse(TextUtils.trimToEmpty(value));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("取引日は yyyy-MM-dd 形式で入力してください");
        }
    }

    private BigDecimal multiplier(Row row, int column, BigDecimal defaultValue) {
        String formula = formulaOrText(row, column);
        if (formula.isBlank()) {
            return defaultValue;
        }
        Matcher matcher = LAST_MULTIPLIER_PATTERN.matcher(formula);
        if (!matcher.find()) {
            return defaultValue;
        }
        return new BigDecimal(matcher.group(1));
    }

    private String formulaOrText(Row row, int column) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(column);
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return cell.getCellFormula();
        }
        if (cell.getCellType() == CellType.STRING) {
            String value = TextUtils.trimToEmpty(cell.getStringCellValue());
            return value.startsWith("'=") ? value.substring(1) : value;
        }
        return "";
    }

    private BigDecimal number(Row row, int column, DataFormatter formatter) {
        if (row == null) {
            return ZERO;
        }
        Cell cell = row.getCell(column);
        if (cell == null) {
            return ZERO;
        }
        if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
            try {
                return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(4, RoundingMode.HALF_UP);
            } catch (IllegalStateException ignored) {
                // Fall through to formatted text parsing.
            }
        }
        String value = text(row, column, formatter).replace(",", "");
        if (value.isBlank() || value.startsWith("=") || value.startsWith("'=")) {
            return ZERO;
        }
        return new BigDecimal(value).setScale(4, RoundingMode.HALF_UP);
    }

    private String text(Row row, int column, DataFormatter formatter) {
        Cell cell = row == null ? null : row.getCell(column);
        return cell == null ? "" : TextUtils.trimToEmpty(formatter.formatCellValue(cell));
    }

    private boolean isBlank(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int i : new int[] {0, 1, 2, 3, 4, 5, 7}) {
            if (!text(row, i, formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal amount(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }

    private boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private record StockDate(String stockName, LocalDate tradeDate) {
    }
}
