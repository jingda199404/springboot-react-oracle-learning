package com.example.employee.stock;

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
public class StockTradeRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<StockTrade> rowMapper = (rs, rowNum) -> {
        StockTrade trade = new StockTrade(
                rs.getLong("USER_ID"),
                rs.getInt("TRADE_YEAR"),
                rs.getDate("TRADE_DATE").toLocalDate(),
                rs.getString("STOCK_NAME"),
                rs.getString("SOURCE_LABEL"),
                rs.getBigDecimal("BUY_QUANTITY"),
                rs.getBigDecimal("BUY_PRICE"),
                rs.getBigDecimal("BUY_GROSS_AMOUNT"),
                rs.getBigDecimal("BUY_FEE"),
                rs.getBigDecimal("BUY_TOTAL_AMOUNT"),
                rs.getBigDecimal("SELL_QUANTITY"),
                rs.getBigDecimal("SELL_PRICE"),
                rs.getBigDecimal("SELL_GROSS_AMOUNT"),
                rs.getBigDecimal("SELL_FEE"),
                rs.getBigDecimal("SELL_NET_AMOUNT"),
                rs.getBigDecimal("PROFIT_LOSS"),
                rs.getBigDecimal("BUY_MULTIPLIER"),
                rs.getBigDecimal("SELL_MULTIPLIER"),
                rs.getInt("SOURCE_ROW_NUMBER")
        );
        trade.setId(rs.getLong("ID"));
        return trade;
    };

    public StockTradeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<StockTrade> findByUserId(Long userId) {
        return jdbcTemplate.query("SELECT * FROM STOCK_TRADES WHERE USER_ID = ? ORDER BY TRADE_DATE ASC, SOURCE_ROW_NUMBER ASC", rowMapper, userId);
    }

    public List<StockTrade> findByUserIdAndTradeYear(Long userId, Integer tradeYear) {
        return jdbcTemplate.query("""
                SELECT * FROM STOCK_TRADES
                 WHERE USER_ID = ? AND TRADE_YEAR = ?
                 ORDER BY TRADE_DATE ASC, SOURCE_ROW_NUMBER ASC
                """, rowMapper, userId, tradeYear);
    }

    public Optional<StockTrade> findByIdAndUserId(Long id, Long userId) {
        return jdbcTemplate.query("SELECT * FROM STOCK_TRADES WHERE ID = ? AND USER_ID = ?", rowMapper, id, userId).stream().findFirst();
    }

    public StockTrade save(StockTrade trade) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO STOCK_TRADES
                    (USER_ID, TRADE_YEAR, TRADE_DATE, STOCK_NAME, SOURCE_LABEL, BUY_QUANTITY, BUY_PRICE, BUY_GROSS_AMOUNT,
                     BUY_FEE, BUY_TOTAL_AMOUNT, SELL_QUANTITY, SELL_PRICE, SELL_GROSS_AMOUNT, SELL_FEE, SELL_NET_AMOUNT,
                     PROFIT_LOSS, BUY_MULTIPLIER, SELL_MULTIPLIER, SOURCE_ROW_NUMBER)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, trade.getUserId());
            statement.setInt(2, trade.getTradeYear());
            statement.setDate(3, Date.valueOf(trade.getTradeDate()));
            statement.setString(4, trade.getStockName());
            statement.setString(5, trade.getSourceLabel());
            statement.setBigDecimal(6, trade.getBuyQuantity());
            statement.setBigDecimal(7, trade.getBuyPrice());
            statement.setBigDecimal(8, trade.getBuyGrossAmount());
            statement.setBigDecimal(9, trade.getBuyFee());
            statement.setBigDecimal(10, trade.getBuyTotalAmount());
            statement.setBigDecimal(11, trade.getSellQuantity());
            statement.setBigDecimal(12, trade.getSellPrice());
            statement.setBigDecimal(13, trade.getSellGrossAmount());
            statement.setBigDecimal(14, trade.getSellFee());
            statement.setBigDecimal(15, trade.getSellNetAmount());
            statement.setBigDecimal(16, trade.getProfitLoss());
            statement.setBigDecimal(17, trade.getBuyMultiplier());
            statement.setBigDecimal(18, trade.getSellMultiplier());
            statement.setInt(19, trade.getSourceRowNumber());
            return statement;
        }, keyHolder);
        trade.setId(keyHolder.getKey().longValue());
        return trade;
    }

    public void saveAll(List<StockTrade> trades) {
        trades.forEach(this::save);
    }

    public void delete(StockTrade trade) {
        jdbcTemplate.update("DELETE FROM STOCK_TRADES WHERE ID = ?", trade.getId());
    }

    public void deleteByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM STOCK_TRADES WHERE USER_ID = ?", userId);
    }

    public void deleteByUserIdAndTradeYear(Long userId, Integer tradeYear) {
        jdbcTemplate.update("DELETE FROM STOCK_TRADES WHERE USER_ID = ? AND TRADE_YEAR = ?", userId, tradeYear);
    }
}
