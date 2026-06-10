package com.nurba.java.dto.responce;

import lombok.Data;

@Data
public class CollectionResponse {
    private Long id;
    private Long groupId;
    private String groupName;
    private String name;
    private String slug;
    private Integer sortOrder;
    private Boolean active;
}
