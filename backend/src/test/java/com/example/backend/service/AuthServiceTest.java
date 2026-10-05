package com.example.backend.service;

import com.example.backend.dto.request.AuthRequestDto;
import com.example.backend.dto.response.AuthResponseDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.mapper.AuthMapper;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.model.enums.Status;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.detail.UserDetailService;
import com.example.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManagerBuilder authenticationManagerBuilder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private CacheService cacheService;
    @Mock
    private AuthMapper authMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserDetailService userDetailService;
    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setUuid("uuid-active");
        activeUser.setUsername("testuser");
        activeUser.setPassword("encoded-password");
        activeUser.setFullName("Test User");
        activeUser.setEmail("test@example.com");
        activeUser.setStatus(Status.ACTIVE);

        inactiveUser = new User();
        inactiveUser.setId(2L);
        inactiveUser.setUuid("uuid-inactive");
        inactiveUser.setUsername("inactiveuser");
        inactiveUser.setStatus(Status.INACTIVE);
    }

    // ─── me() ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("me() - thành công: trả về Me DTO")
    void me_success_returnsMe() {
        AuthResponseDto.Me meDto = new AuthResponseDto.Me();
        meDto.setFullName("Test User");

        when(userRepository.findByUuid("uuid-active")).thenReturn(Optional.of(activeUser));
        when(authMapper.toMe(activeUser)).thenReturn(meDto);

        AuthResponseDto.Me result = authService.me("uuid-active");

        assertThat(result).isNotNull();
        assertThat(result.getFullName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("me() - không tìm thấy user → throw ACCOUNT_NOT_FOUND")
    void me_userNotFound_throwsException() {
        when(userRepository.findByUuid("missing-uuid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me("missing-uuid"))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    // ─── login() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login() - user INACTIVE → throw ACCOUNT_INACTIVE")
    void login_inactiveUser_throwsException() {
        AuthRequestDto.Login loginDto = new AuthRequestDto.Login();
        loginDto.setUsername("inactiveuser");
        loginDto.setPassword("pass");

        AuthenticationManager authManager = mock(AuthenticationManager.class);
        Authentication authentication = mock(Authentication.class);
        when(authenticationManagerBuilder.getObject()).thenReturn(authManager);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("inactiveuser")).thenReturn(Optional.of(inactiveUser));

        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThatThrownBy(() -> authService.login(loginDto, response))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
    }

    @Test
    @DisplayName("login() - thành công: trả về access token")
    void login_success_returnsToken() {
        AuthRequestDto.Login loginDto = new AuthRequestDto.Login();
        loginDto.setUsername("testuser");
        loginDto.setPassword("password");

        AuthenticationManager authManager = mock(AuthenticationManager.class);
        Authentication authentication = mock(Authentication.class);
        when(authenticationManagerBuilder.getObject()).thenReturn(authManager);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(jwtUtil.generateAccessToken(anyString(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyString(), any())).thenReturn("refresh-token");

        AuthResponseDto.info infoDto = new AuthResponseDto.info();
        infoDto.setAccessToken("access-token");
        when(authMapper.toInfoResponse(eq(activeUser), eq("access-token"))).thenReturn(infoDto);

        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthResponseDto.info result = authService.login(loginDto, response);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        verify(cacheService).saveRefreshToken(eq("uuid-active"), eq("refresh-token"));
    }

    @Test
    @DisplayName("login() - user không tồn tại → throw ACCOUNT_NOT_FOUND")
    void login_userNotFound_throwsException() {
        AuthRequestDto.Login loginDto = new AuthRequestDto.Login();
        loginDto.setUsername("ghost");
        loginDto.setPassword("pass");

        AuthenticationManager authManager = mock(AuthenticationManager.class);
        Authentication authentication = mock(Authentication.class);
        when(authenticationManagerBuilder.getObject()).thenReturn(authManager);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThatThrownBy(() -> authService.login(loginDto, response))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    // ─── register() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("register() - username/email đã tồn tại → throw CONFLICT_ACCOUNT")
    void register_usernameExists_throwsConflict() {
        AuthRequestDto.Register req = new AuthRequestDto.Register();
        req.setUsername("testuser");
        req.setPassword("pass123");
        req.setFullName("Test");
        req.setRoles(List.of("staff"));

        when(userRepository.existsByEmail("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_ACCOUNT);
    }

    @Test
    @DisplayName("register() - role là 'admin' → throw ROLE_NOT_FOUND")
    void register_adminRole_throwsRoleNotFound() {
        AuthRequestDto.Register req = new AuthRequestDto.Register();
        req.setUsername("newuser");
        req.setPassword("pass123");
        req.setFullName("New User");
        req.setRoles(List.of("admin"));

        when(userRepository.existsByEmail("newuser")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROLE_NOT_FOUND);
    }

    @Test
    @DisplayName("register() - role không tồn tại trong DB → throw ROLE_NOT_FOUND")
    void register_roleNotFound_throwsException() {
        AuthRequestDto.Register req = new AuthRequestDto.Register();
        req.setUsername("newuser");
        req.setPassword("pass123");
        req.setFullName("New User");
        req.setRoles(List.of("unknown_role"));

        when(userRepository.existsByEmail("newuser")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByName("unknown_role")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROLE_NOT_FOUND);
    }

    @Test
    @DisplayName("register() - thành công: lưu user, trả về Me DTO")
    void register_success_returnsMe() {
        AuthRequestDto.Register req = new AuthRequestDto.Register();
        req.setUsername("newuser");
        req.setPassword("pass123");
        req.setFullName("New User");
        req.setEmail("new@example.com");
        req.setPhone("0912345678");
        req.setRoles(List.of("staff"));

        Role staffRole = new Role(1L, "staff");
        AuthResponseDto.Me meDto = new AuthResponseDto.Me();
        meDto.setFullName("New User");

        when(userRepository.existsByEmail("newuser")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByName("staff")).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(activeUser);
        when(authMapper.toMe(any(User.class))).thenReturn(meDto);

        AuthResponseDto.Me result = authService.register(req);

        assertThat(result).isNotNull();
        assertThat(result.getFullName()).isEqualTo("New User");
        verify(userRepository).save(any(User.class));
    }

    // ─── refreshAccessToken() ─────────────────────────────────────────────────

    @Test
    @DisplayName("refreshAccessToken() - token type không phải refresh_token → throw UNAUTHORIZED_REFRESH_TOKEN")
    void refreshAccessToken_wrongType_throwsException() {
        Jwt jwt = mock(Jwt.class);
        when(jwtUtil.verifyToken("bad-token")).thenReturn(jwt);
        when(jwt.getClaim("type")).thenReturn("access_token");

        assertThatThrownBy(() -> authService.refreshAccessToken("bad-token"))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("refreshAccessToken() - jwtUtil ném exception → throw UNAUTHORIZED_REFRESH_TOKEN")
    void refreshAccessToken_invalidToken_throwsException() {
        when(jwtUtil.verifyToken("invalid-token")).thenThrow(new RuntimeException("parse error"));

        assertThatThrownBy(() -> authService.refreshAccessToken("invalid-token"))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("refreshAccessToken() - user INACTIVE → throw ACCOUNT_INACTIVE")
    void refreshAccessToken_inactiveUser_throwsException() {
        Jwt jwt = mock(Jwt.class);
        when(jwtUtil.verifyToken("refresh-tok")).thenReturn(jwt);
        when(jwt.getClaim("type")).thenReturn("refresh_token");
        when(jwt.getSubject()).thenReturn("uuid-inactive");
        when(userRepository.findByUuid("uuid-inactive")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authService.refreshAccessToken("refresh-tok"))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
    }

    @Test
    @DisplayName("refreshAccessToken() - thành công: trả về access token mới")
    void refreshAccessToken_success_returnsNewToken() {
        Jwt jwt = mock(Jwt.class);
        UserDetails userDetails = mock(UserDetails.class);
        when(jwtUtil.verifyToken("valid-refresh")).thenReturn(jwt);
        when(jwt.getClaim("type")).thenReturn("refresh_token");
        when(jwt.getSubject()).thenReturn("uuid-active");
        when(userRepository.findByUuid("uuid-active")).thenReturn(Optional.of(activeUser));
        when(userDetailService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(Set.of());
        when(jwtUtil.generateAccessToken(eq("uuid-active"), any(Authentication.class)))
                .thenReturn("new-access-token");

        AuthResponseDto.RefreshToken result = authService.refreshAccessToken("valid-refresh");

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
    }
}
