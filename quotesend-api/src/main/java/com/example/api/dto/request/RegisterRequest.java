package com.example.api.dto.request;
import jakarta.validation.constraints.*;
import lombok.Getter;
@Getter
public class RegisterRequest {
    @NotBlank(message="Name is required") @Size(min=2, message="Name must be at least 2 characters")
    private String name;
    @NotBlank(message="Email is required") @Email(message="Invalid email format")
    private String email;
    @NotBlank(message="Password is required") @Size(min=8, message="Password must be at least 8 characters")
    private String password;
    private String company;
}
