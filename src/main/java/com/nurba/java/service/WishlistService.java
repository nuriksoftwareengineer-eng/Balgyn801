package com.nurba.java.service;

import com.nurba.java.dto.responce.WishlistItemResponse;

import java.util.List;

public interface WishlistService {
    List<WishlistItemResponse> getWishlist(String email);
    WishlistItemResponse addToWishlist(String email, Long designId);
    void removeFromWishlist(String email, Long designId);
    boolean isInWishlist(String email, Long designId);
    long countWishlist(String email);
}
