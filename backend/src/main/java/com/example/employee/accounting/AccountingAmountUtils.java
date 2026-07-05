package com.example.employee.accounting;

import java.math.BigDecimal;

public final class AccountingAmountUtils {

    private AccountingAmountUtils() {
    }

    public static BigDecimal balanceDelta(AccountingEntryType entryType, AccountingBalanceType balanceType, BigDecimal amount) {
        if (balanceType == AccountingBalanceType.ASSET) {
            return entryType == AccountingEntryType.INCOME ? amount : amount.negate();
        }
        return entryType == AccountingEntryType.EXPENSE ? amount : amount.negate();
    }
}
