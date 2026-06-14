package com.nurba.java.domain;

import com.nurba.java.enums.GarmentType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "size_chart_images")
@Data
@NoArgsConstructor
public class SizeChartImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "garment_type", nullable = false, unique = true, length = 30)
    private GarmentType garmentType;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @Column(length = 128)
    private String title;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
