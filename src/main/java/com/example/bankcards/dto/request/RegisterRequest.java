package com.example.bankcards.dto.request;

import com.example.bankcards.entity.Role;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RegisterRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Password is required")
    private String password;

    private Role role = Role.ROLE_USER;
}
