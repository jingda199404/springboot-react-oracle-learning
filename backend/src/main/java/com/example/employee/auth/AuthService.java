package com.example.employee.auth;

import com.example.employee.error.AuthenticationException;
import com.example.employee.error.ConflictException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    public static final String ACCOUNTING_PERMISSION = "ACCOUNTING";
    public static final String EMPLOYEE_PERMISSION = "EMPLOYEE";
    public static final String PERMISSION_SETTING_PERMISSION = "PERMISSION_SETTING";

    private final AppUserRepository repository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AppUserRepository repository, PermissionRepository permissionRepository) {
        this.repository = repository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        ensurePermissionMaster();
        String username = request.username().trim();
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("このユーザー名はすでに登録されています：" + username);
        }

        AppUser user = new AppUser(username, passwordEncoder.encode(request.password()), null);
        user.setPermissionEntities(permissionEntities(defaultPermissionCodes(username)));
        user = repository.save(user);
        return response(user, "登録が完了しました");
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        ensurePermissionMaster();
        AppUser user = repository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new AuthenticationException("ユーザー名またはパスワードが正しくありません"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException("ユーザー名またはパスワードが正しくありません");
        }
        ensureDefaultPermissions(user);
        return response(user, "ログインしました");
    }

    @Transactional
    public AppUser findUser(Long userId) {
        ensurePermissionMaster();
        AppUser user = repository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("ログイン情報を確認できません"));
        ensureDefaultPermissions(user);
        return user;
    }

    public void requirePermission(Long userId, String permission) {
        AppUser user = findUser(userId);
        if (!permissions(user).contains(permission)) {
            throw new AuthenticationException("このページを利用する権限がありません");
        }
    }

    private AuthResponse response(AppUser user, String message) {
        return new AuthResponse(user.getId(), user.getUsername(), permissions(user), message);
    }

    public List<String> permissions(AppUser user) {
        ensureDefaultPermissions(user);
        return user.getPermissionEntities().stream()
                .map(Permission::getCode)
                .distinct()
                .toList();
    }

    public List<PermissionDto> findAllPermissions() {
        ensurePermissionMaster();
        return permissionRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(permission -> new PermissionDto(permission.getCode(), permission.getLabel()))
                .toList();
    }

    public List<UserPermissionResponse> findAllUserPermissions() {
        ensurePermissionMaster();
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .peek(this::ensureDefaultPermissions)
                .map(user -> new UserPermissionResponse(user.getId(), user.getUsername(), permissions(user)))
                .toList();
    }

    @Transactional
    public UserPermissionResponse updateUserPermissions(Long currentUserId, Long targetUserId, List<String> codes) {
        requirePermission(currentUserId, PERMISSION_SETTING_PERMISSION);
        if (currentUserId.equals(targetUserId) && (codes == null || !codes.contains(PERMISSION_SETTING_PERMISSION))) {
            throw new IllegalArgumentException("自分自身から権限設定の権限は外せません");
        }
        AppUser target = findUser(targetUserId);
        target.setPermissionEntities(permissionEntities(codes == null ? List.of() : codes));
        target.setPermissions(null);
        return new UserPermissionResponse(target.getId(), target.getUsername(), permissions(target));
    }

    private List<String> defaultPermissionCodes(String username) {
        if ("admin".equalsIgnoreCase(username)) {
            return List.of(ACCOUNTING_PERMISSION, EMPLOYEE_PERMISSION, PERMISSION_SETTING_PERMISSION);
        }
        return List.of(ACCOUNTING_PERMISSION);
    }

    private void ensureDefaultPermissions(AppUser user) {
        List<String> legacyCodes = legacyPermissionCodes(user);
        if (!legacyCodes.isEmpty()) {
            user.getPermissionEntities().addAll(permissionEntities(legacyCodes));
            user.setPermissions(null);
        }
        if (!user.getPermissionEntities().isEmpty()) {
            return;
        }
        user.setPermissionEntities(permissionEntities(legacyCodes.isEmpty() ? defaultPermissionCodes(user.getUsername()) : legacyCodes));
        user.setPermissions(null);
    }

    private List<String> legacyPermissionCodes(AppUser user) {
        if (user.getPermissions() == null || user.getPermissions().isBlank()) {
            return List.of();
        }
        return Arrays.stream(user.getPermissions().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private Set<Permission> permissionEntities(List<String> codes) {
        ensurePermissionMaster();
        Set<Permission> result = new LinkedHashSet<>();
        for (String code : codes) {
            permissionRepository.findByCode(code)
                    .ifPresent(result::add);
        }
        return result;
    }

    private void ensurePermissionMaster() {
        createPermissionIfMissing(ACCOUNTING_PERMISSION, "家計簿");
        createPermissionIfMissing(EMPLOYEE_PERMISSION, "社員管理");
        createPermissionIfMissing(PERMISSION_SETTING_PERMISSION, "権限設定");
        ensureInitialPermissionAdmin();
    }

    private void createPermissionIfMissing(String code, String label) {
        if (!permissionRepository.existsByCode(code)) {
            permissionRepository.save(new Permission(code, label));
        }
    }

    private void ensureInitialPermissionAdmin() {
        List<AppUser> users = repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        if (users.isEmpty()) {
            return;
        }
        boolean existsAdmin = users.stream()
                .flatMap(user -> user.getPermissionEntities().stream())
                .anyMatch(permission -> PERMISSION_SETTING_PERMISSION.equals(permission.getCode()));
        if (existsAdmin) {
            return;
        }
        Permission permission = permissionRepository.findByCode(PERMISSION_SETTING_PERMISSION)
                .orElseThrow(() -> new IllegalStateException("権限マスタが見つかりません：" + PERMISSION_SETTING_PERMISSION));
        AppUser firstUser = users.get(0);
        permissionRepository.findByCode(ACCOUNTING_PERMISSION).ifPresent(firstUser.getPermissionEntities()::add);
        permissionRepository.findByCode(EMPLOYEE_PERMISSION).ifPresent(firstUser.getPermissionEntities()::add);
        firstUser.getPermissionEntities().add(permission);
    }
}
