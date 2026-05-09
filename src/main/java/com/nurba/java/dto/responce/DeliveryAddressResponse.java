package com.nurba.java.dto.responce;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddressResponse {
    private String city;
    private String street;
    private String apartment;
    private String postalCode;
    private String recipientName;
    private String recipientPhone;
}
