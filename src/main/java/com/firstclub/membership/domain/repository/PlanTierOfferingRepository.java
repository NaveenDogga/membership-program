package com.firstclub.membership.domain.repository;

import com.firstclub.membership.domain.model.PlanTierOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanTierOfferingRepository extends JpaRepository<PlanTierOffering, Long> {

    Optional<PlanTierOffering> findByPlanIdAndTierIdAndActiveTrue(Long planId, Long tierId);

    List<PlanTierOffering> findByActiveTrue();

    List<PlanTierOffering> findByPlanIdAndActiveTrue(Long planId);
}
