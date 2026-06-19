package it.northleap.backend.security;

import it.northleap.backend.entities.User;
import it.northleap.backend.repositories.UserRoleRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRoleRepository userRoleRepository;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {

        // parte header auth + token
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        final String email;

        try {
            email = jwtService.extractEmail(token);
        } catch (Exception e) {
            // Token malformato, scaduto o firma non valida: lasciamo la richiesta
            // non autenticata, sarà Spring Security a rispondere 401/403 più avanti
            filterChain.doFilter(request, response);
            return;
        }

        // l'user è effettivamente autenticato
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtService.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // Actor RBAC, separato da UserDetails: usato da RbacInterceptor/@CurrentActor
                if (userDetails instanceof UserPrincipal userPrincipal) {
                    User user = userPrincipal.getUser();
                    List<UUID> roleIds = userRoleRepository.findByUser_Id(user.getId()).stream()
                            .map(userRole -> userRole.getRole().getId())
                            .toList();
                    request.setAttribute(Actor.REQUEST_ATTRIBUTE,
                            new Actor(user.getId(), ActorType.USER, user.getEmail(), roleIds));
                }
            }
        }

        filterChain.doFilter(request, response);
    }

}
