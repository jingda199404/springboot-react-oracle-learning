package com.example.employee.accounting;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingBalanceRepository extends JpaRepository<AccountingBalance, Long> {

    List<AccountingBalance> findByUserId(Long userId, Sort sort);

    Optional<AccountingBalance> findByIdAndUserId(Long id, Long userId);

    Optional<AccountingBalance> findFirstByUserIdAndAccountName(Long userId, String accountName);

    Optional<AccountingBalance> findFirstByUserIdAndTypeAndAccountName(Long userId, AccountingBalanceType type, String accountName);

    List<AccountingBalance> findByTypeAndRepaymentDay(AccountingBalanceType type, Integer repaymentDay);

    List<AccountingBalance> findByUserIdAndTypeAndRepaymentDay(Long userId, AccountingBalanceType type, Integer repaymentDay);
}
