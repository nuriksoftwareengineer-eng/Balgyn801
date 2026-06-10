package com.nurba.java.domain;

import com.nurba.java.enums.GarmentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "design_garments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DesignGarment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private Design design;

    @Enumerated(EnumType.STRING)
    @Column(name = "garment_type", nullable = false, length = 30)
    private GarmentType garmentType;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "designGarment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<DesignGarmentPrice> prices = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "design_garment_colors",
            joinColumns = @JoinColumn(name = "design_garment_id"),
            inverseJoinColumns = @JoinColumn(name = "color_id")
    )
    private Set<Color> colors = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "design_garment_sizes",
            joinColumns = @JoinColumn(name = "design_garment_id"),
            inverseJoinColumns = @JoinColumn(name = "size_id")
    )
    private Set<Size> sizes = new LinkedHashSet<>();
}
