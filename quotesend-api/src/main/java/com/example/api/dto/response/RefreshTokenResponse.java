package com.example.api.dto.response;
import lombok.*;
@Getter @Setter @Builder
public class RefreshTokenResponse {
    private String accessToken;
    private String refreshToken;
}
