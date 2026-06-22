package it.northleap.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Rate limiter a finestra fissa, in-memory, per IP+endpoint - protegge gli endpoint pubblici più
// esposti a brute-force (login, refresh, accept-invite) da tentativi ripetuti. Nessun limite
// applicativo esisteva prima (gap segnalato nella security review).
//
// Limite consapevole e dichiarato: lo stato vive in memoria del processo, quindi non protegge
// se l'app gira su più istanze dietro un load balancer (in quel caso ogni istanza ha il proprio
// contatore) - non sostituisce un rate limiting centralizzato a livello di gateway/WAF in
// produzione multi-istanza, ma è comunque una base di protezione reale per il caso comune
// (singola istanza, o anche multi-istanza con sticky session sull'IP).
@Component
public class RateLimiter {

    private final int maxAttempts;
    private final Duration window;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static final class Bucket {
        final AtomicInteger count = new AtomicInteger(0);
        volatile Instant windowStart;

        Bucket(Instant now) {
            this.windowStart = now;
        }
    }

    public RateLimiter(
            @Value("${app.rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public boolean allow(String key) {
        return allow(key, Instant.now());
    }

    // overload a tempo esplicito, stesso seam di testabilità già usato altrove nel progetto
    // (AnalyticsService.referenceDate, WorkflowEngine.runWorkflow) per evitare di dover
    // mockare/aspettare il tempo reale nei test
    boolean allow(String key, Instant now) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(now));
        synchronized (bucket) {
            if (Duration.between(bucket.windowStart, now).compareTo(window) >= 0) {
                bucket.windowStart = now;
                bucket.count.set(0);
            }
            return bucket.count.incrementAndGet() <= maxAttempts;
        }
    }
}
