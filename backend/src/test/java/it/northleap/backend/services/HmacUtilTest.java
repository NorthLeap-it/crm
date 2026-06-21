package it.northleap.backend.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HmacUtilTest {

    @Test
    void sha256HexMatchesKnownVector() {
        // vettore noto HMAC-SHA256(key="key", data="The quick brown fox jumps over the lazy dog")
        String hex = HmacUtil.sha256Hex("key", "The quick brown fox jumps over the lazy dog");
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8", hex);
    }

    @Test
    void differentSecretsProduceDifferentSignatures() {
        String a = HmacUtil.sha256Hex("secret-a", "payload");
        String b = HmacUtil.sha256Hex("secret-b", "payload");
        org.junit.jupiter.api.Assertions.assertNotEquals(a, b);
    }
}
