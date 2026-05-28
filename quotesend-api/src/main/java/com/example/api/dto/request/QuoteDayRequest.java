package com.example.api.dto.request;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
@Getter
public class QuoteDayRequest {
    @NotNull(message="Day number is required") private Integer dayNumber;
    private String location;
    private String dateLabel;
    private String hotel;
    private String sights;
    private String note;
    private String imageUrl;
}
