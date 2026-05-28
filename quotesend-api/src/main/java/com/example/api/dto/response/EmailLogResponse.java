package com.example.api.dto.response;
import lombok.*;
@Getter @Setter @Builder
public class EmailLogResponse {
    private Long    id;
    private Long    quoteId;
    private String  toEmail;
    private String  ccEmail;
    private String  subject;
    private String  sentAt;
    private Boolean opened;
    private String  openedAt;
    private Boolean clicked;
    private String  clickedAt;
}
