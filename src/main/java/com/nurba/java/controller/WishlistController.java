package com.nurba.java.controller;

import com.nurba.java.dto.responce.WishlistItemResponse;
import com.nurba.java.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public List<WishlistItemResponse> getWishlist(Principal principal) {
        return wishlistService.getWishlist(principal.getName());
    }

    @PostMapping("/{designId}")
    public WishlistItemResponse add(@PathVariable Long designId, Principal principal) {
        return wishlistService.addToWishlist(principal.getName(), designId);
    }

    @DeleteMapping("/{designId}")
    public ResponseEntity<Void> remove(@PathVariable Long designId, Principal principal) {
        wishlistService.removeFromWishlist(principal.getName(), designId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check/{designId}")
    public Map<String, Object> check(@PathVariable Long designId, Principal principal) {
        boolean inWishlist = wishlistService.isInWishlist(principal.getName(), designId);
        long count = wishlistService.countWishlist(principal.getName());
        return Map.of("inWishlist", inWishlist, "count", count);
    }

    @GetMapping("/count")
    public Map<String, Long> count(Principal principal) {
        return Map.of("count", wishlistService.countWishlist(principal.getName()));
    }
}
