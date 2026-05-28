package com.example.api.dto.response;
import lombok.*;
@Getter @Setter @Builder
public class LoginResponse {
    private String       accessToken;
    private String       refreshToken;
    private UserResponse user;
}
