package com.example.bankcards.dto.response;

import lombok.Data;

@Data
public class AuthResponse {
    private String firstName;
    private String lastName;
    private String role;
    private String token;
    private Long userId;

}
