package com.n11.bootcamp.common_lib.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    String keyHeader() default "Idempotency-Key";

    long ttlSeconds() default 86400;

    boolean required() default false;
}
