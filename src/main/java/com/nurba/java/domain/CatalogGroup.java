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

    /** Card preview image — used on the catalog index page. */
    @Column(name = "cover_image_url", length = 512)
    private String coverImageUrl;

    /** Full-width header image — used on the group detail page. */
    @Column(name = "banner_image_url", length = 512)
    private String bannerImageUrl;

    private LocalDateTime createdAt;

    @Column(name = "name_kk", length = 200)
    private String nameKk;

    @Column(name = "name_en", length = 200)
    private String nameEn;

    @OneToMany(mappedBy = "catalogGroup", fetch = FetchType.LAZY)
    private List<Collection> collections = new ArrayList<>();
}
