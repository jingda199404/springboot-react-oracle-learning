package com.example.employee.japanese;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JapaneseKnowledgeArticleRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<JapaneseKnowledgeArticle> rowMapper = (rs, rowNum) -> {
        JapaneseKnowledgeArticle article = new JapaneseKnowledgeArticle();
        article.setId(rs.getLong("ID"));
        article.setTitleJa(rs.getString("TITLE_JA"));
        article.setTitleZh(rs.getString("TITLE_ZH"));
        article.setContentJa(rs.getString("CONTENT_JA"));
        article.setContentZh(rs.getString("CONTENT_ZH"));
        article.setTopic(rs.getString("TOPIC"));
        article.setReadingMinutes(rs.getInt("READING_MINUTES"));
        return article;
    };

    public JapaneseKnowledgeArticleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM JAPANESE_KNOWLEDGE_ARTICLES", Long.class);
        return count == null ? 0 : count;
    }

    public List<JapaneseKnowledgeArticle> findAll() {
        return jdbcTemplate.query("SELECT * FROM JAPANESE_KNOWLEDGE_ARTICLES ORDER BY ID ASC", rowMapper);
    }

    public JapaneseKnowledgeArticle save(JapaneseKnowledgeArticle article) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO JAPANESE_KNOWLEDGE_ARTICLES
                    (TITLE_JA, TITLE_ZH, CONTENT_JA, CONTENT_ZH, TOPIC, READING_MINUTES)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, article.getTitleJa());
            statement.setString(2, article.getTitleZh());
            statement.setString(3, article.getContentJa());
            statement.setString(4, article.getContentZh());
            statement.setString(5, article.getTopic());
            statement.setInt(6, article.getReadingMinutes());
            return statement;
        }, keyHolder);
        article.setId(keyHolder.getKey().longValue());
        return article;
    }
}
