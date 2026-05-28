package com.example.api.dto.response;
import lombok.*;
@Getter @Setter @Builder
public class QuoteCostResponse {
    private Long    id;
    private String  label;
    private Double  amount;
    private Integer sortOrder;
}
