package com.nurba.java.controller;

import com.nurba.java.domain.AppUser;
import com.nurba.java.dto.responce.AdminUserResponse;
import com.nurba.java.dto.responce.PageResponse;
import com.nurba.java.enums.Role;
import com.nurba.java.repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AppUserRepository repository;

    @GetMapping
    public PageResponse<AdminUserResponse> listUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<AppUser> result = search.isBlank()
                ? repository.findAll(pageable)
                : repository.searchByEmail(search, pageable);

        return PageResponse.of(result.map(u -> AdminUserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .roles(u.getRoles().stream().map(Role::name).collect(Collectors.toList()))
                .createdAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : null)
                .build()));
    }
}
