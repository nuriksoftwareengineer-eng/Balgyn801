package com.nurba.java.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "catalog_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean active = true;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "catalogGroup", fetch = FetchType.LAZY)
    private List<Collection> collections = new ArrayList<>();
}
