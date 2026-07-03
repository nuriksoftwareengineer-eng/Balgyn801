package com.nurba.java.repositories;

import com.nurba.java.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("""
            SELECT c FROM Customer c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<Customer> search(@Param("q") String q, Pageable pageable);
}
