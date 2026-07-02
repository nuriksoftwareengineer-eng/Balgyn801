package com.nurba.java.repositories;

import com.nurba.java.domain.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM AppUser u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<AppUser> searchByEmail(@Param("q") String q, Pageable pageable);
}
