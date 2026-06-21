package com.nurba.java.mapper;

import com.nurba.java.domain.Color;
import com.nurba.java.dto.request.CreateColorRequest;
import com.nurba.java.dto.responce.ColorResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ColorMapper {

    @Mapping(target = "id", ignore = true)
    Color toEntity(CreateColorRequest request);

    ColorResponse toResponse(Color entity);
}
