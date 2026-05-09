package com.nurba.java.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddressRequest {
    private String city;
    private String street;
    private String apartment;
    private String postalCode;
    private String recipientName;
    private String recipientPhone;
}
