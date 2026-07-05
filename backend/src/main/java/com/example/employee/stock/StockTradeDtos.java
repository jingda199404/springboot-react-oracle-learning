package com.example.employee.stock;

import java.math.BigDecimal;
import java.util.List;

record StockTradePageResponse(StockTradeSummary summary, List<StockTrade> trades) {
}

record StockTradeSummary(
        int tradeCount,
        int winCount,
        int lossCount,
        BigDecimal buyTotalAmount,
        BigDecimal sellNetAmount,
        BigDecimal buyFee,
        BigDecimal sellFee,
        BigDecimal profitLoss
) {
}

record StockTradeImportResponse(int imported, int skipped, List<String> errors) {
}

record StockTradeRequest(
        String tradeDate,
        String stockName,
        BigDecimal buyQuantity,
        BigDecimal buyPrice,
        BigDecimal sellQuantity,
        BigDecimal sellPrice,
        BigDecimal buyMultiplier,
        BigDecimal sellMultiplier
) {
}
