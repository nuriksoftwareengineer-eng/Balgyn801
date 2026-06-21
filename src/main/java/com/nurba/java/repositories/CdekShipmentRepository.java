package com.nurba.java.repositories;

import com.nurba.java.domain.CdekShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CdekShipmentRepository extends JpaRepository<CdekShipment, Long> {

    /** Отправление, привязанное к заказу (OneToOne). */
    Optional<CdekShipment> findByOrder_Id(Long orderId);

    /** Поиск отправления по UUID от CDEK для обработки вебхуков (точечный запрос, не findAll). */
    Optional<CdekShipment> findByCdekOrderUuid(String cdekOrderUuid);
}
