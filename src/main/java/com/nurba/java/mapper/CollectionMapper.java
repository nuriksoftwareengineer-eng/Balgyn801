package com.nurba.java.mapper;

import com.nurba.java.domain.Collection;
import com.nurba.java.dto.request.CreateCollectionRequest;
import com.nurba.java.dto.responce.CollectionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CollectionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "catalogGroup", ignore = true)
    @Mapping(target = "designs", ignore = true)
    Collection toEntity(CreateCollectionRequest request);

    @Mapping(source = "catalogGroup.id",    target = "groupId")
    @Mapping(source = "catalogGroup.name",  target = "groupName")
    @Mapping(source = "catalogGroup.nameKk", target = "groupNameKk")
    @Mapping(source = "catalogGroup.nameEn", target = "groupNameEn")
    CollectionResponse toResponse(Collection entity);
}
