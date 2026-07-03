package com.nurba.java.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "parcel_trackings",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_parcel_trackings_order_carrier",
        columnNames = {"order_id", "carrier", "tracking_number"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParcelTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 50)
    private String carrier;

    @Column(name = "tracking_number", nullable = false, length = 100)
    private String trackingNumber;

    @Column(name = "last_status", length = 50)
    private String lastStatus;

    @Column(name = "status_detail", columnDefinition = "TEXT")
    private String statusDetail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<TrackingEvent> events = new ArrayList<>();

    @Column(nullable = false, length = 50)
    private String provider = "17TRACK";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingEvent {
        private String timestamp;
        private String location;
        private String description;
        private String status;
    }
}
