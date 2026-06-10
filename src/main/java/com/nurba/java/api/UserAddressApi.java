package com.nurba.java.api;

import com.nurba.java.dto.request.CreateUserAddressRequest;
import com.nurba.java.dto.responce.UserAddressResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Me / Addresses", description = "Saved delivery addresses for authenticated users")
@RequestMapping("/api/v1/me/addresses")
public interface UserAddressApi {

    @Operation(summary = "List own saved addresses")
    @GetMapping
    List<UserAddressResponse> list(@AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "Save a new delivery address")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserAddressResponse create(@Valid @RequestBody CreateUserAddressRequest request,
                               @AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "Delete a saved address")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id,
                @AuthenticationPrincipal UserDetails userDetails);
}
