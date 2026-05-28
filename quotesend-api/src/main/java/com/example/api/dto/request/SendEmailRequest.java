package com.example.api.dto.request;
import jakarta.validation.constraints.*;
import lombok.Getter;
@Getter
public class SendEmailRequest {
    @NotBlank(message="Recipient email is required") @Email
    private String toEmail;
    @Email private String ccEmail;
    @NotBlank(message="Subject is required") private String subject;
    @NotBlank(message="Message is required") private String message;
}
