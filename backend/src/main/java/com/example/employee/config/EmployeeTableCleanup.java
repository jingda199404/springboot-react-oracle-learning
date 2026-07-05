package com.example.employee.config;

import com.example.employee.auth.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmployeeTableCleanup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmployeeTableCleanup.class);

    private final JdbcTemplate jdbcTemplate;
    private final AuthService authService;

    public EmployeeTableCleanup(JdbcTemplate jdbcTemplate, AuthService authService) {
        this.jdbcTemplate = jdbcTemplate;
        this.authService = authService;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS EMPLOYEES");
        migrateJapaneseArticleColumns();
        authService.initializePermissionMaster();
        log.info("Obsolete employee table cleanup finished.");
    }

    private void migrateJapaneseArticleColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE JAPANESE_KNOWLEDGE_ARTICLES MODIFY CONTENT_JA LONGTEXT NOT NULL");
            jdbcTemplate.execute("ALTER TABLE JAPANESE_KNOWLEDGE_ARTICLES MODIFY CONTENT_ZH LONGTEXT NOT NULL");
            log.info("Japanese article content columns migrated to LONGTEXT.");
        } catch (RuntimeException exception) {
            log.debug("Japanese article migration skipped. message={}", exception.getMessage());
        }
    }
}
