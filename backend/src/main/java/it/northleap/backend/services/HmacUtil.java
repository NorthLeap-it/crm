package it.northleap.backend.services;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// Util statica per HMAC-SHA256 esadecimale, usata in entrambe le direzioni: firma outbound
// (WorkflowActionExecutor.send_webhook) e verifica inbound (WebhookService.receive). Estratta
// da WorkflowActionExecutor (era privata lì) in Fase 6 quando è servita una seconda volta.
public final class HmacUtil {

    private HmacUtil() {
    }

    public static String sha256Hex(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}
