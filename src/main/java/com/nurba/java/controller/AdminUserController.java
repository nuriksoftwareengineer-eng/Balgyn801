package com.nurba.java.controller;

import com.nurba.java.dto.responce.AdminUserResponse;
import com.nurba.java.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin: Users", description = "Управление пользователями (только ADMIN)")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AuthService authService;

    @Operation(
            summary = "Список всех пользователей",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    public List<AdminUserResponse> listUsers() {
        return authService.listUsers();
    }
}
