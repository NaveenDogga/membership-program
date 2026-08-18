package com.firstclub.membership.domain.repository;

import com.firstclub.membership.domain.model.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {

    Optional<MembershipPlan> findByCodeIgnoreCase(String code);

    List<MembershipPlan> findByActiveTrue();
}
