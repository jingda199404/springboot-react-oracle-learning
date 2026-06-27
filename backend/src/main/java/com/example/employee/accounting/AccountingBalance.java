package com.example.employee.accounting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ACCOUNTING_BALANCES")
public class AccountingBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "AS_OF_DATE", nullable = false)
    private LocalDate asOfDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false, length = 20)
    private AccountingBalanceType type;

    @Column(name = "ACCOUNT_NAME", nullable = false, length = 100)
    private String accountName;

    @Column(name = "CATEGORY", nullable = false, length = 80)
    private String category;

    @Column(name = "AMOUNT", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "REPAYMENT_DAY")
    private Integer repaymentDay;

    @Column(name = "REPAYMENT_ACCOUNT_NAME", length = 100)
    private String repaymentAccountName;

    @Column(name = "LAST_REPAYMENT_DATE")
    private LocalDate lastRepaymentDate;

    @Column(name = "MEMO", length = 255)
    private String memo;

    @Column(name = "USER_ID")
    private Long userId;

    protected AccountingBalance() {
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
}
