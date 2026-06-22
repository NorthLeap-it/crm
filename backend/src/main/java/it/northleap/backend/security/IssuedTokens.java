package it.northleap.backend.security;

import it.northleap.backend.dtos.AuthResponse;

// Risultato interno di AuthService.issueTokens - i token grezzi servono solo al controller per
// costruire i cookie (AuthCookieService); il body HTTP pubblico (AuthResponse) non li contiene mai.
public record IssuedTokens(String accessToken, String refreshToken, AuthResponse body) {
}
