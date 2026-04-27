package com.n11.bootcamp.user_service.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public class UserPrincipalConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String userId = extractUserId(jwt);
        String email = jwt.getSubject();
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) roles = List.of();

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        UserPrincipal principal = new UserPrincipal(userId, email, roles);
        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }

    private String extractUserId(Jwt jwt) {
        Object id = jwt.getClaim("userId");
        if (id instanceof String s) return s;
        return null;
    }
}
