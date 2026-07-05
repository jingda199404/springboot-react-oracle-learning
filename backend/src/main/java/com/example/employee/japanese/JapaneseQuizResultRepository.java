package com.example.employee.japanese;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JapaneseQuizResultRepository {

    private final JdbcTemplate jdbcTemplate;

    public JapaneseQuizResultRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countByUserId(Long userId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM JAPANESE_QUIZ_RESULTS WHERE USER_ID = ?", Long.class, userId);
        return count == null ? 0 : count;
    }

    public long countByUserIdAndCorrectTrue(Long userId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM JAPANESE_QUIZ_RESULTS WHERE USER_ID = ? AND CORRECT = TRUE", Long.class, userId);
        return count == null ? 0 : count;
    }

    public JapaneseQuizResult save(JapaneseQuizResult result) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO JAPANESE_QUIZ_RESULTS (USER_ID, QUESTION_ID, SELECTED_OPTION, CORRECT, ANSWERED_AT)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, result.getUserId());
            statement.setLong(2, result.getQuestion().getId());
            statement.setString(3, result.getSelectedOption());
            statement.setBoolean(4, result.isCorrect());
            statement.setTimestamp(5, Timestamp.from(result.getAnsweredAt()));
            return statement;
        }, keyHolder);
        result.setId(keyHolder.getKey().longValue());
        return result;
    }

    public void deleteByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM JAPANESE_QUIZ_RESULTS WHERE USER_ID = ?", userId);
    }
}
