package com.example.employee.japanese;

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
public class JapaneseWordRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<JapaneseWord> rowMapper = (rs, rowNum) -> {
        JapaneseWord word = new JapaneseWord();
        word.setId(rs.getLong("ID"));
        word.setWord(rs.getString("WORD"));
        word.setReading(rs.getString("READING"));
        word.setMeaningZh(rs.getString("MEANING_ZH"));
        word.setPartOfSpeech(rs.getString("PART_OF_SPEECH"));
        word.setCategory(rs.getString("CATEGORY"));
        word.setLevel(rs.getString("LEVEL_NAME"));
        word.setExampleJa(rs.getString("EXAMPLE_JA"));
        word.setExampleZh(rs.getString("EXAMPLE_ZH"));
        return word;
    };

    public JapaneseWordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM JAPANESE_WORDS", Long.class);
        return count == null ? 0 : count;
    }

    public List<JapaneseWord> findAll() {
        return jdbcTemplate.query("SELECT * FROM JAPANESE_WORDS ORDER BY LEVEL_NAME ASC, ID ASC", rowMapper);
    }

    public List<JapaneseWord> findAllById() {
        return jdbcTemplate.query("SELECT * FROM JAPANESE_WORDS ORDER BY ID ASC", rowMapper);
    }

    public List<JapaneseWord> findByCategory(String category) {
        return jdbcTemplate.query("SELECT * FROM JAPANESE_WORDS WHERE CATEGORY = ? ORDER BY LEVEL_NAME ASC, ID ASC", rowMapper, category);
    }

    public Optional<JapaneseWord> findById(Long id) {
        return jdbcTemplate.query("SELECT * FROM JAPANESE_WORDS WHERE ID = ?", rowMapper, id).stream().findFirst();
    }

    public Optional<JapaneseWord> findFirstByWordAndReading(String word, String reading) {
        return jdbcTemplate.query("SELECT * FROM JAPANESE_WORDS WHERE WORD = ? AND READING = ? ORDER BY ID ASC LIMIT 1", rowMapper, word, reading)
                .stream().findFirst();
    }

    public JapaneseWord save(JapaneseWord word) {
        if (word.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO JAPANESE_WORDS
                        (WORD, READING, MEANING_ZH, PART_OF_SPEECH, CATEGORY, LEVEL_NAME, EXAMPLE_JA, EXAMPLE_ZH)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, word.getWord());
                statement.setString(2, word.getReading());
                statement.setString(3, word.getMeaningZh());
                statement.setString(4, word.getPartOfSpeech());
                statement.setString(5, word.getCategory());
                statement.setString(6, word.getLevel());
                statement.setString(7, word.getExampleJa());
                statement.setString(8, word.getExampleZh());
                return statement;
            }, keyHolder);
            word.setId(keyHolder.getKey().longValue());
        }
        return word;
    }
}
