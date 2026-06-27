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
@Table(name = "ACCOUNTING_ENTRIES")
public class AccountingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ENTRY_DATE", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false, length = 20)
    private AccountingEntryType type;

    @Column(name = "CATEGORY", nullable = false, length = 80)
    private String category;

    @Column(name = "AMOUNT", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "PAYMENT_METHOD", length = 100)
    private String paymentMethod;

    @Column(name = "MEMO", length = 255)
    private String memo;

    @Column(name = "USER_ID")
    private Long userId;

    protected AccountingEntry() {
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
}
