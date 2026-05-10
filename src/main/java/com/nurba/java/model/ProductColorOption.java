package com.nurba.java.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Вариант цвета в каталоге (хранится в JSON-колонке товара). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductColorOption {
    private String name;
    /** Необязательный HEX, например #0a0a0a */
    private String hex;
}
