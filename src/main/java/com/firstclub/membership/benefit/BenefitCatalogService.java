package com.firstclub.membership.benefit;

import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.model.TierBenefit;
import com.firstclub.membership.domain.repository.TierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class BenefitCatalogService {

    private final TierRepository tierRepository;
    private final AtomicReference<Map<String, List<EffectiveBenefit>>> catalogue = new AtomicReference<>();

    @Transactional(readOnly = true)
    public List<EffectiveBenefit> benefitsForTier(String tierCode) {
        Map<String, List<EffectiveBenefit>> snapshot = catalogue.get();
        if (snapshot == null) {
            snapshot = buildSnapshot();
            catalogue.compareAndSet(null, snapshot);
            snapshot = catalogue.get();
        }
        return snapshot.getOrDefault(normalize(tierCode), List.of());
    }

    @Transactional(readOnly = true)
    public void refresh() {
        catalogue.set(buildSnapshot());
    }

    public void refreshAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            refresh();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refresh();
            }
        });
    }

    private Map<String, List<EffectiveBenefit>> buildSnapshot() {
        List<Tier> tiers = tierRepository.findAllByOrderByRankAsc();
        Map<String, List<EffectiveBenefit>> snapshot = new LinkedHashMap<>();

        for (Tier tier : tiers) {

            Map<BenefitType, EffectiveBenefit> merged = new LinkedHashMap<>();
            for (Tier candidate : tiers) {
                boolean isSelf = candidate.getRank() == tier.getRank();
                boolean isInherited = tier.isInheritsLowerTierBenefits() && candidate.getRank() < tier.getRank();
                if (!candidate.isActive() || !(isSelf || isInherited)) {
                    continue;
                }
                for (TierBenefit benefit : candidate.getBenefits()) {
                    if (!benefit.isActive()) {
                        continue;
                    }
                    merged.put(benefit.getBenefitType(), new EffectiveBenefit(
                            benefit.getBenefitType(),
                            benefit.getDescription(),
                            candidate.getCode(),
                            Map.copyOf(benefit.getConfig())));
                }
            }
            snapshot.put(normalize(tier.getCode()), List.copyOf(merged.values()));
        }
        return Map.copyOf(snapshot);
    }

    private String normalize(String tierCode) {
        return tierCode == null ? "" : tierCode.toUpperCase(Locale.ROOT);
    }
}
