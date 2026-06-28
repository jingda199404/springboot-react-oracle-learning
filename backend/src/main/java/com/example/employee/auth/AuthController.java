package com.example.employee.auth;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = service.register(request);
        return ResponseEntity.created(URI.create("/api/users/" + response.id())).body(response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return service.login(request);
    }

    @GetMapping("/session")
    public ResponseEntity<Void> session(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Session-Token") String sessionToken) {
        service.validateSession(userId, sessionToken);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Session-Token") String sessionToken) {
        service.logout(userId, sessionToken);
        return ResponseEntity.noContent().build();
    }
}
