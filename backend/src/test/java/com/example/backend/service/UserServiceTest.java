package com.example.backend.service;

import com.example.backend.dto.request.UserRoleUpdateRequestDto;
import com.example.backend.dto.request.UserUpdateRequestDto;
import com.example.backend.dto.response.UserResponseDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.model.enums.Status;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUuid("user-uuid-001");
        existingUser.setUsername("johndoe");
        existingUser.setFullName("John Doe");
        existingUser.setEmail("john@example.com");
        existingUser.setPhone("0901234567");
        existingUser.setStatus(Status.ACTIVE);
    }

    // ─── updateUser() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUser() - user không tồn tại → throw ACCOUNT_NOT_FOUND")
    void updateUser_notFound_throwsException() {
        when(userRepository.findByUuid("no-uuid")).thenReturn(Optional.empty());
        UserUpdateRequestDto req = new UserUpdateRequestDto();

        assertThatThrownBy(() -> userService.updateUser("no-uuid", req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("updateUser() - email mới đã tồn tại → throw CONFLICT_USER_EMAIL")
    void updateUser_emailConflict_throwsException() {
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        UserUpdateRequestDto req = new UserUpdateRequestDto();
        req.setEmail("taken@example.com");  // khác với email hiện tại

        assertThatThrownBy(() -> userService.updateUser("user-uuid-001", req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_USER_EMAIL);
    }

    @Test
    @DisplayName("updateUser() - phone mới đã tồn tại → throw CONFLICT_USER_PHONE")
    void updateUser_phoneConflict_throwsException() {
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByPhone("0999999999")).thenReturn(true);

        UserUpdateRequestDto req = new UserUpdateRequestDto();
        req.setPhone("0999999999");  // khác với phone hiện tại

        assertThatThrownBy(() -> userService.updateUser("user-uuid-001", req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_USER_PHONE);
    }

    @Test
    @DisplayName("updateUser() - thành công: cập nhật fullName, password, status")
    void updateUser_success_updatesFields() {
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-newpass");

        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setFullName("John Updated");
        when(userMapper.toResponse(existingUser)).thenReturn(responseDto);

        UserUpdateRequestDto req = new UserUpdateRequestDto();
        req.setFullName("John Updated");
        req.setPassword("newpass");
        req.setStatus(Status.INACTIVE);

        UserResponseDto result = userService.updateUser("user-uuid-001", req);

        assertThat(result.getFullName()).isEqualTo("John Updated");
        assertThat(existingUser.getStatus()).isEqualTo(Status.INACTIVE);
        verify(passwordEncoder).encode("newpass");
    }

    @Test
    @DisplayName("updateUser() - email giống cũ: không kiểm tra conflict")
    void updateUser_sameEmail_doesNotCheckConflict() {
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(existingUser));
        UserResponseDto responseDto = new UserResponseDto();
        when(userMapper.toResponse(existingUser)).thenReturn(responseDto);

        UserUpdateRequestDto req = new UserUpdateRequestDto();
        req.setEmail("john@example.com");  // chính xác email cũ

        userService.updateUser("user-uuid-001", req);

        verify(userRepository, never()).existsByEmail(anyString());
    }

    // ─── updateUserRoles() ────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUserRoles() - user không tồn tại → throw ACCOUNT_NOT_FOUND")
    void updateUserRoles_notFound_throwsException() {
        when(userRepository.findByUuid("no-uuid")).thenReturn(Optional.empty());
        UserRoleUpdateRequestDto req = new UserRoleUpdateRequestDto();
        req.setRoles(java.util.Set.of("staff"));

        assertThatThrownBy(() -> userService.updateUserRoles("no-uuid", req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("updateUserRoles() - role là 'admin' → throw FORBIDDEN_ACCESS")
    void updateUserRoles_adminRole_throwsForbidden() {
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(existingUser));
        UserRoleUpdateRequestDto req = new UserRoleUpdateRequestDto();
        req.setRoles(java.util.Set.of("admin"));

        assertThatThrownBy(() -> userService.updateUserRoles("user-uuid-001", req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN_ACCESS);
    }

    @Test
    @DisplayName("updateUserRoles() - role không tồn tại trong DB → throw ROLE_NOT_FOUND")
    void updateUserRoles_roleNotFound_throwsException() {
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName("ghost_role")).thenReturn(Optional.empty());

        UserRoleUpdateRequestDto req = new UserRoleUpdateRequestDto();
        req.setRoles(java.util.Set.of("ghost_role"));

        assertThatThrownBy(() -> userService.updateUserRoles("user-uuid-001", req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROLE_NOT_FOUND);
    }

    @Test
    @DisplayName("updateUserRoles() - thành công: gán roles mới cho user")
    void updateUserRoles_success_setsNewRoles() {
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(existingUser));

        Role staffRole = new Role(1L, "staff");
        when(roleRepository.findByName("staff")).thenReturn(Optional.of(staffRole));

        UserResponseDto responseDto = new UserResponseDto();
        when(userMapper.toResponse(existingUser)).thenReturn(responseDto);

        UserRoleUpdateRequestDto req = new UserRoleUpdateRequestDto();
        req.setRoles(java.util.Set.of("staff"));

        UserResponseDto result = userService.updateUserRoles("user-uuid-001", req);

        assertThat(result).isNotNull();
        assertThat(existingUser.getRoles()).contains(staffRole);
    }

    // ─── findByUsername() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUsername() - không tồn tại → throw ACCOUNT_NOT_FOUND")
    void findByUsername_notFound_throwsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername("ghost"))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("findByUsername() - thành công: trả về User")
    void findByUsername_success_returnsUser() {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(existingUser));
        User result = userService.findByUsername("johndoe");
        assertThat(result.getUsername()).isEqualTo("johndoe");
    }

    // ─── getUserById() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserById() - không tồn tại → throw ACCOUNT_NOT_FOUND")
    void getUserById_notFound_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("getUserById() - thành công: trả về UserResponseDto")
    void getUserById_success_returnsDto() {
        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setUuid("user-uuid-001");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userMapper.toResponse(existingUser)).thenReturn(responseDto);

        UserResponseDto result = userService.getUserById(1L);
        assertThat(result.getUuid()).isEqualTo("user-uuid-001");
    }

    // ─── findByUuid() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUuid() - không tồn tại → throw ACCOUNT_NOT_FOUND")
    void findByUuid_notFound_throwsException() {
        when(userRepository.findByUuid("xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUuid("xxx"))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("findByUuid() - thành công: trả về User")
    void findByUuid_success_returnsUser() {
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(existingUser));

        User result = userService.findByUuid("user-uuid-001");
        assertThat(result).isNotNull();
        assertThat(result.getUuid()).isEqualTo("user-uuid-001");
    }

    @Test
    @DisplayName("updateUser() - phone giống số cũ → không kiểm tra conflict")
    void updateUser_samePhone_doesNotCheckConflict() {
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(existingUser));
        UserResponseDto responseDto = new UserResponseDto();
        when(userMapper.toResponse(existingUser)).thenReturn(responseDto);

        UserUpdateRequestDto req = new UserUpdateRequestDto();
        req.setPhone("0901234567"); // chính xác phone cũ

        userService.updateUser("user-uuid-001", req);
        verify(userRepository, never()).existsByPhone(anyString());
    }

    // ─── getAllUsers() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllUsers() - trả về danh sách user phân trang")
    void getAllUsers_success_returnsPagedUsers() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<User> userPage = new org.springframework.data.domain.PageImpl<>(List.of(existingUser), pageable, 1);

        when(userRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(userPage);
        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setUuid("user-uuid-001");
        when(userMapper.toResponse(existingUser)).thenReturn(responseDto);

        var result = userService.getAllUsers("john", Status.ACTIVE, pageable);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getUuid()).isEqualTo("user-uuid-001");
    }

    @Test
    @DisplayName("getAllUsers() - verify specification lambda toPredicate execution")
    @SuppressWarnings("unchecked")
    void getAllUsers_specificationPredicate_executed() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(userRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        userService.getAllUsers("keyword", Status.ACTIVE, pageable);

        org.mockito.ArgumentCaptor<org.springframework.data.jpa.domain.Specification<User>> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(userRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<User> spec = captor.getValue();

        jakarta.persistence.criteria.Root<User> root = mock(jakarta.persistence.criteria.Root.class);
        jakarta.persistence.criteria.CriteriaQuery<?> query = mock(jakarta.persistence.criteria.CriteriaQuery.class);
        jakarta.persistence.criteria.CriteriaBuilder cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);
        jakarta.persistence.criteria.Subquery<Long> subquery = mock(jakarta.persistence.criteria.Subquery.class);
        jakarta.persistence.criteria.Root<User> subRoot = mock(jakarta.persistence.criteria.Root.class);
        jakarta.persistence.criteria.Join<User, Role> subRoles = mock(jakarta.persistence.criteria.Join.class);

        jakarta.persistence.criteria.Path pathId = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathRoleName = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathUsername = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathFullName = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathEmail = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathPhone = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathStatus = mock(jakarta.persistence.criteria.Path.class);

        jakarta.persistence.criteria.Predicate predIn = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predNotIn = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predEqualAdmin = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predStatus = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predOr = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predAnd = mock(jakarta.persistence.criteria.Predicate.class);

        when(query.subquery(Long.class)).thenReturn(subquery);
        when(subquery.from(User.class)).thenReturn(subRoot);
        when(subRoot.<User, Role>join("roles")).thenReturn(subRoles);
        when(subRoles.get("name")).thenReturn(pathRoleName);
        when(cb.equal(pathRoleName, "admin")).thenReturn(predEqualAdmin);
        when(subRoot.get("id")).thenReturn(pathId);
        when(subquery.select(pathId)).thenReturn(subquery);
        when(subquery.where(predEqualAdmin)).thenReturn(subquery);

        when(root.get("id")).thenReturn(pathId);
        when(pathId.in(subquery)).thenReturn(predIn);
        when(cb.not(predIn)).thenReturn(predNotIn);

        when(root.get("username")).thenReturn(pathUsername);
        when(root.get("fullName")).thenReturn(pathFullName);
        when(root.get("email")).thenReturn(pathEmail);
        when(root.get("phone")).thenReturn(pathPhone);
        when(root.get("status")).thenReturn(pathStatus);

        jakarta.persistence.criteria.Expression expLower = mock(jakarta.persistence.criteria.Expression.class);
        when(cb.lower(any())).thenReturn(expLower);
        jakarta.persistence.criteria.Predicate predLike = mock(jakarta.persistence.criteria.Predicate.class);
        when(cb.like(any(), anyString())).thenReturn(predLike);
        when(cb.or(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(predOr);
        when(cb.equal(pathStatus, Status.ACTIVE)).thenReturn(predStatus);
        when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(predAnd);

        jakarta.persistence.criteria.Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isNotNull();

        // Also test null keyword and null status branch
        userService.getAllUsers(null, null, pageable);
        captor = org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(userRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<User> specNoFilter = captor.getValue();
        jakarta.persistence.criteria.Predicate result2 = specNoFilter.toPredicate(root, query, cb);
        assertThat(result2).isNotNull();
    }
}
