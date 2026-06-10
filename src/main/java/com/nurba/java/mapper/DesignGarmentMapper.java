package com.nurba.java.mapper;

import com.nurba.java.domain.DesignGarment;
import com.nurba.java.dto.request.CreateDesignGarmentRequest;
import com.nurba.java.dto.responce.DesignGarmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ColorMapper.class, SizeMapper.class, DesignGarmentPriceMapper.class})
public interface DesignGarmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "design", ignore = true)
    @Mapping(target = "prices", ignore = true)
    @Mapping(target = "colors", ignore = true)
    @Mapping(target = "sizes", ignore = true)
    DesignGarment toEntity(CreateDesignGarmentRequest request);

    @Mapping(source = "design.id", target = "designId")
    @Mapping(source = "design.name", target = "designName")
    @Mapping(source = "prices", target = "prices")
    @Mapping(source = "colors", target = "colors")
    @Mapping(source = "sizes", target = "sizes")
    DesignGarmentResponse toResponse(DesignGarment entity);
}
