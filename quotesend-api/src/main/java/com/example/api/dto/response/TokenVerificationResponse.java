package com.example.api.dto.response;
import lombok.*;
import java.util.List;
@Getter @Setter @Builder
public class TokenVerificationResponse {
    private boolean      isValid;
    private List<String> authorities;
}
