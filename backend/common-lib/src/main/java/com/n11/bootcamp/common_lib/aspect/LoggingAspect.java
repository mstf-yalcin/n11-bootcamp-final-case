package com.n11.bootcamp.common_lib.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();

        log.info(">>> {} args={}", method, Arrays.toString(pjp.getArgs()));
        try {
            Object result = pjp.proceed();
            log.info("<<< {} [{}ms]", method, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable t) {
            log.error("!!! {} threw {} [{}ms]", method, t.getClass().getSimpleName(),
                    System.currentTimeMillis() - start);
            throw t;
        }
    }
}
