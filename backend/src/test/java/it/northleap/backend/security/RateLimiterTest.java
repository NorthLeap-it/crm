package it.northleap.backend.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void allowsUpToMaxAttemptsThenBlocks() {
        RateLimiter limiter = new RateLimiter(3, 60);
        Instant t0 = Instant.now();

        assertTrue(limiter.allow("k", t0));
        assertTrue(limiter.allow("k", t0));
        assertTrue(limiter.allow("k", t0));
        assertFalse(limiter.allow("k", t0), "il quarto tentativo nella stessa finestra deve essere bloccato");
    }

    @Test
    void windowResetsAfterItExpires() {
        RateLimiter limiter = new RateLimiter(1, 60);
        Instant t0 = Instant.now();

        assertTrue(limiter.allow("k", t0));
        assertFalse(limiter.allow("k", t0.plusSeconds(30)), "ancora dentro la stessa finestra");
        assertTrue(limiter.allow("k", t0.plusSeconds(61)), "la finestra successiva deve ripartire da zero");
    }

    @Test
    void differentKeysHaveIndependentBuckets() {
        RateLimiter limiter = new RateLimiter(1, 60);
        Instant t0 = Instant.now();

        assertTrue(limiter.allow("ip-a", t0));
        assertTrue(limiter.allow("ip-b", t0), "una chiave diversa (es. IP diverso) non deve essere influenzata");
        assertFalse(limiter.allow("ip-a", t0));
    }
}
