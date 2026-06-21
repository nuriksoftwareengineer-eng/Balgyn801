package com.nurba.java.mapper;

import com.nurba.java.domain.Review;
import com.nurba.java.dto.responce.ReviewResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(source = "design.id",    target = "designId")
    @Mapping(source = "design.name",  target = "designName")
    @Mapping(source = "appUser.email",target = "authorEmail")
    ReviewResponse toResponse(Review review);
}
