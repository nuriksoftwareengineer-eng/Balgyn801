package com.nurba.java.mapper;

import com.nurba.java.domain.Size;
import com.nurba.java.dto.request.CreateSizeRequest;
import com.nurba.java.dto.responce.SizeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SizeMapper {

    @Mapping(target = "id", ignore = true)
    Size toEntity(CreateSizeRequest request);

    SizeResponse toResponse(Size entity);
}
