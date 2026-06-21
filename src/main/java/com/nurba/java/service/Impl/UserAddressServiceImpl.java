package com.nurba.java.service.Impl;

import com.nurba.java.domain.AppUser;
import com.nurba.java.domain.UserAddress;
import com.nurba.java.dto.request.CreateUserAddressRequest;
import com.nurba.java.dto.responce.UserAddressResponse;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.UserAddressMapper;
import com.nurba.java.repositories.AppUserRepository;
import com.nurba.java.repositories.UserAddressRepository;
import com.nurba.java.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressRepository repository;
    private final AppUserRepository appUserRepository;
    private final UserAddressMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserAddressResponse> listOwn(String userEmail) {
        AppUser user = findUser(userEmail);
        return repository.findByAppUser_IdOrderByCreatedAtDesc(user.getId())
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public UserAddressResponse create(CreateUserAddressRequest request, String userEmail) {
        AppUser user = findUser(userEmail);

        UserAddress address = new UserAddress();
        address.setAppUser(user);
        address.setLabel(request.getLabel());
        address.setCity(request.getCity());
        address.setStreet(request.getStreet());
        address.setApartment(request.getApartment());
        address.setPostalCode(request.getPostalCode());
        address.setRecipientName(request.getRecipientName());
        address.setRecipientPhone(request.getRecipientPhone());
        address.setCreatedAt(LocalDateTime.now());

        return mapper.toResponse(repository.save(address));
    }

    @Override
    @Transactional
    public void delete(Long id, String userEmail) {
        UserAddress address = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Адрес не найден: " + id));
        if (!address.getAppUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new BusinessRuleException("Нельзя удалить чужой адрес");
        }
        repository.deleteById(id);
    }

    private AppUser findUser(String email) {
        return appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }
}
