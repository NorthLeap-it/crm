package it.northleap.backend.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// Util statica per l'hashing SHA-256 esadecimale di chiavi/token in chiaro (ApiKey, Invite),
// stessa logica di JwtService.hashToken ma factorizzata qui perché JwtService resta
// scope-limitato al dominio JWT access/refresh, non diventa una utility generica.
public final class HashUtil {

    private HashUtil() {
    }

    public static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
