package com.example.employee.stock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;

public class StockTrade {

    private Long id;

    private Long userId;

    private Integer tradeYear;

    private LocalDate tradeDate;

    private String stockName;

    private String sourceLabel;

    private BigDecimal buyQuantity;

    private BigDecimal buyPrice;

    private BigDecimal buyGrossAmount;

    private BigDecimal buyFee;

    private BigDecimal buyTotalAmount;

    private BigDecimal sellQuantity;

    private BigDecimal sellPrice;

    private BigDecimal sellGrossAmount;

    private BigDecimal sellFee;

    private BigDecimal sellNetAmount;

    private BigDecimal profitLoss;

    private BigDecimal buyMultiplier;

    private BigDecimal sellMultiplier;

    private Integer sourceRowNumber;

    public StockTrade() {
    }

    public StockTrade(
            Long userId,
            Integer tradeYear,
            LocalDate tradeDate,
            String stockName,
            String sourceLabel,
            BigDecimal buyQuantity,
            BigDecimal buyPrice,
            BigDecimal buyGrossAmount,
            BigDecimal buyFee,
            BigDecimal buyTotalAmount,
            BigDecimal sellQuantity,
            BigDecimal sellPrice,
            BigDecimal sellGrossAmount,
            BigDecimal sellFee,
            BigDecimal sellNetAmount,
            BigDecimal profitLoss,
            BigDecimal buyMultiplier,
            BigDecimal sellMultiplier,
            Integer sourceRowNumber
    ) {
        this.userId = userId;
        this.tradeYear = tradeYear;
        this.tradeDate = tradeDate;
        this.stockName = stockName;
        this.sourceLabel = sourceLabel;
        this.buyQuantity = buyQuantity;
        this.buyPrice = buyPrice;
        this.buyGrossAmount = buyGrossAmount;
        this.buyFee = buyFee;
        this.buyTotalAmount = buyTotalAmount;
        this.sellQuantity = sellQuantity;
        this.sellPrice = sellPrice;
        this.sellGrossAmount = sellGrossAmount;
        this.sellFee = sellFee;
        this.sellNetAmount = sellNetAmount;
        this.profitLoss = profitLoss;
        this.buyMultiplier = buyMultiplier;
        this.sellMultiplier = sellMultiplier;
        this.sourceRowNumber = sourceRowNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonIgnore
    public Long getUserId() {
        return userId;
    }

    public Integer getTradeYear() {
        return tradeYear;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public String getStockName() {
        return stockName;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public BigDecimal getBuyQuantity() {
        return buyQuantity;
    }

    public BigDecimal getBuyPrice() {
        return buyPrice;
    }

    public BigDecimal getBuyGrossAmount() {
        return buyGrossAmount;
    }

    public BigDecimal getBuyFee() {
        return buyFee;
    }

    public BigDecimal getBuyTotalAmount() {
        return buyTotalAmount;
    }

    public BigDecimal getSellQuantity() {
        return sellQuantity;
    }

    public BigDecimal getSellPrice() {
        return sellPrice;
    }

    public BigDecimal getSellGrossAmount() {
        return sellGrossAmount;
    }

    public BigDecimal getSellFee() {
        return sellFee;
    }

    public BigDecimal getSellNetAmount() {
        return sellNetAmount;
    }

    public BigDecimal getProfitLoss() {
        return profitLoss;
    }

    public BigDecimal getBuyMultiplier() {
        return buyMultiplier;
    }

    public BigDecimal getSellMultiplier() {
        return sellMultiplier;
    }

    public Integer getSourceRowNumber() {
        return sourceRowNumber;
    }
}
