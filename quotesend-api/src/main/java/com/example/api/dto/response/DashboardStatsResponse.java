package com.example.api.dto.response;
import lombok.*;
import java.util.List;
@Getter @Setter @Builder
public class DashboardStatsResponse {
    private Long                    totalQuotes;
    private Double                  estimatedRevenue;
    private Long                    draftCount;
    private Long                    sentCount;
    private Long                    viewedCount;
    private Long                    emailsSent;
    private Double                  openRate;
    private List<QuoteListItemResponse> recentQuotes;
    private List<EmailLogResponse>      recentEmails;
}
