package com.example.employee.auth;

import com.example.employee.error.AuthenticationException;
import com.example.employee.error.ConflictException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public static final String ACCOUNTING_PERMISSION = "ACCOUNTING";
    public static final String PERMISSION_SETTING_PERMISSION = "PERMISSION_SETTING";
    public static final Duration SESSION_TIMEOUT = Duration.ofMinutes(30);

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
        log.info("User registered. userId={}, username={}", user.getId(), user.getUsername());
        return response(user, "登録が完了しました", null);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        ensurePermissionMaster();
        AppUser user = repository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new AuthenticationException("ユーザー名またはパスワードが正しくありません"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("User login failed. username={}", request.username().trim());
            throw new AuthenticationException("ユーザー名またはパスワードが正しくありません");
        }
        if (hasActiveSession(user, Instant.now())) {
            log.info("Existing session replaced by new login. userId={}, username={}", user.getId(), user.getUsername());
        }
        ensureDefaultPermissions(user);
        String sessionToken = createSession(user);
        log.info("User logged in. userId={}, username={}", user.getId(), user.getUsername());
        return response(user, "ログインしました", sessionToken);
    }

    @Transactional
    public void logout(Long userId, String sessionToken) {
        AppUser user = requireActiveSession(userId, sessionToken);
        clearSession(user);
        log.info("User logged out. userId={}, username={}", user.getId(), user.getUsername());
    }

    @Transactional(readOnly = true)
    public void validateSession(Long userId, String sessionToken) {
        requireActiveSession(userId, sessionToken, false);
    }

    @Transactional
    public AppUser findUser(Long userId) {
        ensurePermissionMaster();
        AppUser user = repository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("ログイン情報を確認できません"));
        ensureDefaultPermissions(user);
        return user;
    }

    public AppUser requireActiveSession(Long userId, String sessionToken) {
        return requireActiveSession(userId, sessionToken, true);
    }

    private AppUser requireActiveSession(Long userId, String sessionToken, boolean refreshActivity) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new AuthenticationException("ログインが必要です");
        }
        AppUser user = findUser(userId);
        Instant now = Instant.now();
        if (!sessionToken.equals(user.getActiveSessionToken())) {
            log.warn("Invalid session token. userId={}", userId);
            throw new AuthenticationException("他の端末でログインされたため、再ログインしてください");
        }
        if (isSessionExpired(user, now)) {
            clearSession(user);
            log.info("Session expired. userId={}, username={}", user.getId(), user.getUsername());
            throw new AuthenticationException("30分以上操作がなかったため、再ログインしてください");
        }
        if (refreshActivity) {
            user.setSessionLastActivityAt(now);
        }
        return user;
    }

    public void requirePermission(Long userId, String sessionToken, String permission) {
        AppUser user = requireActiveSession(userId, sessionToken);
        if (!permissions(user).contains(permission)) {
            throw new AuthenticationException("このページを利用する権限がありません");
        }
    }

    private AuthResponse response(AppUser user, String message, String sessionToken) {
        return new AuthResponse(user.getId(), user.getUsername(), permissions(user), message, sessionToken);
    }

    public List<String> permissions(AppUser user) {
        ensureDefaultPermissions(user);
        return user.getPermissionEntities().stream()
                .map(Permission::getCode)
                .filter(code -> !"EMPLOYEE".equals(code))
                .distinct()
                .toList();
    }

    public List<PermissionDto> findAllPermissions() {
        ensurePermissionMaster();
        return permissionRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(permission -> !"EMPLOYEE".equals(permission.getCode()))
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

    public List<UserPermissionResponse> findAllManagedUsers() {
        return findAllUserPermissions();
    }

    @Transactional
    public UserPermissionResponse createManagedUser(String username, String password, List<String> codes) {
        ensurePermissionMaster();
        String normalizedUsername = username == null ? "" : username.trim();
        validateCredentials(normalizedUsername, password);
        if (repository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ConflictException("このユーザー名はすでに登録されています：" + normalizedUsername);
        }

        List<String> permissionCodes = codes == null || codes.isEmpty()
                ? defaultPermissionCodes(normalizedUsername)
                : normalizePermissionCodes(codes);
        AppUser user = new AppUser(normalizedUsername, passwordEncoder.encode(password), null);
        user.setPermissionEntities(permissionEntitiesStrict(permissionCodes));
        user = repository.save(user);
        log.info("Managed user created. userId={}, username={}, permissions={}",
                user.getId(), user.getUsername(), permissionCodes);
        return new UserPermissionResponse(user.getId(), user.getUsername(), permissions(user));
    }

    @Transactional
    public UserPermissionResponse updateUserPassword(Long currentUserId, Long targetUserId, String password) {
        AppUser currentUser = findUser(currentUserId);
        if (!permissions(currentUser).contains(PERMISSION_SETTING_PERMISSION)) {
            throw new AuthenticationException("このページを利用する権限がありません");
        }
        if (password == null || password.length() < 6 || password.length() > 72) {
            throw new IllegalArgumentException("パスワードは 6 文字以上 72 文字以内で入力してください");
        }
        AppUser target = findUser(targetUserId);
        target.setPasswordHash(passwordEncoder.encode(password));
        if (!currentUserId.equals(targetUserId)) {
            clearSession(target);
        }
        log.info("User password updated. currentUserId={}, targetUserId={}", currentUserId, targetUserId);
        return new UserPermissionResponse(target.getId(), target.getUsername(), permissions(target));
    }

    @Transactional
    public UserPermissionResponse updateUserPermissions(Long currentUserId, Long targetUserId, List<String> codes) {
        AppUser currentUser = findUser(currentUserId);
        if (!permissions(currentUser).contains(PERMISSION_SETTING_PERMISSION)) {
            throw new AuthenticationException("このページを利用する権限がありません");
        }
        if (currentUserId.equals(targetUserId) && (codes == null || !codes.contains(PERMISSION_SETTING_PERMISSION))) {
            throw new IllegalArgumentException("自分自身から権限設定の権限は外せません");
        }
        AppUser target = findUser(targetUserId);
        target.setPermissionEntities(permissionEntities(codes == null ? List.of() : codes));
        target.setPermissions(null);
        log.info("User permissions updated. currentUserId={}, targetUserId={}, permissions={}",
                currentUserId, targetUserId, codes == null ? List.of() : codes);
        return new UserPermissionResponse(target.getId(), target.getUsername(), permissions(target));
    }

    private String createSession(AppUser user) {
        String sessionToken = UUID.randomUUID().toString().replace("-", "");
        user.setActiveSessionToken(sessionToken);
        user.setSessionLastActivityAt(Instant.now());
        return sessionToken;
    }

    private boolean hasActiveSession(AppUser user, Instant now) {
        if (user.getActiveSessionToken() == null || user.getActiveSessionToken().isBlank()) {
            return false;
        }
        if (isSessionExpired(user, now)) {
            clearSession(user);
            return false;
        }
        return true;
    }

    private boolean isSessionExpired(AppUser user, Instant now) {
        Instant lastActivityAt = user.getSessionLastActivityAt();
        return lastActivityAt == null || lastActivityAt.plus(SESSION_TIMEOUT).isBefore(now);
    }

    private void clearSession(AppUser user) {
        user.setActiveSessionToken(null);
        user.setSessionLastActivityAt(null);
    }

    private List<String> defaultPermissionCodes(String username) {
        if ("admin".equalsIgnoreCase(username)) {
            return List.of(ACCOUNTING_PERMISSION, PERMISSION_SETTING_PERMISSION);
        }
        return List.of(ACCOUNTING_PERMISSION);
    }

    private void validateCredentials(String username, String password) {
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("ユーザー名は 3 文字以上 50 文字以内で入力してください");
        }
        if (password == null || password.length() < 6 || password.length() > 72) {
            throw new IllegalArgumentException("パスワードは 6 文字以上 72 文字以内で入力してください");
        }
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

    private Set<Permission> permissionEntitiesStrict(List<String> codes) {
        ensurePermissionMaster();
        Set<Permission> result = new LinkedHashSet<>();
        for (String code : normalizePermissionCodes(codes)) {
            Permission permission = permissionRepository.findByCode(code)
                    .orElseThrow(() -> new IllegalArgumentException("存在しない権限コードです：" + code));
            result.add(permission);
        }
        return result;
    }

    private List<String> normalizePermissionCodes(List<String> codes) {
        return codes.stream()
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .filter(code -> !"EMPLOYEE".equals(code))
                .distinct()
                .toList();
    }

    private void ensurePermissionMaster() {
        createPermissionIfMissing(ACCOUNTING_PERMISSION, "家計簿");
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
        firstUser.getPermissionEntities().add(permission);
    }
}
