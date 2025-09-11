package com.example.bankcards.controller;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.service.JwtTokenProvider;
import com.example.bankcards.service.UserService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(Role.ROLE_ADMIN);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setFirstName("John");
        regularUser.setLastName("Doe");
        regularUser.setRole(Role.ROLE_USER);
    }

    @Test
    void getMyProfile_ShouldReturnUserResponse() throws Exception {
        when(userService.getMyAccount()).thenReturn(regularUser);

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void changeMyPassword_ShouldReturnOk() throws Exception {
        doNothing().when(userService).changeMyPassword(anyString());

        mockMvc.perform(patch("/users/me/password")
                        .param("newPassword", "newPassword123"))
                .andExpect(status().isOk());

        verify(userService).changeMyPassword("newPassword123");
    }

    @Test
    void changeMyPassword_WithShortPassword_ShouldReturnBadRequest() throws Exception {
        doThrow(new BadRequestException("Password too short"))
                .when(userService).changeMyPassword(anyString());

        mockMvc.perform(patch("/users/me/password")
                        .param("newPassword", "short"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsers_ShouldReturnUsersList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(regularUser, adminUser));

        mockMvc.perform(get("/users/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[1].id").value(1))
                .andExpect(jsonPath("$[1].firstName").value("Admin"));
    }

    @Test
    void createUser_ShouldReturnUserResponse() throws Exception {
        when(userService.createUser(anyString(), anyString(), anyString(), any(Role.class)))
                .thenReturn(regularUser);

        mockMvc.perform(post("/users")
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("password", "password123")
                        .param("role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void createUser_WithExistingUser_ShouldReturnBadRequest() throws Exception {
        when(userService.createUser(anyString(), anyString(), anyString(), any(Role.class)))
                .thenThrow(new BadRequestException("User already exists"));

        mockMvc.perform(post("/users")
                        .param("firstName", "Existing")
                        .param("lastName", "User")
                        .param("password", "password123")
                        .param("role", "ROLE_USER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_ShouldReturnNoContent() throws Exception {
        doNothing().when(userService).deleteUser(anyLong());

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    void deleteUser_WithSelfDeletion_ShouldReturnBadRequest() throws Exception {
        doThrow(new BadRequestException("Cannot delete yourself"))
                .when(userService).deleteUser(anyLong());

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeUserRole_ShouldReturnUserResponse() throws Exception {
        when(userService.findUserBuId(anyLong())).thenReturn(regularUser);
        doNothing().when(userService).changeUserRole(anyLong(), any(Role.class));

        mockMvc.perform(patch("/users/1/role")
                        .param("newRole", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void findUserByName_ShouldReturnUserResponse() throws Exception {
        when(userService.findUserByName(anyString(), anyString())).thenReturn(regularUser);

        mockMvc.perform(get("/users/search")
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void findUserByName_WithNonExistingUser_ShouldReturnNotFound() throws Exception {
        when(userService.findUserByName(anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/users/search")
                        .param("firstName", "Non")
                        .param("lastName", "Existing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findUserById_ShouldReturnUserResponse() throws Exception {
        when(userService.findUserBuId(anyLong())).thenReturn(regularUser);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void findUsersByRole_ShouldReturnUsersList() throws Exception {
        when(userService.findUsersByRole(any(Role.class))).thenReturn(List.of(regularUser));

        mockMvc.perform(get("/users/by-role/ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].role").value("ROLE_USER"));
    }

    @Test
    void createUser_WithMissingParameters_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/users")
                        .param("lastName", "User")
                        .param("password", "password123")
                        .param("role", "ROLE_USER"))
                .andExpect(status().isBadRequest());
    }
}