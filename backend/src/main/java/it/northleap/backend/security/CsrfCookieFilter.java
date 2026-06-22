package it.northleap.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Pattern ufficiale Spring Security per SPA (vedi reference docs, sezione "CSRF: Integrating
// with Single Page Applications"). CsrfFilter calcola il CsrfToken in modo "lazy" - viene
// effettivamente generato/scritto come cookie XSRF-TOKEN solo quando qualcosa chiama
// CsrfToken.getToken(). Senza questo filtro il cookie non verrebbe mai emesso finche' nessun
// endpoint legge esplicitamente il token, lasciando il frontend senza nulla da rimandare
// indietro come header X-XSRF-TOKEN sulla prima richiesta mutante utile (es. login).
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
