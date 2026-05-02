package com.n11.bootcamp.user_service.service;

import com.n11.bootcamp.common_lib.auth.enums.Role;
import com.n11.bootcamp.user_service.dto.internal.RefreshTokenDto;
import com.n11.bootcamp.user_service.dto.request.AuthRequest;
import com.n11.bootcamp.user_service.dto.request.RefreshTokenRequest;
import com.n11.bootcamp.user_service.dto.request.RegisterRequest;
import com.n11.bootcamp.user_service.dto.response.TokenResponse;
import com.n11.bootcamp.user_service.dto.response.UserResponse;
import com.n11.bootcamp.user_service.entity.RoleEntity;
import com.n11.bootcamp.user_service.entity.User;
import com.n11.bootcamp.user_service.exception.EmailAlreadyExistsException;
import com.n11.bootcamp.user_service.exception.InvalidTokenException;
import com.n11.bootcamp.user_service.exception.PhoneAlreadyExistsException;
import com.n11.bootcamp.user_service.exception.UserNotFoundException;
import com.n11.bootcamp.user_service.repository.RoleRepository;
import com.n11.bootcamp.user_service.repository.UserRepository;
import com.n11.bootcamp.user_service.util.PhoneNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class AuthService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AuthService(JwtService jwtService, RefreshTokenService refreshTokenService,
            AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder,
            UserRepository userRepository, RoleRepository roleRepository) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public TokenResponse register(RegisterRequest authRequest) {

        if (userRepository.existsByEmail(authRequest.email())) {
            throw new EmailAlreadyExistsException();
        }

        String normalizedPhone = PhoneNormalizer.ensureCountryCode(authRequest.phone());
        if (normalizedPhone != null && userRepository.existsByPhone(normalizedPhone)) {
            throw new PhoneAlreadyExistsException();
        }

        User user = new User();
        user.setEmail(authRequest.email());
        user.setFirstName(authRequest.firstName());
        user.setLastName(authRequest.lastName());
        user.setPhone(normalizedPhone);
        user.setPassword(passwordEncoder.encode(authRequest.password()));

        RoleEntity userRole = roleRepository.findByAuthority(Role.USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        user.setRoles(List.of(userRole));
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user.getId(), authRequest.email(), List.of(Role.USER.name()));
        String refreshToken = refreshTokenService.generateRefreshToken(user);

        log.info("User registered successfully: {}", authRequest.email());
        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse login(AuthRequest authRequest) {
        Authentication auth = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(authRequest.email(), authRequest.password()));

        User user = (User) auth.getPrincipal();
        List<String> roles = extractRoles(user);
        String accessToken = jwtService.generateToken(user.getId(), authRequest.email(), roles);
        String refreshToken = refreshTokenService.generateRefreshToken(user);

        log.info("User logged in: {}", authRequest.email());
        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        RefreshTokenDto result = refreshTokenService.refreshTokenLogin(refreshTokenRequest);
        User user = result.user();
        String accessToken = jwtService.generateToken(user.getId(), user.getUsername(), extractRoles(user));
        log.info("Token refreshed for user: {}", user.getUsername());
        return new TokenResponse(accessToken, result.rawToken());
    }

    @Transactional
    public void logout(RefreshTokenRequest request, String authHeader) {
        log.info("Logging out user...");
        refreshTokenService.revokeToken(request.refreshToken());
        log.info("Logout successful.");
    }

    public Boolean validate(String token) {
        if (jwtService.validateToken(token)) {
            return true;
        }
        throw new InvalidTokenException("Invalid token");
    }

    public UserResponse getUserInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRoles().stream().map(RoleEntity::getRole).map(Role::name).toList());
    }

    private List<String> extractRoles(User user) {
        return user.getRoles().stream()
                .map(RoleEntity::getRole)
                .map(Role::name)
                .toList();
    }

}
