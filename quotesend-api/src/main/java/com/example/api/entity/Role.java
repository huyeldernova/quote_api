package com.example.api.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="roles")
@NoArgsConstructor @AllArgsConstructor @Builder @Getter @Setter
public class Role {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
    @Column(unique=true,nullable=false) private String name;
    private String description;
}
