package com.nurba.java.dto.responce;

import lombok.Value;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic pagination wrapper returned by all admin list endpoints.
 * Encapsulates Spring's Page<T> without exposing framework internals to the API.
 */
@Value
public class PageResponse<T> {
    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean hasNext;
    boolean hasPrevious;

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
