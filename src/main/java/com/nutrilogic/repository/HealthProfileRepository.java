package com.nutrilogic.repository;

import com.nutrilogic.model.HealthProfile;
import com.nutrilogic.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HealthProfileRepository extends JpaRepository<HealthProfile, Long> {

    // Used by history page — oldest first (for chart)
    List<HealthProfile> findByUserOrderByCreatedAtAsc(User user);

    // Used by home page — latest record only
    HealthProfile findTopByUserOrderByCreatedAtDesc(User user);

    // Generic (keep for any existing usage)
    List<HealthProfile> findByUser(User user);
}