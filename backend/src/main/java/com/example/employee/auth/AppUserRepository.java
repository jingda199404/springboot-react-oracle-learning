package com.example.employee.auth;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AppUserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionRepository permissionRepository;
    private final RowMapper<AppUser> rowMapper = (rs, rowNum) -> {
        AppUser user = new AppUser();
        user.setId(rs.getLong("ID"));
        user.setUsername(rs.getString("USERNAME"));
        user.setPasswordHash(rs.getString("PASSWORD_HASH"));
        user.setPermissions(rs.getString("PERMISSIONS"));
        user.setActiveSessionToken(rs.getString("ACTIVE_SESSION_TOKEN"));
        Timestamp lastActivityAt = rs.getTimestamp("SESSION_LAST_ACTIVITY_AT");
        user.setSessionLastActivityAt(lastActivityAt == null ? null : lastActivityAt.toInstant());
        return user;
    };

    public AppUserRepository(JdbcTemplate jdbcTemplate, PermissionRepository permissionRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionRepository = permissionRepository;
    }

    public Optional<AppUser> findById(Long id) {
        return jdbcTemplate.query("SELECT * FROM APP_USERS WHERE ID = ?", rowMapper, id).stream()
                .findFirst()
                .map(this::attachPermissions);
    }

    public Optional<AppUser> findByUsernameIgnoreCase(String username) {
        return jdbcTemplate.query("SELECT * FROM APP_USERS WHERE LOWER(USERNAME) = LOWER(?)", rowMapper, username).stream()
                .findFirst()
                .map(this::attachPermissions);
    }

    public boolean existsByUsernameIgnoreCase(String username) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM APP_USERS WHERE LOWER(USERNAME) = LOWER(?)", Integer.class, username);
        return count != null && count > 0;
    }

    public List<AppUser> findAll() {
        return jdbcTemplate.query("SELECT * FROM APP_USERS ORDER BY ID ASC", rowMapper).stream()
                .map(this::attachPermissions)
                .toList();
    }

    public AppUser save(AppUser user) {
        if (user.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO APP_USERS (USERNAME, PASSWORD_HASH, PERMISSIONS, ACTIVE_SESSION_TOKEN, SESSION_LAST_ACTIVITY_AT)
                        VALUES (?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                bindUser(statement, user);
                return statement;
            }, keyHolder);
            user.setId(keyHolder.getKey().longValue());
        } else {
            jdbcTemplate.update("""
                    UPDATE APP_USERS
                       SET USERNAME = ?, PASSWORD_HASH = ?, PERMISSIONS = ?, ACTIVE_SESSION_TOKEN = ?, SESSION_LAST_ACTIVITY_AT = ?
                     WHERE ID = ?
                    """, user.getUsername(), user.getPasswordHash(), user.getPermissions(), user.getActiveSessionToken(),
                    timestamp(user.getSessionLastActivityAt()), user.getId());
        }
        replacePermissions(user);
        return user;
    }

    public void delete(AppUser user) {
        jdbcTemplate.update("DELETE FROM USER_PERMISSIONS WHERE USER_ID = ?", user.getId());
        jdbcTemplate.update("DELETE FROM APP_USERS WHERE ID = ?", user.getId());
    }

    private AppUser attachPermissions(AppUser user) {
        List<Permission> permissions = jdbcTemplate.query("""
                SELECT P.*
                  FROM PERMISSIONS P
                  JOIN USER_PERMISSIONS UP ON UP.PERMISSION_ID = P.ID
                 WHERE UP.USER_ID = ?
                 ORDER BY P.ID ASC
                """, (rs, rowNum) -> {
            Permission permission = new Permission();
            permission.setId(rs.getLong("ID"));
            permission.setCode(rs.getString("CODE"));
            permission.setLabel(rs.getString("LABEL"));
            return permission;
        }, user.getId());
        user.setPermissionEntities(new LinkedHashSet<>(permissions));
        return user;
    }

    private void replacePermissions(AppUser user) {
        jdbcTemplate.update("DELETE FROM USER_PERMISSIONS WHERE USER_ID = ?", user.getId());
        Set<Permission> permissions = user.getPermissionEntities() == null ? Set.of() : user.getPermissionEntities();
        Set<Long> insertedPermissionIds = new LinkedHashSet<>();
        for (Permission permission : permissions) {
            Permission persisted = permission.getId() == null
                    ? permissionRepository.findByCode(permission.getCode()).orElse(permission)
                    : permission;
            if (persisted.getId() != null && insertedPermissionIds.add(persisted.getId())) {
                jdbcTemplate.update("INSERT INTO USER_PERMISSIONS (USER_ID, PERMISSION_ID) VALUES (?, ?)", user.getId(), persisted.getId());
            }
        }
    }

    private void bindUser(PreparedStatement statement, AppUser user) throws java.sql.SQLException {
        statement.setString(1, user.getUsername());
        statement.setString(2, user.getPasswordHash());
        statement.setString(3, user.getPermissions());
        statement.setString(4, user.getActiveSessionToken());
        statement.setTimestamp(5, timestamp(user.getSessionLastActivityAt()));
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
