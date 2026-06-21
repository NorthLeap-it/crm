package it.northleap.backend.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Guardia anti-SSRF, porting letterale di isSafeUrl in workflow.engine.ts. Funzione di utilità
// pura testata a parte (05-WORKFLOW-ENGINE.md la vuole esplicitamente così, non un controllo
// informale dentro l'azione HTTP) — usata da WorkflowActionExecutor prima di QUALSIASI richiesta
// verso un URL fornito dalla configurazione di un workflow.
//
// Limite noto, ereditato dall'originale e non risolto qui: nessuna verifica sull'IP a cui un
// hostname *risolve* (no DNS resolution check) — un dominio pubblico che fa DNS rebinding verso
// un IP privato bypassa questo controllo, perché si guarda solo la stringa letterale nell'URL.
// Stessa lacuna dell'originale, riportata fedelmente.
public final class SafeUrlValidator {

    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    private SafeUrlValidator() {
    }

    public static boolean isSafe(String rawUrl) {
        URI u;
        try {
            u = new URI(rawUrl);
        } catch (URISyntaxException | NullPointerException e) {
            return false;
        }
        String scheme = u.getScheme();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return false;
        }
        if (u.getHost() == null || u.getHost().isEmpty()) {
            return false;
        }
        String host = u.getHost().toLowerCase();
        if (host.equals("localhost") || host.equals("0.0.0.0") || host.endsWith(".local") || host.endsWith(".internal")) {
            return false;
        }
        Matcher m = IPV4.matcher(host);
        if (m.matches()) {
            int a = Integer.parseInt(m.group(1));
            int b = Integer.parseInt(m.group(2));
            if (a == 10 || a == 127 || (a == 192 && b == 168) || (a == 172 && b >= 16 && b <= 31) || (a == 169 && b == 254)) {
                return false;
            }
        }
        return true;
    }
}
