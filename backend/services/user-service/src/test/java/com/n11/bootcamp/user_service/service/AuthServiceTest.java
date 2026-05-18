package com.n11.bootcamp.user_service.service;

import com.n11.bootcamp.common_lib.auth.enums.Role;
import com.n11.bootcamp.user_service.dto.internal.RefreshTokenDto;
import com.n11.bootcamp.user_service.dto.request.AuthRequest;
import com.n11.bootcamp.user_service.dto.request.RefreshTokenRequest;
import com.n11.bootcamp.user_service.dto.request.RegisterRequest;
import com.n11.bootcamp.user_service.dto.response.TokenResponse;
import com.n11.bootcamp.user_service.entity.RoleEntity;
import com.n11.bootcamp.user_service.entity.User;
import com.n11.bootcamp.user_service.exception.EmailAlreadyExistsException;
import com.n11.bootcamp.user_service.repository.RoleRepository;
import com.n11.bootcamp.user_service.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService _authService;

    @Mock
    private JwtService _jwtService;

    @Mock
    private RefreshTokenService _refreshTokenService;

    @Mock
    private AuthenticationManager _authenticationManager;

    @Mock
    private PasswordEncoder _passwordEncoder;

    @Mock
    private UserRepository _userRepository;

    @Mock
    private RoleRepository _roleRepository;

    private User createTestUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        RoleEntity userRole = new RoleEntity();
        userRole.setAuthority(Role.USER);
        user.setRoles(List.of(userRole));
        return user;
    }

    @Test
    void testRegister_ShouldSaveUserAndReturnTokens_WhenEmailIsUnique() {
        // given
        RegisterRequest request = new RegisterRequest(
                "newuser@test.com",
                "password123",
                "Yeni",
                "Kullanici",
                "5551234567"
        );
        RoleEntity userRole = new RoleEntity();
        userRole.setAuthority(Role.USER);

        Mockito.when(_userRepository.existsByEmail("newuser@test.com")).thenReturn(false);
        Mockito.when(_userRepository.existsByPhone(Mockito.anyString())).thenReturn(false);
        Mockito.when(_passwordEncoder.encode("password123")).thenReturn("hashed-password");
        Mockito.when(_roleRepository.findByAuthority(Role.USER)).thenReturn(Optional.of(userRole));
        Mockito.when(_userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            if (u.getId() == null) u.setId(UUID.randomUUID());
            return u;
        });
        Mockito.when(_jwtService.generateToken(Mockito.any(UUID.class), Mockito.eq("newuser@test.com"), Mockito.anyList()))
                .thenReturn("access-token");
        Mockito.when(_refreshTokenService.generateRefreshToken(Mockito.any(User.class)))
                .thenReturn("raw-refresh-token");

        // when
        TokenResponse response = _authService.register(request);

        // then
        Assertions.assertNotNull(response);
        Assertions.assertEquals("access-token", response.accessToken());
        Assertions.assertEquals("raw-refresh-token", response.refreshToken());
        Mockito.verify(_userRepository).save(Mockito.any(User.class));
    }

    @Test
    void testRegister_ShouldThrowException_WhenEmailAlreadyExists() {
        // given
        RegisterRequest request = new RegisterRequest(
                "duplicate@test.com",
                "password123",
                "Test",
                "User",
                "5551234567"
        );
        Mockito.when(_userRepository.existsByEmail("duplicate@test.com")).thenReturn(true);

        // when & then
        Assertions.assertThrows(EmailAlreadyExistsException.class, () -> _authService.register(request));
        Mockito.verify(_userRepository, Mockito.never()).save(Mockito.any(User.class));
    }

    @Test
    void testLogin_ShouldReturnTokenResponse_WhenCredentialsAreValid() {
        // given
        AuthRequest request = new AuthRequest("testuser@test.com", "password123");
        User user = createTestUser("testuser@test.com");
        Authentication authentication = Mockito.mock(Authentication.class);

        Mockito.when(_authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        Mockito.when(authentication.getPrincipal()).thenReturn(user);
        Mockito.when(_jwtService.generateToken(Mockito.eq(user.getId()), Mockito.eq("testuser@test.com"), Mockito.anyList()))
                .thenReturn("access-token");
        Mockito.when(_refreshTokenService.generateRefreshToken(user)).thenReturn("raw-refresh-token");

        // when
        TokenResponse response = _authService.login(request);

        // then
        Assertions.assertNotNull(response);
        Assertions.assertEquals("access-token", response.accessToken());
        Assertions.assertEquals("raw-refresh-token", response.refreshToken());
    }

    @Test
    void testRefreshToken_ShouldReturnNewTokenResponse_WhenTokenIsValid() {
        // given
        String oldRawToken = "old-raw-token";
        RefreshTokenRequest request = new RefreshTokenRequest(oldRawToken);
        User user = createTestUser("testuser@test.com");
        RefreshTokenDto dto = new RefreshTokenDto("new-raw-token", user);

        Mockito.when(_refreshTokenService.refreshTokenLogin(request)).thenReturn(dto);
        Mockito.when(_jwtService.generateToken(Mockito.eq(user.getId()), Mockito.eq("testuser@test.com"), Mockito.anyList()))
                .thenReturn("new-access-token");

        // when
        TokenResponse response = _authService.refreshToken(request);

        // then
        Assertions.assertNotNull(response);
        Assertions.assertEquals("new-access-token", response.accessToken());
        Assertions.assertEquals("new-raw-token", response.refreshToken());
    }

    @Test
    void testLogout_ShouldRevokeRefreshToken() {
        // given
        String refreshTokenStr = "refresh-123";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenStr);
        String authHeader = "Bearer access-123";

        // when
        _authService.logout(request, authHeader);

        // then
        Mockito.verify(_refreshTokenService).revokeToken(refreshTokenStr);
    }
}
