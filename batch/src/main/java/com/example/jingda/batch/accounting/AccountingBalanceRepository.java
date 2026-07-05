package com.example.jingda.batch.accounting;

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
public class AccountingBalanceRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AccountingBalance> rowMapper = (rs, rowNum) -> {
        AccountingBalance balance = new AccountingBalance();
        balance.setId(rs.getLong("ID"));
        balance.setAsOfDate(rs.getDate("AS_OF_DATE").toLocalDate());
        balance.setType(AccountingBalanceType.valueOf(rs.getString("TYPE")));
        balance.setAccountName(rs.getString("ACCOUNT_NAME"));
        balance.setCategory(rs.getString("CATEGORY"));
        balance.setAmount(rs.getBigDecimal("AMOUNT"));
        balance.setRepaymentDay((Integer) rs.getObject("REPAYMENT_DAY"));
        balance.setRepaymentAccountName(rs.getString("REPAYMENT_ACCOUNT_NAME"));
        Date lastRepaymentDate = rs.getDate("LAST_REPAYMENT_DATE");
        balance.setLastRepaymentDate(lastRepaymentDate == null ? null : lastRepaymentDate.toLocalDate());
        balance.setMemo(rs.getString("MEMO"));
        balance.setUserId(rs.getLong("USER_ID"));
        return balance;
    };

    public AccountingBalanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AccountingBalance> findById(Long id) {
        return jdbcTemplate.query("SELECT * FROM ACCOUNTING_BALANCES WHERE ID = ?", rowMapper, id).stream().findFirst();
    }

    public Optional<AccountingBalance> findFirstByUserIdAndTypeAndAccountName(Long userId, AccountingBalanceType type, String accountName) {
        return jdbcTemplate.query("""
                SELECT * FROM ACCOUNTING_BALANCES
                 WHERE USER_ID = ? AND TYPE = ? AND ACCOUNT_NAME = ?
                 ORDER BY ID ASC LIMIT 1
                """, rowMapper, userId, type.name(), accountName).stream().findFirst();
    }

    public List<AccountingBalance> findByTypeAndRepaymentDay(AccountingBalanceType type, Integer repaymentDay) {
        return jdbcTemplate.query("""
                SELECT * FROM ACCOUNTING_BALANCES
                 WHERE TYPE = ? AND REPAYMENT_DAY = ?
                 ORDER BY ID ASC
                """, rowMapper, type.name(), repaymentDay);
    }

    public AccountingBalance save(AccountingBalance balance) {
        if (balance.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO ACCOUNTING_BALANCES
                        (AS_OF_DATE, TYPE, ACCOUNT_NAME, CATEGORY, AMOUNT, REPAYMENT_DAY, REPAYMENT_ACCOUNT_NAME, LAST_REPAYMENT_DATE, MEMO, USER_ID)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                bind(statement, balance);
                return statement;
            }, keyHolder);
            balance.setId(keyHolder.getKey().longValue());
        } else {
            jdbcTemplate.update("""
                    UPDATE ACCOUNTING_BALANCES
                       SET AS_OF_DATE = ?, TYPE = ?, ACCOUNT_NAME = ?, CATEGORY = ?, AMOUNT = ?, REPAYMENT_DAY = ?,
                           REPAYMENT_ACCOUNT_NAME = ?, LAST_REPAYMENT_DATE = ?, MEMO = ?, USER_ID = ?
                     WHERE ID = ?
                    """, Date.valueOf(balance.getAsOfDate()), balance.getType().name(), balance.getAccountName(), balance.getCategory(),
                    balance.getAmount(), balance.getRepaymentDay(), balance.getRepaymentAccountName(),
                    balance.getLastRepaymentDate() == null ? null : Date.valueOf(balance.getLastRepaymentDate()),
                    balance.getMemo(), balance.getUserId(), balance.getId());
        }
        return balance;
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM ACCOUNTING_BALANCES");
    }

    private void bind(PreparedStatement statement, AccountingBalance balance) throws java.sql.SQLException {
        statement.setDate(1, Date.valueOf(balance.getAsOfDate()));
        statement.setString(2, balance.getType().name());
        statement.setString(3, balance.getAccountName());
        statement.setString(4, balance.getCategory());
        statement.setBigDecimal(5, balance.getAmount());
        statement.setObject(6, balance.getRepaymentDay());
        statement.setString(7, balance.getRepaymentAccountName());
        statement.setDate(8, balance.getLastRepaymentDate() == null ? null : Date.valueOf(balance.getLastRepaymentDate()));
        statement.setString(9, balance.getMemo());
        statement.setLong(10, balance.getUserId());
    }
}
