package com.nurba.java.service.Impl;

import com.nurba.java.domain.AppUser;
import com.nurba.java.domain.Design;
import com.nurba.java.domain.Review;
import com.nurba.java.dto.request.CreateReviewRequest;
import com.nurba.java.dto.responce.ReviewResponse;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.ReviewMapper;
import com.nurba.java.repositories.AppUserRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.repositories.OrderItemRepository;
import com.nurba.java.repositories.ReviewRepository;
import com.nurba.java.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final DesignRepository designRepository;
    private final AppUserRepository appUserRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> listForDesign(Long designId) {
        return reviewRepository.findByDesign_IdOrderByCreatedAtDesc(designId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ReviewResponse create(Long designId, CreateReviewRequest request, String userEmail) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Design design = designRepository.findById(designId)
                .orElseThrow(() -> new NotFoundException("Дизайн не найден: " + designId));

        // One review per user per design (enforced also at DB level via unique constraint)
        if (reviewRepository.existsByDesign_IdAndAppUser_Id(designId, user.getId())) {
            throw new BusinessRuleException("Вы уже оставляли отзыв на этот дизайн");
        }

        // Business rule: user must have a DELIVERED order containing this design
        if (!orderItemRepository.hasPurchasedDesign(user.getId(), designId)) {
            throw new BusinessRuleException(
                    "Вы можете оставить отзыв только после получения заказа с этим дизайном");
        }

        Review review = new Review();
        review.setDesign(design);
        review.setAppUser(user);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCreatedAt(LocalDateTime.now());

        return mapper.toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void deleteAdmin(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new NotFoundException("Отзыв не найден: " + id);
        }
        reviewRepository.deleteById(id);
    }
}
