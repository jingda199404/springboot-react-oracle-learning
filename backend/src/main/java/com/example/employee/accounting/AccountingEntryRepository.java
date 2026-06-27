package com.example.employee.accounting;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingEntryRepository extends JpaRepository<AccountingEntry, Long> {

    List<AccountingEntry> findByUserId(Long userId, Sort sort);

    Optional<AccountingEntry> findByIdAndUserId(Long id, Long userId);
}
