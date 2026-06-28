package com.example.employee.config;

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

    public EmployeeTableCleanup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS EMPLOYEES");
        log.info("Obsolete employee table cleanup finished.");
    }
}
