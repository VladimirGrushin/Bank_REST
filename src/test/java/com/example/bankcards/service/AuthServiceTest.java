package com.example.bankcards.service;

import com.example.bankcards.dto.request.AuthRequest;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.Role;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static com.example.bankcards.service.TestUtils.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private AuthRequest validAuthRequest;
    private AuthRequest registerRequest;

    @BeforeEach
    void setUp() {
        // Создаем тестового пользователя
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.ROLE_USER);

        // Создаем тестовые запросы
        validAuthRequest = new AuthRequest();
        validAuthRequest.setFirstName("John");
        validAuthRequest.setLastName("Doe");
        validAuthRequest.setPassword("password123");

        registerRequest = new AuthRequest();
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Smith");
        registerRequest.setPassword("newPassword123");

        // Мокируем SecurityContext
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void authenticate_ValidCredentials_ShouldReturnAuthResponse() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByFirstNameAndLastName("John", "Doe"))
                .thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(testUser)).thenReturn("jwt-token");

        // Act
        AuthResponse response = authService.authenticate(validAuthRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());

        verify(securityContext).setAuthentication(authentication);
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("John Doe", "password123")
        );
    }

    @Test
    void authenticate_InvalidCredentials_ShouldThrowBadCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () ->
                authService.authenticate(validAuthRequest));
    }

    @Test
    void authenticate_UserNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByFirstNameAndLastName("John", "Doe"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                authService.authenticate(validAuthRequest));
    }

    @Test
    void register_NewUser_ShouldCreateUserAndReturnAuthResponse() {
        // Arrange
        when(userRepository.existsByFirstNameAndLastName("Jane", "Smith")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("new-jwt-token");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("new-jwt-token", response.getToken());
        assertEquals(2L, response.getUserId());
        assertEquals("Jane", response.getFirstName());
        assertEquals("Smith", response.getLastName());

        verify(userRepository).save(argThat(user ->
                user.getFirstName().equals("Jane") &&
                        user.getLastName().equals("Smith") &&
                        user.getPassword().equals("encodedPassword") &&
                        user.getRole() == Role.ROLE_USER
        ));
    }

    @Test
    void register_ExistingUser_ShouldThrowBadRequestException() {
        // Arrange
        when(userRepository.existsByFirstNameAndLastName("Jane", "Smith")).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                authService.register(registerRequest));

        assertTrue(exception.getMessage().contains("User with name 'Jane Smith' already exists"));
    }

    @Test
    void registerAdmin_NewAdminUser_ShouldCreateAdminUser() {
        // Arrange
        when(userRepository.existsByFirstNameAndLastName("Admin", "User")).thenReturn(false);
        when(passwordEncoder.encode("adminPassword")).thenReturn("encodedAdminPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(3L);
            return user;
        });
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("admin-jwt-token");

        AuthRequest adminRequest = new AuthRequest();
        adminRequest.setFirstName("Admin");
        adminRequest.setLastName("User");
        adminRequest.setPassword("adminPassword");

        // Act
        AuthResponse response = authService.registerAdmin(adminRequest);

        // Assert
        assertNotNull(response);
        assertEquals("admin-jwt-token", response.getToken());

        verify(userRepository).save(argThat(user ->
                user.getFirstName().equals("Admin") &&
                        user.getLastName().equals("User") &&
                        user.getPassword().equals("encodedAdminPassword") &&
                        user.getRole() == Role.ROLE_ADMIN
        ));
    }

    @Test
    void registerAdmin_ExistingUser_ShouldThrowBadRequestException() {
        // Arrange
        when(userRepository.existsByFirstNameAndLastName("Admin", "User")).thenReturn(true);

        AuthRequest adminRequest = new AuthRequest();
        adminRequest.setFirstName("Admin");
        adminRequest.setLastName("User");
        adminRequest.setPassword("adminPassword");

        // Act & Assert
        assertThrows(BadRequestException.class, () ->
                authService.registerAdmin(adminRequest));
    }

    @Test
    void createAuthResponse_ShouldCreateCorrectResponse() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");

        // Act
        AuthResponse response = authService.createAuthResponse(user, "test-token");

        // Assert
        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("Test", response.getFirstName());
        assertEquals("User", response.getLastName());
    }

    @Test
    void createUsername_ShouldConcatenateFirstNameAndLastName() {
        // Act
        String username = authService.createUsername("John", "Doe");

        // Assert
        assertEquals("John Doe", username);
    }

    @Test
    void createUsername_WithEmptyNames_ShouldHandleCorrectly() {
        // Act
        String username1 = authService.createUsername("", "Doe");
        String username2 = authService.createUsername("John", "");
        String username3 = authService.createUsername("", "");

        // Assert
        assertEquals(" Doe", username1);
        assertEquals("John ", username2);
        assertEquals(" ", username3);
    }

    @Test
    void logout_ShouldClearSecurityContext() {
        // Act
        authService.logout();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void register_PasswordEncoding_ShouldUsePasswordEncoder() {
        // Arrange
        when(userRepository.existsByFirstNameAndLastName("Jane", "Smith")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("properly-encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("token");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        verify(passwordEncoder).encode("newPassword123");
    }

    @Test
    void authenticate_WithDifferentNameFormats_ShouldHandleCorrectly() {
        // Arrange
        AuthRequest requestWithSpaces = new AuthRequest();
        requestWithSpaces.setFirstName("John Michael");
        requestWithSpaces.setLastName("Doe Smith");
        requestWithSpaces.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByFirstNameAndLastName("John Michael", "Doe Smith"))
                .thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(testUser)).thenReturn("token");

        // Act
        AuthResponse response = authService.authenticate(requestWithSpaces);

        // Assert
        assertNotNull(response);
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("John Michael Doe Smith", "password")
        );
    }

    @Test
    void register_WithSpecialCharacters_ShouldHandleCorrectly() {
        // Arrange
        AuthRequest specialRequest = new AuthRequest();
        specialRequest.setFirstName("Jöhn");
        specialRequest.setLastName("Döe");
        specialRequest.setPassword("pässwörd");

        when(userRepository.existsByFirstNameAndLastName("Jöhn", "Döe")).thenReturn(false);
        when(passwordEncoder.encode("pässwörd")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(4L);
            return user;
        });
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("token");

        // Act
        AuthResponse response = authService.register(specialRequest);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(argThat(user ->
                user.getFirstName().equals("Jöhn") &&
                        user.getLastName().equals("Döe")
        ));
    }

    @Test
    void authenticate_WithEmptyPassword_ShouldThrowException() {
        // Arrange
        AuthRequest emptyPasswordRequest = new AuthRequest();
        emptyPasswordRequest.setFirstName("John");
        emptyPasswordRequest.setLastName("Doe");
        emptyPasswordRequest.setPassword("");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Empty password"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () ->
                authService.authenticate(emptyPasswordRequest));
    }
}