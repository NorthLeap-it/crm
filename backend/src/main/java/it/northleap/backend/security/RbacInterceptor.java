package it.northleap.backend.security;

import it.northleap.backend.exceptions.RbacDeniedException;
import it.northleap.backend.services.RbacService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

// Equivalente Spring del RbacGuard NestJS: legge @RequirePerm sul metodo, risolve l'Actor
// (popolato da JwtAuthenticationFilter come request attribute) e chiama RbacService.resolve
@Component
@RequiredArgsConstructor
public class RbacInterceptor implements HandlerInterceptor {

    private static final String RESOLVED_SCOPE_ATTRIBUTE = "rbac.scope";

    private final RbacService rbacService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePerm requirePerm = handlerMethod.getMethodAnnotation(RequirePerm.class);
        if (requirePerm == null) {
            return true;
        }

        Actor actor = (Actor) request.getAttribute(Actor.REQUEST_ATTRIBUTE);
        if (actor == null) {
            throw new RbacDeniedException();
        }

        RbacService.Resolution resolution = rbacService.resolve(actor.roleIds(), requirePerm.resource(), requirePerm.action());
        if (!resolution.allowed()) {
            throw new RbacDeniedException();
        }

        // lo scope risolto viene esposto per filtri record-level (OWN) usati a valle, Fase 3
        request.setAttribute(RESOLVED_SCOPE_ATTRIBUTE, resolution.scope());
        return true;
    }
}
