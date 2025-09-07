package com.example.bankcards.dto.response;

import lombok.Data;

@Data
public class AuthResponse {
    private String firstName;
    private String lastName;
    private String role;
    private String token;

    public AuthResponse(String firstName, String lastName, String role, String token){
        this.token = token;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }


}
