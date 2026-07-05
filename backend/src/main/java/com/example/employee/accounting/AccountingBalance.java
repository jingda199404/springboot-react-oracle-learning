package com.example.employee.accounting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AccountingBalance {

    private Long id;

    private LocalDate asOfDate;

    private AccountingBalanceType type;

    private String accountName;

    private String category;

    private BigDecimal amount;

    private Integer repaymentDay;

    private String repaymentAccountName;

    private LocalDate lastRepaymentDate;

    private String memo;

    private Long userId;

    public AccountingBalance() {
    }

    public AccountingBalance(LocalDate asOfDate, AccountingBalanceType type, String accountName, String category, BigDecimal amount, Integer repaymentDay, String repaymentAccountName, LocalDate lastRepaymentDate, String memo, Long userId) {
        this.asOfDate = asOfDate;
        this.type = type;
        this.accountName = accountName;
        this.category = category;
        this.amount = amount;
        this.repaymentDay = repaymentDay;
        this.repaymentAccountName = repaymentAccountName;
        this.lastRepaymentDate = lastRepaymentDate;
        this.memo = memo;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
    }

    public AccountingBalanceType getType() {
        return type;
    }

    public void setType(AccountingBalanceType type) {
        this.type = type;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
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

    public Integer getRepaymentDay() {
        return repaymentDay;
    }

    public void setRepaymentDay(Integer repaymentDay) {
        this.repaymentDay = repaymentDay;
    }

    public String getRepaymentAccountName() {
        return repaymentAccountName;
    }

    public void setRepaymentAccountName(String repaymentAccountName) {
        this.repaymentAccountName = repaymentAccountName;
    }

    public LocalDate getLastRepaymentDate() {
        return lastRepaymentDate;
    }

    public void setLastRepaymentDate(LocalDate lastRepaymentDate) {
        this.lastRepaymentDate = lastRepaymentDate;
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
