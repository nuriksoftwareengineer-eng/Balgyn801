package com.nurba.java.controller;

import com.nurba.java.api.UserAddressApi;
import com.nurba.java.dto.request.CreateUserAddressRequest;
import com.nurba.java.dto.responce.UserAddressResponse;
import com.nurba.java.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserAddressController implements UserAddressApi {

    private final UserAddressService service;

    @Override
    public List<UserAddressResponse> list(UserDetails userDetails) {
        return service.listOwn(userDetails.getUsername());
    }

    @Override
    public UserAddressResponse create(CreateUserAddressRequest request, UserDetails userDetails) {
        return service.create(request, userDetails.getUsername());
    }

    @Override
    public void delete(Long id, UserDetails userDetails) {
        service.delete(id, userDetails.getUsername());
    }
}
