package com.example.employee.accounting;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AccountingEntryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AccountingEntry> rowMapper = (rs, rowNum) -> {
        AccountingEntry entry = new AccountingEntry();
        entry.setId(rs.getLong("ID"));
        entry.setEntryDate(rs.getDate("ENTRY_DATE").toLocalDate());
        entry.setType(AccountingEntryType.valueOf(rs.getString("TYPE")));
        entry.setCategory(rs.getString("CATEGORY"));
        entry.setAmount(rs.getBigDecimal("AMOUNT"));
        entry.setPaymentMethod(rs.getString("PAYMENT_METHOD"));
        entry.setMemo(rs.getString("MEMO"));
        entry.setUserId(rs.getLong("USER_ID"));
        return entry;
    };

    public AccountingEntryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AccountingEntry> findByUserId(Long userId) {
        return jdbcTemplate.query("SELECT * FROM ACCOUNTING_ENTRIES WHERE USER_ID = ? ORDER BY ENTRY_DATE DESC, ID DESC", rowMapper, userId);
    }

    public Optional<AccountingEntry> findByIdAndUserId(Long id, Long userId) {
        return jdbcTemplate.query("SELECT * FROM ACCOUNTING_ENTRIES WHERE ID = ? AND USER_ID = ?", rowMapper, id, userId).stream().findFirst();
    }

    public AccountingEntry save(AccountingEntry entry) {
        if (entry.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO ACCOUNTING_ENTRIES (ENTRY_DATE, TYPE, CATEGORY, AMOUNT, PAYMENT_METHOD, MEMO, USER_ID)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                bind(statement, entry);
                return statement;
            }, keyHolder);
            entry.setId(keyHolder.getKey().longValue());
        } else {
            jdbcTemplate.update("""
                    UPDATE ACCOUNTING_ENTRIES
                       SET ENTRY_DATE = ?, TYPE = ?, CATEGORY = ?, AMOUNT = ?, PAYMENT_METHOD = ?, MEMO = ?, USER_ID = ?
                     WHERE ID = ?
                    """, Date.valueOf(entry.getEntryDate()), entry.getType().name(), entry.getCategory(), entry.getAmount(),
                    entry.getPaymentMethod(), entry.getMemo(), entry.getUserId(), entry.getId());
        }
        return entry;
    }

    public void delete(AccountingEntry entry) {
        jdbcTemplate.update("DELETE FROM ACCOUNTING_ENTRIES WHERE ID = ?", entry.getId());
    }

    public void deleteByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM ACCOUNTING_ENTRIES WHERE USER_ID = ?", userId);
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM ACCOUNTING_ENTRIES");
    }

    private void bind(PreparedStatement statement, AccountingEntry entry) throws java.sql.SQLException {
        statement.setDate(1, Date.valueOf(entry.getEntryDate()));
        statement.setString(2, entry.getType().name());
        statement.setString(3, entry.getCategory());
        statement.setBigDecimal(4, entry.getAmount());
        statement.setString(5, entry.getPaymentMethod());
        statement.setString(6, entry.getMemo());
        statement.setLong(7, entry.getUserId());
    }
}
