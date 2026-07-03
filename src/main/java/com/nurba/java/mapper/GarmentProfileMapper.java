package com.nurba.java.mapper;

import com.nurba.java.domain.GarmentProfile;
import com.nurba.java.dto.request.CreateGarmentProfileRequest;
import com.nurba.java.dto.responce.GarmentProfileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface GarmentProfileMapper {

    @Mapping(target = "id", ignore = true)
    GarmentProfile toEntity(CreateGarmentProfileRequest request);

    GarmentProfileResponse toResponse(GarmentProfile entity);

    @Mapping(target = "id", ignore = true)
    void updateEntity(CreateGarmentProfileRequest request, @MappingTarget GarmentProfile entity);
}
