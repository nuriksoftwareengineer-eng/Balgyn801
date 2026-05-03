package com.nurba.java.domain;

import com.nurba.java.enums.CustomDesignStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "custom_designs")
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
