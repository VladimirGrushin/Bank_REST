package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static com.example.bankcards.service.TestUtils.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private User regularUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        // Создаем тестовых пользователей
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(Role.ROLE_ADMIN);
        adminUser.setPassword("encodedAdminPassword");


        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setFirstName("John");
        regularUser.setLastName("Doe");
        regularUser.setRole(Role.ROLE_USER);
        regularUser.setPassword("encodedUserPassword");


        anotherUser = new User();
        anotherUser.setId(3L);
        anotherUser.setFirstName("Jane");
        anotherUser.setLastName("Smith");
        anotherUser.setRole(Role.ROLE_USER);
        anotherUser.setPassword("encodedAnotherPassword");



        SecurityContextHolder.setContext(securityContext);
    }

    private void mockAuthentication(User user) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getFirstName() + " " + user.getLastName());
        when(userRepository.findByFirstNameAndLastName(user.getFirstName(), user.getLastName()))
                .thenReturn(Optional.of(user));
    }

    @Test
    void createUser_AdminUser_ShouldCreateNewUser() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.existsByFirstNameAndLastName("New", "User")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(4L);
            return user;
        });

        // Act
        User result = userService.createUser("password123", "New", "User", Role.ROLE_USER);

        // Assert
        assertNotNull(result);
        assertEquals("New", result.getFirstName());
        assertEquals("User", result.getLastName());
        assertEquals(Role.ROLE_USER, result.getRole());
        assertEquals("encodedNewPassword", result.getPassword());

        verify(userRepository).save(argThat(user ->
                user.getFirstName().equals("New") &&
                        user.getLastName().equals("User") &&
                        user.getRole() == Role.ROLE_USER
        ));
    }

    @Test
    void createUser_ExistingUser_ShouldThrowBadRequestException() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.existsByFirstNameAndLastName("Existing", "User")).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () ->
                userService.createUser("password", "Existing", "User", Role.ROLE_USER));
    }

    @Test
    void createUser_NonAdminUser_ShouldThrowAccessDeniedException() {
        // Arrange
        mockAuthentication(regularUser);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                userService.createUser("password", "New", "User", Role.ROLE_USER));
    }

    @Test
    void deleteUser_AdminUser_ShouldDeleteUser() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.findById(3L)).thenReturn(Optional.of(anotherUser));

        // Act
        userService.deleteUser(3L);

        // Assert
        verify(userRepository).delete(anotherUser);
    }

    @Test
    void deleteUser_DeleteSelf_ShouldThrowBadRequestException() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // Act & Assert
        assertThrows(BadRequestException.class, () ->
                userService.deleteUser(1L));
    }

    @Test
    void deleteUser_NonAdminUser_ShouldThrowAccessDeniedException() {
        // Arrange
        mockAuthentication(regularUser);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                userService.deleteUser(3L));
    }

    @Test
    void changeUserRole_AdminUser_ShouldChangeRole() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        // Act
        userService.changeUserRole(2L, Role.ROLE_ADMIN);

        // Assert
        assertEquals(Role.ROLE_ADMIN, regularUser.getRole());
        verify(userRepository).save(regularUser);
    }

    @Test
    void findUserById_AdminUser_ShouldReturnUser() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

        // Act
        User result = userService.findUserBuId(2L);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("John", result.getFirstName());
    }

    @Test
    void findUserById_UserNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                userService.findUserBuId(999L));
    }

    @Test
    void findUserByName_AdminUser_ShouldReturnUser() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(regularUser));

        // Act
        User result = userService.findUserByName("John", "Doe");

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("John", result.getFirstName());
    }

    @Test
    void findUsersByRole_AdminUser_ShouldReturnFilteredUsers() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.findByRole(Role.ROLE_USER)).thenReturn(List.of(regularUser, anotherUser));

        // Act
        List<User> result = userService.findUsersByRole(Role.ROLE_USER);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(user -> user.getRole() == Role.ROLE_USER));
    }

    @Test
    void getAllUsers_AdminUser_ShouldReturnAllUsers() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.findAll()).thenReturn(List.of(adminUser, regularUser, anotherUser));

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertEquals(3, result.size());
    }

    @Test
    void getMyAccount_ShouldReturnCurrentUser() {
        // Arrange
        mockAuthentication(regularUser);

        // Act
        User result = userService.getMyAccount();

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("John", result.getFirstName());
    }

    @Test
    void changeMyPassword_ValidPassword_ShouldChangePassword() {
        // Arrange
        mockAuthentication(regularUser);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        // Act
        userService.changeMyPassword("newPassword123");

        // Assert
        assertEquals("encodedNewPassword", regularUser.getPassword());
        verify(userRepository).save(regularUser);
    }

    @Test
    void changeMyPassword_ShortPassword_ShouldThrowBadRequestException() {

        // Act & Assert
        assertThrows(BadRequestException.class, () ->
                userService.changeMyPassword("short"));
    }

    @Test
    void changeMyPassword_NullPassword_ShouldThrowBadRequestException() {


        assertThrows(BadRequestException.class, () ->
                userService.changeMyPassword(null));
    }

    @Test
    void changeMyPassword_EmptyPassword_ShouldThrowBadRequestException() {
        // Arrange

        assertThrows(BadRequestException.class, () ->
                userService.changeMyPassword(""));
    }

    @Test
    void isUserAdmin_AdminUser_ShouldNotThrowException() {
        // Arrange
        mockAuthentication(adminUser);

        // Act & Assert - не должно бросать исключение
        assertDoesNotThrow(() -> {
            userService.createUser("password", "Test", "User", Role.ROLE_USER);
        });
    }

    @Test
    void isUserAdmin_NonAdminUser_ShouldThrowAccessDeniedException() {
        // Arrange
        mockAuthentication(regularUser);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                userService.getAllUsers());
    }

    @Test
    void getCurrentUser_ValidUser_ShouldReturnUser() {
        // Arrange
        mockAuthentication(regularUser);

        // Act
        User result = userService.getMyAccount();

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getId());
    }

    @Test
    void getCurrentUser_UserNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("Non Existing User");
        when(userRepository.findByFirstNameAndLastName("Non", "Existing")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                userService.getMyAccount());
    }

    @Test
    void createUser_WithAdminRole_ShouldCreateAdminUser() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.existsByFirstNameAndLastName("Super", "Admin")).thenReturn(false);
        when(passwordEncoder.encode("adminPass")).thenReturn("encodedAdminPass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(5L);
            return user;
        });

        // Act
        User result = userService.createUser("adminPass", "Super", "Admin", Role.ROLE_ADMIN);

        // Assert
        assertNotNull(result);
        assertEquals(Role.ROLE_ADMIN, result.getRole());
        assertTrue(result.isAdmin());
    }

    @Test
    void changeUserRole_ToAdmin_ShouldMakeUserAdmin() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        // Act
        userService.changeUserRole(2L, Role.ROLE_ADMIN);

        // Assert
        assertEquals(Role.ROLE_ADMIN, regularUser.getRole());
        assertTrue(regularUser.isAdmin());
    }

    @Test
    void changeUserRole_ToUser_ShouldRemoveAdmin() {
        mockAuthentication(adminUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        // Act
        userService.changeUserRole(1L, Role.ROLE_USER);

        // Assert - проверяем только что роль изменилась
        assertEquals(Role.ROLE_USER, adminUser.getRole());
        assertFalse(adminUser.isAdmin());
        verify(userRepository).save(adminUser);
    }

    @Test
    void getAllUsers_EmptyDatabase_ShouldReturnEmptyList() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.findAll()).thenReturn(List.of());

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findUsersByRole_NoUsersWithRole_ShouldReturnEmptyList() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.findByRole(Role.ROLE_ADMIN)).thenReturn(List.of());

        // Act
        List<User> result = userService.findUsersByRole(Role.ROLE_ADMIN);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}