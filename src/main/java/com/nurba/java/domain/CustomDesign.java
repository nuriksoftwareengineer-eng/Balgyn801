package com.nurba.java.domain;

import com.nurba.java.enums.CustomDesignStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "custom_designs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomDesign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String referenceImageUrl;

    @Enumerated(EnumType.STRING)
    private CustomDesignStatus status;

    private LocalDateTime createdAt;
}
