package com.nurba.java.dto.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddressResponse {

    private Long          id;
    private String        label;
    private String        city;
    private String        street;
    private String        apartment;
    private String        postalCode;
    private String        recipientName;
    private String        recipientPhone;
    private LocalDateTime createdAt;
}
