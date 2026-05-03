package com.nurba.java.repositories;

import com.nurba.java.domain.CdekShipment;
import com.nurba.java.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CdekShipmentRepository extends JpaRepository<CdekShipment, Long> {
}
