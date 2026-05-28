package com.example.api.dto.response;
import lombok.*;
@Getter @Setter @Builder
public class EmailStatsResponse {
    private Long   totalSent;
    private Long   totalOpened;
    private Long   totalClicked;
    private Double openRate;
}
