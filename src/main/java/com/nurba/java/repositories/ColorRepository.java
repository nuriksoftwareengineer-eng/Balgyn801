package com.nurba.java.repositories;

import com.nurba.java.domain.Color;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ColorRepository extends JpaRepository<Color, Long> {
    List<Color> findAllByOrderBySortOrderAsc();
}
