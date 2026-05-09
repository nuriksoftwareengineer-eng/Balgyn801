package com.nurba.java.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequest {
    private Long id;
    private String name;
    private String phone;
    private String telegramUsername;
    private LocalDateTime createAt;

}
