package it.northleap.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Costruisce i cookie httpOnly access_token/refresh_token. Mai esposti in JSON (vedi
// AuthResponse) - solo qui, come Set-Cookie. secure/sameSite configurabili perche' in sviluppo
// locale (http://localhost) "Secure" andrebbe a bloccare il cookie del tutto; in produzione va
// messo a true (richiede HTTPS).
@Component
@RequiredArgsConstructor
public class AuthCookieService {

    public static final String ACCESS_COOKIE_NAME = "access_token";
    public static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final JwtService jwtService;

    @Value("${app.security.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookie-samesite:Lax}")
    private String cookieSameSite;

    public ResponseCookie accessTokenCookie(String token) {
        return build(ACCESS_COOKIE_NAME, token, "/api", Duration.ofMillis(jwtService.getExpirationAccessMs()));
    }

    public ResponseCookie refreshTokenCookie(String token) {
        return build(REFRESH_COOKIE_NAME, token, "/api/auth", Duration.ofMillis(jwtService.getExpirationRefreshMs()));
    }

    public ResponseCookie clearAccessTokenCookie() {
        return build(ACCESS_COOKIE_NAME, "", "/api", Duration.ZERO);
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return build(REFRESH_COOKIE_NAME, "", "/api/auth", Duration.ZERO);
    }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
