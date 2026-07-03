package com.nurba.java.dto.responce;

import java.time.LocalDateTime;

public record WishlistItemResponse(
        Long id,
        Long designId,
        String designName,
        String designSlug,
        String mainImageUrl,
        String collectionName,
        String groupSlug,
        LocalDateTime addedAt
) {}
