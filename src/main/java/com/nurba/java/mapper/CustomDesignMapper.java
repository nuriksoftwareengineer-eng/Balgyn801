package com.nurba.java.mapper;

import com.nurba.java.domain.CustomDesign;
import com.nurba.java.dto.request.CreateCustomDesignRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomDesignMapper {

    CustomDesign toEntity(CreateCustomDesignRequest request);
}