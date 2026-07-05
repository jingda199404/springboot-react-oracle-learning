package com.example.employee.japanese;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JapaneseWordProgressRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JapaneseWordRepository wordRepository;

    public JapaneseWordProgressRepository(JdbcTemplate jdbcTemplate, JapaneseWordRepository wordRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.wordRepository = wordRepository;
    }

    public Optional<JapaneseWordProgress> findByUserIdAndWordId(Long userId, Long wordId) {
        return jdbcTemplate.query("SELECT * FROM JAPANESE_WORD_PROGRESS WHERE USER_ID = ? AND WORD_ID = ?", rowMapper(), userId, wordId)
                .stream().findFirst();
    }

    public long countByUserIdAndNextReviewDateLessThanEqual(Long userId, LocalDate date) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM JAPANESE_WORD_PROGRESS
                 WHERE USER_ID = ? AND NEXT_REVIEW_DATE <= ?
                """, Long.class, userId, Date.valueOf(date));
        return count == null ? 0 : count;
    }

    public List<JapaneseWordProgress> findByUserIdAndNextReviewDateLessThanEqualOrderByNextReviewDateAscIdAsc(Long userId, LocalDate date) {
        return jdbcTemplate.query("""
                SELECT * FROM JAPANESE_WORD_PROGRESS
                 WHERE USER_ID = ? AND NEXT_REVIEW_DATE <= ?
                 ORDER BY NEXT_REVIEW_DATE ASC, ID ASC
                """, rowMapper(), userId, Date.valueOf(date));
    }

    public JapaneseWordProgress save(JapaneseWordProgress progress) {
        if (progress.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO JAPANESE_WORD_PROGRESS
                        (USER_ID, WORD_ID, MASTERY_STATUS, REVIEW_COUNT, NEXT_REVIEW_DATE, LAST_REVIEWED_AT)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                bind(statement, progress);
                return statement;
            }, keyHolder);
            progress.setId(keyHolder.getKey().longValue());
        } else {
            jdbcTemplate.update("""
                    UPDATE JAPANESE_WORD_PROGRESS
                       SET USER_ID = ?, WORD_ID = ?, MASTERY_STATUS = ?, REVIEW_COUNT = ?, NEXT_REVIEW_DATE = ?, LAST_REVIEWED_AT = ?
                     WHERE ID = ?
                    """, progress.getUserId(), progress.getWord().getId(), progress.getMasteryStatus(), progress.getReviewCount(),
                    progress.getNextReviewDate() == null ? null : Date.valueOf(progress.getNextReviewDate()),
                    progress.getLastReviewedAt() == null ? null : Timestamp.from(progress.getLastReviewedAt()), progress.getId());
        }
        return progress;
    }

    public void deleteByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM JAPANESE_WORD_PROGRESS WHERE USER_ID = ?", userId);
    }

    private RowMapper<JapaneseWordProgress> rowMapper() {
        return (rs, rowNum) -> {
            JapaneseWordProgress progress = new JapaneseWordProgress();
            progress.setId(rs.getLong("ID"));
            progress.setUserId(rs.getLong("USER_ID"));
            progress.setWord(wordRepository.findById(rs.getLong("WORD_ID")).orElse(null));
            progress.setMasteryStatus(rs.getString("MASTERY_STATUS"));
            progress.setReviewCount(rs.getInt("REVIEW_COUNT"));
            Date nextReviewDate = rs.getDate("NEXT_REVIEW_DATE");
            progress.setNextReviewDate(nextReviewDate == null ? null : nextReviewDate.toLocalDate());
            Timestamp lastReviewedAt = rs.getTimestamp("LAST_REVIEWED_AT");
            progress.setLastReviewedAt(lastReviewedAt == null ? null : lastReviewedAt.toInstant());
            return progress;
        };
    }

    private void bind(PreparedStatement statement, JapaneseWordProgress progress) throws java.sql.SQLException {
        statement.setLong(1, progress.getUserId());
        statement.setLong(2, progress.getWord().getId());
        statement.setString(3, progress.getMasteryStatus());
        statement.setInt(4, progress.getReviewCount());
        statement.setDate(5, progress.getNextReviewDate() == null ? null : Date.valueOf(progress.getNextReviewDate()));
        statement.setTimestamp(6, progress.getLastReviewedAt() == null ? null : Timestamp.from(progress.getLastReviewedAt()));
    }
}
