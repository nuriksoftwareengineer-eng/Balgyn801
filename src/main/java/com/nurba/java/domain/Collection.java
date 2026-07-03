package com.nurba.java.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "collections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private CatalogGroup catalogGroup;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Обложка коллекции (URL картинки) — для карточки/превью коллекции. */
    @Column(name = "cover_image_url", length = 512)
    private String coverImageUrl;

    /** Баннер коллекции (URL картинки) — для шапки страницы коллекции. */
    @Column(name = "banner_image_url", length = 512)
    private String bannerImageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean active = true;

    private LocalDateTime createdAt;

    @Column(name = "name_kk", length = 200)
    private String nameKk;

    @Column(name = "name_en", length = 200)
    private String nameEn;

    @OneToMany(mappedBy = "collection", fetch = FetchType.LAZY)
    private List<Design> designs = new ArrayList<>();
}
