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
public class JapaneseQuizQuestionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<JapaneseQuizQuestion> rowMapper = (rs, rowNum) -> {
        JapaneseQuizQuestion question = new JapaneseQuizQuestion();
        question.setId(rs.getLong("ID"));
        question.setQuestionType(rs.getString("QUESTION_TYPE"));
        question.setPrompt(rs.getString("PROMPT"));
        question.setOptionA(rs.getString("OPTION_A"));
        question.setOptionB(rs.getString("OPTION_B"));
        question.setOptionC(rs.getString("OPTION_C"));
        question.setOptionD(rs.getString("OPTION_D"));
        question.setCorrectOption(rs.getString("CORRECT_OPTION"));
        question.setExplanation(rs.getString("EXPLANATION"));
        question.setCategory(rs.getString("CATEGORY"));
        question.setLevel(rs.getString("LEVEL_NAME"));
        return question;
    };

    public JapaneseQuizQuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM JAPANESE_QUIZ_QUESTIONS", Long.class);
        return count == null ? 0 : count;
    }

    public List<JapaneseQuizQuestion> findAll() {
        return jdbcTemplate.query("SELECT * FROM JAPANESE_QUIZ_QUESTIONS ORDER BY ID ASC", rowMapper);
    }

    public Optional<JapaneseQuizQuestion> findById(Long id) {
        return jdbcTemplate.query("SELECT * FROM JAPANESE_QUIZ_QUESTIONS WHERE ID = ?", rowMapper, id).stream().findFirst();
    }

    public JapaneseQuizQuestion save(JapaneseQuizQuestion question) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO JAPANESE_QUIZ_QUESTIONS
                    (QUESTION_TYPE, PROMPT, OPTION_A, OPTION_B, OPTION_C, OPTION_D, CORRECT_OPTION, EXPLANATION, CATEGORY, LEVEL_NAME)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, question.getQuestionType());
            statement.setString(2, question.getPrompt());
            statement.setString(3, question.getOptionA());
            statement.setString(4, question.getOptionB());
            statement.setString(5, question.getOptionC());
            statement.setString(6, question.getOptionD());
            statement.setString(7, question.getCorrectOption());
            statement.setString(8, question.getExplanation());
            statement.setString(9, question.getCategory());
            statement.setString(10, question.getLevel());
            return statement;
        }, keyHolder);
        question.setId(keyHolder.getKey().longValue());
        return question;
    }
}
