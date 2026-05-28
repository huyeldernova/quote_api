package com.example.api.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name="quote_costs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class QuoteCost {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="quote_id")
    private Quote quote;

    private String  label;

    private Double  amount;

    private Integer sortOrder;
}
