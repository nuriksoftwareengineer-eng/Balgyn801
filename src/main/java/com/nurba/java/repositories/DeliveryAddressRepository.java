package com.nurba.java.repositories;

import com.nurba.java.domain.CustomDesign;
import com.nurba.java.domain.Customer;
import com.nurba.java.domain.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {
}
