package com.example.api.dto.request;
import jakarta.validation.constraints.*;
import lombok.Getter;
@Getter
public class QuoteCostRequest {
    @NotBlank(message="Cost label is required") private String  label;
    @NotNull(message="Cost amount is required")  private Double  amount;
    private Integer sortOrder;
}
