package it.northleap.backend.config;

import it.northleap.backend.security.CurrentActorArgumentResolver;
import it.northleap.backend.security.RbacInterceptor;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    // per gestire richeiste http
    // tramite interceptor
    // inoltre con addArgumentsResolver semplifico
    // la logica del controller

    private final RbacInterceptor rbacInterceptor;
    private final CurrentActorArgumentResolver currentActorArgumentResolver;

    @Override
    public void addInterceptors(@NotNull InterceptorRegistry registry) {
        registry.addInterceptor(rbacInterceptor);
    }

    @Override
    public void addArgumentResolvers(@NotNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentActorArgumentResolver);
    }
}
