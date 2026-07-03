package com.nurba.java.repositories;

import com.nurba.java.domain.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingRepository extends JpaRepository<SiteSetting, String> {
}
