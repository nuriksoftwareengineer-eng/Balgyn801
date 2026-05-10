package com.nurba.java.constants;

import java.util.Set;

/** Должны совпадать с чипами категорий на витрине (кроме «Смотреть всё»). */
public final class StoreCategories {

    public static final String VIEW_ALL = "Смотреть всё";

    public static final Set<String> PRODUCT_CATEGORY_LABELS = Set.of(
            "Распродажа",
            "Игры",
            "Аниме",
            "Спорт",
            "Музыка"
    );

    private StoreCategories() {
    }

    public static boolean isProductCategory(String value) {
        return value != null && PRODUCT_CATEGORY_LABELS.contains(value.trim());
    }

    public static boolean isNoFilter(String categoryParam) {
        if (categoryParam == null || categoryParam.isBlank()) {
            return true;
        }
        return VIEW_ALL.equals(categoryParam.trim());
    }
}
