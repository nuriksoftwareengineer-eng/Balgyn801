package com.nurba.java.repositories;

import com.nurba.java.domain.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByAppUser_IdOrderByCreatedAtDesc(Long userId);
}
