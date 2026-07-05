package com.example.employee.auth;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

public class AppUser {

    private Long id;

    private String username;

    private String passwordHash;

    private String legacyPermissions;

    private String activeSessionToken;

    private Instant sessionLastActivityAt;

    private Set<Permission> permissions = new LinkedHashSet<>();

    public AppUser() {
    }

    public AppUser(String username, String passwordHash, String legacyPermissions) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.legacyPermissions = legacyPermissions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPermissions() {
        return legacyPermissions;
    }

    public void setPermissions(String permissions) {
        this.legacyPermissions = permissions;
    }

    public String getActiveSessionToken() {
        return activeSessionToken;
    }

    public void setActiveSessionToken(String activeSessionToken) {
        this.activeSessionToken = activeSessionToken;
    }

    public Instant getSessionLastActivityAt() {
        return sessionLastActivityAt;
    }

    public void setSessionLastActivityAt(Instant sessionLastActivityAt) {
        this.sessionLastActivityAt = sessionLastActivityAt;
    }

    public Set<Permission> getPermissionEntities() {
        return permissions;
    }

    public void setPermissionEntities(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
