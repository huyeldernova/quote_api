package com.example.api.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import java.util.List;
@Getter
public class QuoteFormRequest {
    private String clientName;
    private String clientEmail;
    private String tourName;
    private String tourType;
    private String startDate;
    private String endDate;
    private String routeFrom;
    private String routeTo;
    private String arrivingAt;
    private String departingFrom;
    private String transport;
    private String starRating;
    @NotNull(message="Pax count is required") @Min(value=1, message="Pax count must be at least 1")
    private Integer paxCount;
    @NotNull(message="Profit margin is required") @Min(0) @Max(99)
    private Integer profitMargin;
    @Valid @NotEmpty(message="At least one cost item is required")
    private List<QuoteCostRequest> costs;
    @Valid @NotEmpty(message="At least one day is required")
    private List<QuoteDayRequest>  days;
}
