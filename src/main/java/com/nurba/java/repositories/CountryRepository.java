package com.nurba.java.repositories;

import com.nurba.java.domain.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {

    Optional<Country> findByIso2IgnoreCase(String iso2);

    boolean existsByIso2IgnoreCase(String iso2);

    List<Country> findByActiveTrueOrderByNameRuAsc();
}
