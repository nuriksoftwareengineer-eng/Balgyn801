package com.nurba.java.mapper;

import com.nurba.java.domain.Design;
import com.nurba.java.dto.request.CreateDesignRequest;
import com.nurba.java.dto.responce.DesignResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DesignMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
    @Mapping(target = "sortOrder", ignore = true)
    @Mapping(target = "collection", ignore = true)
    @Mapping(target = "garments", ignore = true)
    @Mapping(target = "status", ignore = true)
    Design toEntity(CreateDesignRequest request);

    @Mapping(source = "collection.id", target = "collectionId")
    @Mapping(source = "collection.name", target = "collectionName")
    @Mapping(source = "collection.slug", target = "collectionSlug")
    @Mapping(source = "collection.catalogGroup.name", target = "groupName")
    @Mapping(source = "collection.catalogGroup.slug", target = "groupSlug")
    @Mapping(target = "activeGarmentCount", ignore = true)
    DesignResponse toResponse(Design entity);
}
