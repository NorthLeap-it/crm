package it.northleap.backend.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// equivalente Spring del @RequirePerm + RbacGuard NestJS: intercettato da RbacInterceptor
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePerm {
    String resource();
    PermAction action();
}
