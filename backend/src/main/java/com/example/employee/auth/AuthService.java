package com.example.employee.auth;

import com.example.employee.error.AuthenticationException;
import com.example.employee.error.ConflictException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        String username = request.username().trim();
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("このユーザー名はすでに登録されています：" + username);
        }

        AppUser user = repository.save(new AppUser(username, passwordEncoder.encode(request.password())));
        return new AuthResponse(user.getId(), user.getUsername(), "登録が完了しました");
    }

    public AuthResponse login(AuthRequest request) {
        AppUser user = repository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new AuthenticationException("ユーザー名またはパスワードが正しくありません"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException("ユーザー名またはパスワードが正しくありません");
        }
        return new AuthResponse(user.getId(), user.getUsername(), "ログインしました");
    }
}
