package com.example.api.dto.response;
import com.example.api.enums.QuoteStatus;
import lombok.*;
import java.util.List;
@Getter @Setter @Builder
public class QuoteResponse {
    private Long    id;
    private String  quoteNumber;
    private String  clientName;
    private String  clientEmail;
    private String  tourName;
    private String  tourType;
    private String  startDate;
    private String  endDate;
    private String  routeFrom;
    private String  routeTo;
    private String  arrivingAt;
    private String  departingFrom;
    private String  transport;
    private String  starRating;
    private Integer paxCount;
    private Integer profitMargin;
    private Double  pricePerPerson;
    private Double  totalAmount;
    private QuoteStatus            status;
    private List<QuoteCostResponse> costs;
    private List<QuoteDayResponse>  days;
    private String createdAt;
    private String updatedAt;
}
