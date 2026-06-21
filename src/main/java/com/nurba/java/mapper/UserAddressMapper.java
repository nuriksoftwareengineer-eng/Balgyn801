package com.nurba.java.mapper;

import com.nurba.java.domain.UserAddress;
import com.nurba.java.dto.responce.UserAddressResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserAddressMapper {

    UserAddressResponse toResponse(UserAddress address);
}
