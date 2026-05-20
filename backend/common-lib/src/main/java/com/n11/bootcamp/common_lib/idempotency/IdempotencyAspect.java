package com.n11.bootcamp.common_lib.idempotency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.n11.bootcamp.common_lib.auth.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.regex.Pattern;

@Aspect
@Slf4j
public class IdempotencyAspect {

    private static final String IN_PROGRESS = "__IN_PROGRESS__";
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]{8,128}$");

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final String serviceName;

    public IdempotencyAspect(StringRedisTemplate redis, ObjectMapper objectMapper, String serviceName) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
    }

    @Around("@annotation(idempotent)")
    public Object handle(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return pjp.proceed();
        }

        String key = request.getHeader(idempotent.keyHeader());
        if (key == null || key.isBlank()) {
            if (idempotent.required()) {
                throw new IdempotencyConflictException();
            }
            return pjp.proceed();
        }
        if (!KEY_PATTERN.matcher(key).matches()) {
            log.warn("Rejecting malformed Idempotency-Key header");
            return pjp.proceed();
        }

        String userId = currentUserId();
        String redisKey = "idem:" + serviceName + ":" + userId + ":" + key;
        Duration ttl = Duration.ofSeconds(idempotent.ttlSeconds());

        Boolean firstTime = redis.opsForValue().setIfAbsent(redisKey, IN_PROGRESS, ttl);
        if (Boolean.FALSE.equals(firstTime)) {
            return returnCached(redisKey);
        }

        try {
            Object result = pjp.proceed();
            cacheResult(redisKey, result, ttl);
            return result;
        } catch (Throwable t) {
            redis.delete(redisKey);
            throw t;
        }
    }

    private Object returnCached(String redisKey) {
        String value = redis.opsForValue().get(redisKey);
        if (value == null || IN_PROGRESS.equals(value)) {
            throw new IdempotencyConflictException();
        }
        try {
            JsonNode root = objectMapper.readTree(value);
            int status = root.path("status").asInt(HttpStatus.OK.value());
            JsonNode body = root.path("body");
            return ResponseEntity.status(status).body(body);
        } catch (Exception e) {
            log.warn("Failed to deserialize idempotency cache for key={}, falling back to conflict", redisKey, e);
            throw new IdempotencyConflictException();
        }
    }

    private void cacheResult(String redisKey, Object result, Duration ttl) {
        if (!(result instanceof ResponseEntity<?> response)) {
            redis.delete(redisKey);
            return;
        }
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("status", response.getStatusCode().value());
            root.set("body", objectMapper.valueToTree(response.getBody()));
            redis.opsForValue().set(redisKey, objectMapper.writeValueAsString(root), ttl);
        } catch (Exception e) {
            log.warn("Failed to cache idempotent response for key={}", redisKey, e);
            redis.delete(redisKey);
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up.id();
        }
        return "anonymous";
    }
}
