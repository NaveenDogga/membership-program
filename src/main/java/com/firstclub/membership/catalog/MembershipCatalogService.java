package com.firstclub.membership.catalog;

import com.firstclub.membership.common.exception.ResourceNotFoundException;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.PlanTierOffering;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.repository.MembershipPlanRepository;
import com.firstclub.membership.domain.repository.PlanTierOfferingRepository;
import com.firstclub.membership.domain.repository.TierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembershipCatalogService {

    private final MembershipPlanRepository planRepository;
    private final TierRepository tierRepository;
    private final PlanTierOfferingRepository offeringRepository;

    @Transactional(readOnly = true)
    public List<MembershipPlan> activePlans() {
        return planRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Tier> activeTiers() {
        return tierRepository.findByActiveTrueOrderByRankAsc();
    }

    @Transactional(readOnly = true)
    public Tier requireTier(String code) {
        return tierRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> ResourceNotFoundException.of("Tier", code));
    }

    @Transactional(readOnly = true)
    public Map<Long, List<PlanTierOffering>> offeringsByPlan() {
        return offeringRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparingInt(offering -> offering.getTier().getRank()))
                .collect(Collectors.groupingBy(offering -> offering.getPlan().getId(),
                        Collectors.toList()));
    }
}
