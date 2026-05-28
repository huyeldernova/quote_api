package com.example.api.dto.response;
import lombok.*;
@Getter @Setter @Builder
public class UserResponse {
    private String id;
    private String name;
    private String email;
    private String company;
    private String role;
}
