package com.nurba.java.mapper;

import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.dto.request.CreateCatalogGroupRequest;
import com.nurba.java.dto.responce.CatalogGroupResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CatalogGroupMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "collections", ignore = true)
    @Mapping(target = "active", ignore = true)
    CatalogGroup toEntity(CreateCatalogGroupRequest request);

    CatalogGroupResponse toResponse(CatalogGroup entity);
}
