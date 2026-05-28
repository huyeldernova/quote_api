package com.example.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ResetPasswordRequest {
    @NotBlank private String email;
    @NotBlank private String otp;
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
    @NotBlank
    private String confirmPassword;
}