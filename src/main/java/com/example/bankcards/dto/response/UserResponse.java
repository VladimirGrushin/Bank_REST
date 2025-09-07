package com.example.bankcards.dto.response;

import com.example.bankcards.entity.Role;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private Role role;
}