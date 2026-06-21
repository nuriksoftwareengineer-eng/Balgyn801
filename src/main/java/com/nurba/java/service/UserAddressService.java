package com.nurba.java.service;

import com.nurba.java.dto.request.CreateUserAddressRequest;
import com.nurba.java.dto.responce.UserAddressResponse;

import java.util.List;

public interface UserAddressService {

    List<UserAddressResponse> listOwn(String userEmail);

    UserAddressResponse create(CreateUserAddressRequest request, String userEmail);

    void delete(Long id, String userEmail);
}
