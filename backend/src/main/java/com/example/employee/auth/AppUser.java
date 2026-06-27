package com.example.employee.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "APP_USERS")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USERNAME", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "PERMISSIONS", length = 200)
    private String legacyPermissions;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "USER_PERMISSIONS",
            joinColumns = @JoinColumn(name = "USER_ID"),
            inverseJoinColumns = @JoinColumn(name = "PERMISSION_ID")
    )
    private Set<Permission> permissions = new LinkedHashSet<>();

    protected AppUser() {
    }

    public AppUser(String username, String passwordHash, String legacyPermissions) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.legacyPermissions = legacyPermissions;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPermissions() {
        return legacyPermissions;
    }

    public void setPermissions(String permissions) {
        this.legacyPermissions = permissions;
    }

    public Set<Permission> getPermissionEntities() {
        return permissions;
    }

    public void setPermissionEntities(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
