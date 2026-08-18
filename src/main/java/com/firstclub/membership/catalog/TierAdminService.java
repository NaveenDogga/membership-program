package com.firstclub.membership.catalog;

import com.firstclub.membership.api.dto.AdminDtos;
import com.firstclub.membership.benefit.BenefitCatalogService;
import com.firstclub.membership.common.exception.ResourceNotFoundException;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.model.TierBenefit;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.domain.repository.TierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TierAdminService {

    private final TierRepository tierRepository;
    private final BenefitCatalogService benefitCatalogService;

    @Transactional
    public Tier upsertBenefit(String tierCode, AdminDtos.UpsertBenefitRequest request) {
        Tier tier = requireTier(tierCode);

        Optional<TierBenefit> existing = tier.getBenefits().stream()
                .filter(benefit -> benefit.getBenefitType() == request.type())
                .findFirst();

        if (existing.isPresent()) {
            TierBenefit benefit = existing.get();
            benefit.setDescription(request.description());
            benefit.setConfig(new HashMap<>(request.configOrEmpty()));
            benefit.setActive(request.activeOrTrue());
        } else {
            tier.addBenefit(TierBenefit.builder()
                    .benefitType(request.type())
                    .description(request.description())
                    .config(new HashMap<>(request.configOrEmpty()))
                    .active(request.activeOrTrue())
                    .build());
        }

        Tier saved = tierRepository.saveAndFlush(tier);
        benefitCatalogService.refreshAfterCommit();
        return saved;
    }

    @Transactional
    public Tier upsertCriterion(String tierCode, AdminDtos.UpsertCriterionRequest request) {
        Tier tier = requireTier(tierCode);

        Optional<TierCriterion> existing = tier.getCriteria().stream()
                .filter(criterion -> criterion.getCriterionType() == request.type())
                .findFirst();

        if (existing.isPresent()) {
            TierCriterion criterion = existing.get();
            criterion.setDescription(request.description());
            criterion.setConfig(new HashMap<>(request.configOrEmpty()));
            criterion.setActive(request.activeOrTrue());
        } else {
            tier.addCriterion(TierCriterion.builder()
                    .criterionType(request.type())
                    .description(request.description())
                    .config(new HashMap<>(request.configOrEmpty()))
                    .active(request.activeOrTrue())
                    .build());
        }

        return tierRepository.saveAndFlush(tier);
    }

    private Tier requireTier(String tierCode) {
        return tierRepository.findByCodeIgnoreCase(tierCode)
                .orElseThrow(() -> ResourceNotFoundException.of("Tier", tierCode));
    }
}
