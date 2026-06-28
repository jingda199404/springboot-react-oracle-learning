package com.example.jingda.batch.accounting;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingBalanceRepository extends JpaRepository<AccountingBalance, Long> {

    List<AccountingBalance> findByTypeAndRepaymentDay(AccountingBalanceType type, Integer repaymentDay);

    Optional<AccountingBalance> findFirstByUserIdAndTypeAndAccountName(Long userId, AccountingBalanceType type, String accountName);
}
