package com.example.api.dto.response;
import lombok.*;
@Getter @Setter @Builder
public class DuplicateQuoteResponse {
    private Long   newQuoteId;
    private String quoteNumber;
}
