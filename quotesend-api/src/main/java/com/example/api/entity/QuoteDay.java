package com.example.api.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="quote_days")
@NoArgsConstructor @AllArgsConstructor @Builder @Getter @Setter
public class QuoteDay {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="quote_id")
    private Quote quote;

    private Integer dayNumber;

    private String  location;

    private String  dateLabel;

    private String  hotel;

    @Column(columnDefinition="TEXT")
    private String sights;

    private String  note;

    private String  imageUrl;
}
