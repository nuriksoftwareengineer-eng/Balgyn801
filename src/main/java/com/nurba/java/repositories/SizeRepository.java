package com.nurba.java.repositories;

import com.nurba.java.domain.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SizeRepository extends JpaRepository<Size, Long> {
    List<Size> findAllByOrderBySortOrderAsc();
}
