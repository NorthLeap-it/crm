package it.northleap.backend.controllers;

import it.northleap.backend.dtos.*;
import it.northleap.backend.security.UserPrincipal;
import it.northleap.backend.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // dice se il workspace è già stato configurato
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> status() {
        return ResponseEntity.ok(authService.status());
    }

    // crea il primo workspace + utente owner, si blocca da solo dopo il primo uso
    @PostMapping("/onboarding")
    public ResponseEntity<AuthResponse> onboarding(@Valid @RequestBody OnboardingRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.onboarding(request, httpRequest));
    }

    // metodo login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    // rinnova access+refresh token, ruotando la sessione
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    // revoca la sessione associata al refresh token passato
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    // utente corrente con i suoi ruoli
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.me(principal));
    }
}
