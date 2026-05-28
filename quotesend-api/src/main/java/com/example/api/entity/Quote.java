package com.example.api.entity;

import com.example.api.enums.QuoteStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="quotes")
@NoArgsConstructor @AllArgsConstructor @Builder @Getter @Setter
public class Quote {

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String quoteNumber;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;

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

    @Column(nullable=false)
    private Integer paxCount;

    @Column(nullable=false)
    private Integer profitMargin;

    private Double pricePerPerson;
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    @Builder.Default
    private QuoteStatus status = QuoteStatus.DRAFT;

    @OneToMany(mappedBy="quote", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<QuoteCost> costs = new ArrayList<>();

    @OneToMany(mappedBy="quote", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("dayNumber ASC")
    @Builder.Default
    private List<QuoteDay> days = new ArrayList<>();

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp   private LocalDateTime updatedAt;
}
