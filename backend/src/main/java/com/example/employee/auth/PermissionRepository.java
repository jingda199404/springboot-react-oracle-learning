package com.example.employee.auth;

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
public class PermissionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Permission> rowMapper = (rs, rowNum) -> {
        Permission permission = new Permission();
        permission.setId(rs.getLong("ID"));
        permission.setCode(rs.getString("CODE"));
        permission.setLabel(rs.getString("LABEL"));
        return permission;
    };

    public PermissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Permission> findAll() {
        return jdbcTemplate.query("SELECT * FROM PERMISSIONS ORDER BY ID ASC", rowMapper);
    }

    public Optional<Permission> findByCode(String code) {
        return jdbcTemplate.query("SELECT * FROM PERMISSIONS WHERE CODE = ?", rowMapper, code).stream().findFirst();
    }

    public boolean existsByCode(String code) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PERMISSIONS WHERE CODE = ?", Integer.class, code);
        return count != null && count > 0;
    }

    public Permission save(Permission permission) {
        if (permission.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO PERMISSIONS (CODE, LABEL) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                statement.setString(1, permission.getCode());
                statement.setString(2, permission.getLabel());
                return statement;
            }, keyHolder);
            permission.setId(keyHolder.getKey().longValue());
        } else {
            jdbcTemplate.update("UPDATE PERMISSIONS SET CODE = ?, LABEL = ? WHERE ID = ?",
                    permission.getCode(), permission.getLabel(), permission.getId());
        }
        return permission;
    }
}
