package com.example.api.dto.response;
import com.example.api.enums.QuoteStatus;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @Builder
public class QuoteListItemResponse {
    private Long        id;
    private String      quoteNumber;
    private String      clientName;
    private String      tourName;
    private String      startDate;
    private Integer     paxCount;
    private Double      pricePerPerson;
    private Double      totalAmount;
    private QuoteStatus status;
    private String      createdAt;
}
