package com.nurba.java.mapper;

import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.dto.request.CreateDesignGarmentPriceRequest;
import com.nurba.java.dto.responce.DesignGarmentPriceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DesignGarmentPriceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "designGarment", ignore = true)
    DesignGarmentPrice toEntity(CreateDesignGarmentPriceRequest request);

    @Mapping(source = "designGarment.id", target = "designGarmentId")
    DesignGarmentPriceResponse toResponse(DesignGarmentPrice entity);
}
