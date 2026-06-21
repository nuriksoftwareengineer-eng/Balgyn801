package com.nurba.java.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser appUser;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, length = 150)
    private String city;

    @Column(nullable = false, length = 250)
    private String street;

    @Column(nullable = false, length = 50)
    private String apartment;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "recipient_name", nullable = false, length = 200)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 30)
    private String recipientPhone;

    private LocalDateTime createdAt;
}
