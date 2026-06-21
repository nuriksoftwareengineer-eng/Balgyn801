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
public class ReviewResponse {

    private Long          id;
    private Long          designId;
    private String        designName;
    private String        authorEmail;
    private Integer       rating;
    private String        comment;
    private LocalDateTime createdAt;
}
