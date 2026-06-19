package it.northleap.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import it.northleap.backend.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Function;


@Service
public class JwtService {

    @Value("${app.jwt.jwt_access_secret}")
    private String jwt_access_secret;

    @Value("${app.jwt.jwt_refresh_secret}")
    private String jwt_refresh_secret;

    @Value("${app.jwt.expiration-access-ms}")
    private long expirationAccessMs;

    @Value("${app.jwt.expiration-refresh-ms}")
    private long expirationRefreshMs;

    // genero token criptato hs256, contenente username, data, expiration
    public String generateAccessToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationAccessMs))
                .signWith(getAccessSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // per il token di refresh
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationRefreshMs))
                .signWith(getRefreshSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // hash del refresh token, è quello che salviamo in Session/Invite, mai il token in chiaro
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // metodo per estrarre l'email partendo dal token
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject, getAccessSigningKey());
    }
    public String extractUserIdFromRefresh(String token) {
        return extractClaim(token, Claims::getSubject, getRefreshSigningKey());
    }

    // scadenza effettiva del refresh token, usata per Session.expiresAt
    public Date extractRefreshExpiration(String token) {
        return extractClaim(token, Claims::getExpiration, getRefreshSigningKey());
    }

    // verifica che il metodo sia valido, identità e la parte di scadenza
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // controlla se il metodo è ancora valido o scaduto
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration, getAccessSigningKey()).before(new Date());
    }

    // metodo di supporto
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver, Key key) {
        final Claims claims = extractAllClaims(token, key);
        return claimsResolver.apply(claims);
    }

    // supporto
    private Claims extractAllClaims(String token, Key key) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // chiave
    private SecretKey getAccessSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwt_access_secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // chiave
    private SecretKey getRefreshSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwt_refresh_secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
