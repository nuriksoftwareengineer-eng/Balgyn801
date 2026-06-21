package com.nurba.java.domain;

import com.nurba.java.model.ProductColorOption;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Boolean inStock;
    /** Совпадает с подписью категории на сайте (см. StoreCategories.PRODUCT_CATEGORY_LABELS). */
    private String category;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> sizes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<ProductColorOption> colors = new ArrayList<>();

    // ── Габариты для упаковки CDEK (опциональны, V18) ────────────────────────────
    // Только для legacy-товаров. Вес design-вариантов считает GarmentWeightService —
    // здесь логику веса не дублируем. NULL = брать значения по умолчанию из конфигурации.

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(name = "length_cm")
    private Integer lengthCm;

    @Column(name = "width_cm")
    private Integer widthCm;

    @Column(name = "height_cm")
    private Integer heightCm;

    private LocalDateTime createdAt;
}
