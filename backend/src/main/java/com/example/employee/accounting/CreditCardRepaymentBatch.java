package com.example.employee.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditCardRepaymentBatch {

    private final AccountingBalanceRepository repository;
    private final ZoneId zoneId;

    public CreditCardRepaymentBatch(
            AccountingBalanceRepository repository,
            @Value("${app.accounting.repayment.zone:Asia/Tokyo}") String zone
    ) {
        this.repository = repository;
        this.zoneId = ZoneId.of(zone);
    }

    @Scheduled(cron = "${app.accounting.repayment.cron:0 5 0 * * *}", zone = "${app.accounting.repayment.zone:Asia/Tokyo}")
    @Transactional
    public void runDailyRepayment() {
        repayDueCards(LocalDate.now(zoneId));
    }

    @Transactional
    public int repayDueCards(LocalDate date) {
        return repayDueCards(repository.findByTypeAndRepaymentDay(AccountingBalanceType.LIABILITY, date.getDayOfMonth()), date);
    }

    @Transactional
    public int repayDueCards(LocalDate date, Long userId) {
        return repayDueCards(repository.findByUserIdAndTypeAndRepaymentDay(userId, AccountingBalanceType.LIABILITY, date.getDayOfMonth()), date);
    }

    private int repayDueCards(List<AccountingBalance> dueLiabilities, LocalDate date) {
        int processed = 0;
        for (AccountingBalance liability : dueLiabilities) {
            if (shouldSkip(liability, date)) {
                continue;
            }

            AccountingBalance repaymentAccount = repository
                    .findFirstByUserIdAndTypeAndAccountName(liability.getUserId(), AccountingBalanceType.ASSET, liability.getRepaymentAccountName())
                    .orElse(null);
            if (repaymentAccount == null) {
                continue;
            }

            BigDecimal repaymentAmount = liability.getAmount();
            repaymentAccount.setAmount(repaymentAccount.getAmount().subtract(repaymentAmount));
            repaymentAccount.setAsOfDate(date);
            liability.setAmount(BigDecimal.ZERO);
            liability.setAsOfDate(date);
            liability.setLastRepaymentDate(date);
            processed++;
        }
        return processed;
    }

    private boolean shouldSkip(AccountingBalance liability, LocalDate date) {
        return liability.getUserId() == null
                || liability.getAmount().compareTo(BigDecimal.ZERO) <= 0
                || liability.getRepaymentAccountName() == null
                || liability.getRepaymentAccountName().isBlank()
                || date.equals(liability.getLastRepaymentDate());
    }
}
