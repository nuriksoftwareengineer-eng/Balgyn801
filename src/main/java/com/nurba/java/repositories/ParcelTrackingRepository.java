package com.nurba.java.repositories;

import com.nurba.java.domain.ParcelTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParcelTrackingRepository extends JpaRepository<ParcelTracking, Long> {

    List<ParcelTracking> findByOrder_Id(Long orderId);

    Optional<ParcelTracking> findByOrder_IdAndCarrierAndTrackingNumber(
            Long orderId, String carrier, String trackingNumber);
}
