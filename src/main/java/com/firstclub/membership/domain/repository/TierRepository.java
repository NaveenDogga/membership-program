package com.firstclub.membership.domain.repository;

import com.firstclub.membership.domain.model.Tier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TierRepository extends JpaRepository<Tier, Long> {

    Optional<Tier> findByCodeIgnoreCase(String code);

    List<Tier> findByActiveTrueOrderByRankAsc();

    List<Tier> findAllByOrderByRankAsc();
}
