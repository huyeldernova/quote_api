package com.example.api.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="email_logs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class EmailLog {

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="quote_id")
    private Quote quote;
    private String toEmail;

    private String ccEmail;

    @Column(columnDefinition="TEXT")
    private String subject;

    @Column(nullable=false, unique=true)
    private String trackingToken;

    private String pdfS3Key;

    @Column(nullable=false)
    private LocalDateTime sentAt;

    @Builder.Default
    private Boolean opened    = false;

    private LocalDateTime openedAt;

    @Builder.Default
    private Boolean clicked   = false;

    private LocalDateTime clickedAt;
}
