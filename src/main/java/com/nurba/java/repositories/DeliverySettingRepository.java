package com.nurba.java.repositories;

import com.nurba.java.domain.DeliverySetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliverySettingRepository extends JpaRepository<DeliverySetting, String> {
}
