package com.example.api.dto.response;
import lombok.*;
@Getter @Setter @Builder
public class QuoteDayResponse {
    private Long    id;
    private Integer dayNumber;
    private String  location;
    private String  dateLabel;
    private String  hotel;
    private String  sights;
    private String  note;
    private String  imageUrl;
}
