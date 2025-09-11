package com.example.bankcards.controller;

import com.example.bankcards.dto.request.AuthRequest;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.service.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthRequest validAuthRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        validAuthRequest = new AuthRequest();
        validAuthRequest.setFirstName("John");
        validAuthRequest.setLastName("Doe");
        validAuthRequest.setPassword("password123");

        authResponse = new AuthResponse();
        authResponse.setToken("jwt-token-123");
        authResponse.setUserId(1L);
        authResponse.setFirstName("John");
        authResponse.setLastName("Doe");
    }

    @Test
    void login_ShouldReturnAuthResponse() throws Exception {
        // Arrange
        when(authService.authenticate(any(AuthRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        when(authService.authenticate(any(AuthRequest.class)))
                .thenThrow(new ResourceNotFoundException("User", "firstName", "John", "lastName", "Doe"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with firstName: 'John' and lastName: 'Doe'"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    void register_ShouldReturnAuthResponse() throws Exception {
        // Arrange
        when(authService.register(any(AuthRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void register_WithExistingUser_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(authService.register(any(AuthRequest.class)))
                .thenThrow(new BadRequestException("User with name 'John Doe' already exists"));

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User with name 'John Doe' already exists"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void registerAdmin_ShouldReturnAuthResponse() throws Exception {
        // Arrange
        when(authService.registerAdmin(any(AuthRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void registerAdmin_WithExistingUser_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(authService.registerAdmin(any(AuthRequest.class)))
                .thenThrow(new BadRequestException("User with name 'John Doe' already exists"));

        // Act & Assert
        mockMvc.perform(post("/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User with name 'John Doe' already exists"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void logout_ShouldReturnOk() throws Exception {
        // Arrange
        doNothing().when(authService).logout();

        // Act & Assert
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk());
    }

    @Test
    void testEndpoint_ShouldReturnWorkingMessage() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/auth/test"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Auth controller is working!")));
    }

    @Test
    void login_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange - создаем невалидный запрос без обязательных полей
        AuthRequest invalidRequest = new AuthRequest();
        invalidRequest.setFirstName(""); // пустое имя
        invalidRequest.setLastName(""); // пустая фамилия
        invalidRequest.setPassword(""); // пустой пароль

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange - создаем невалидный запрос без обязательных полей
        AuthRequest invalidRequest = new AuthRequest();
        invalidRequest.setFirstName(""); // пустое имя
        invalidRequest.setLastName(""); // пустая фамилия
        invalidRequest.setPassword(""); // пустой пароль

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}