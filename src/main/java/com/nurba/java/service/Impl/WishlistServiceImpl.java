package com.nurba.java.service.Impl;

import com.nurba.java.domain.AppUser;
import com.nurba.java.domain.Design;
import com.nurba.java.domain.WishlistItem;
import com.nurba.java.dto.responce.WishlistItemResponse;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.repositories.AppUserRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.repositories.WishlistRepository;
import com.nurba.java.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final AppUserRepository userRepository;
    private final DesignRepository designRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(String email) {
        AppUser user = findUser(email);
        return wishlistRepository.findByUserIdWithDesign(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public WishlistItemResponse addToWishlist(String email, Long designId) {
        AppUser user = findUser(email);
        if (wishlistRepository.existsByUser_IdAndDesign_Id(user.getId(), designId)) {
            throw new BusinessRuleException("Дизайн уже добавлен в избранное");
        }
        Design design = designRepository.findById(designId)
                .orElseThrow(() -> new NotFoundException("Дизайн не найден: " + designId));
        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setDesign(design);
        return toResponse(wishlistRepository.save(item));
    }

    @Override
    @Transactional
    public void removeFromWishlist(String email, Long designId) {
        AppUser user = findUser(email);
        wishlistRepository.deleteByUser_IdAndDesign_Id(user.getId(), designId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(String email, Long designId) {
        AppUser user = findUser(email);
        return wishlistRepository.existsByUser_IdAndDesign_Id(user.getId(), designId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countWishlist(String email) {
        AppUser user = findUser(email);
        return wishlistRepository.countByUser_Id(user.getId());
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + email));
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        Design d = item.getDesign();
        return new WishlistItemResponse(
                item.getId(),
                d.getId(),
                d.getName(),
                d.getSlug(),
                d.getMainImageUrl(),
                d.getCollection().getName(),
                d.getCollection().getCatalogGroup().getSlug(),
                item.getAddedAt()
        );
    }
}
