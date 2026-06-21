package com.nurba.java.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserAddressRequest {

    @NotBlank
    private String label;

    @NotBlank
    private String city;

    @NotBlank
    private String street;

    @NotBlank
    private String apartment;

    @NotBlank
    private String postalCode;

    @NotBlank
    private String recipientName;

    @NotBlank
    private String recipientPhone;
}
