package com.example.employee.accounting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AccountingEntry {

    private Long id;

    private LocalDate entryDate;

    private AccountingEntryType type;

    private String category;

    private BigDecimal amount;

    private String paymentMethod;

    private String memo;

    private Long userId;

    public AccountingEntry() {
    }

    public AccountingEntry(LocalDate entryDate, AccountingEntryType type, String category, BigDecimal amount, String paymentMethod, String memo, Long userId) {
        this.entryDate = entryDate;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.memo = memo;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public AccountingEntryType getType() {
        return type;
    }

    public void setType(AccountingEntryType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @JsonIgnore
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
