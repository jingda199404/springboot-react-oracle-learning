package com.example.jingda.batch.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditCardRepaymentJob {

    private static final Logger log = LoggerFactory.getLogger(CreditCardRepaymentJob.class);

    private final AccountingBalanceRepository repository;

    public CreditCardRepaymentJob(AccountingBalanceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int repayDueCards(LocalDate date) {
        List<AccountingBalance> dueLiabilities = repository.findByTypeAndRepaymentDay(
                AccountingBalanceType.LIABILITY,
                date.getDayOfMonth()
        );
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
            log.info("Credit card repaid. userId={}, liabilityId={}, repaymentAccountId={}, amount={}, date={}",
                    liability.getUserId(), liability.getId(), repaymentAccount.getId(), repaymentAmount, date);
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
