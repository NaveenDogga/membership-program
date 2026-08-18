package com.firstclub.membership.tier;

import com.firstclub.membership.common.exception.BusinessRuleException;
import com.firstclub.membership.domain.enums.CriteriaMatchPolicy;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.domain.model.User;
import com.firstclub.membership.domain.repository.TierRepository;
import com.firstclub.membership.tier.evaluator.TierCriterionEvaluator;
import com.firstclub.membership.tier.evaluator.TierCriterionEvaluatorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TierEligibilityService {

    private final TierRepository tierRepository;
    private final TierCriterionEvaluatorRegistry evaluatorRegistry;
    private final UserActivityProvider userActivityProvider;

    @Transactional(readOnly = true)
    public List<TierEligibility> evaluateAllTiers(User user) {
        Instant now = Instant.now();
        UserActivity activity = userActivityProvider.forUser(user);
        return tierRepository.findByActiveTrueOrderByRankAsc().stream()
                .map(tier -> evaluate(tier, activity, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public TierEligibility evaluateTier(User user, Tier tier) {
        return evaluate(tier, userActivityProvider.forUser(user), Instant.now());
    }

    @Transactional(readOnly = true)
    public Optional<Tier> highestEligibleTier(User user) {
        Instant now = Instant.now();
        UserActivity activity = userActivityProvider.forUser(user);
        return tierRepository.findByActiveTrueOrderByRankAsc().stream()
                .filter(tier -> evaluate(tier, activity, now).eligible())
                .max(Comparator.comparingInt(Tier::getRank));
    }

    public void assertEligible(User user, Tier tier) {
        TierEligibility eligibility = evaluateTier(user, tier);
        if (!eligibility.eligible()) {
            String unmet = eligibility.criteria().stream()
                    .filter(outcome -> !outcome.satisfied())
                    .map(CriterionOutcome::progress)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("criteria not met");
            throw new BusinessRuleException("TIER_NOT_UNLOCKED",
                    "User has not unlocked tier " + tier.getCode() + " (" + unmet + ")");
        }
    }

    private TierEligibility evaluate(Tier tier, UserActivity activity, Instant now) {
        List<TierCriterion> activeCriteria = tier.getCriteria().stream()
                .filter(TierCriterion::isActive)
                .toList();

        List<CriterionOutcome> outcomes = new ArrayList<>(activeCriteria.size());
        for (TierCriterion criterion : activeCriteria) {
            TierCriterionEvaluator evaluator = evaluatorRegistry.resolve(criterion.getCriterionType());
            outcomes.add(new CriterionOutcome(
                    criterion.getCriterionType(),
                    criterion.getDescription(),
                    evaluator.isSatisfied(criterion, activity, now),
                    evaluator.describeProgress(criterion, activity, now)));
        }

        boolean eligible = outcomes.isEmpty()
                || (tier.getCriteriaMatchPolicy() == CriteriaMatchPolicy.ALL
                        ? outcomes.stream().allMatch(CriterionOutcome::satisfied)
                        : outcomes.stream().anyMatch(CriterionOutcome::satisfied));

        return new TierEligibility(tier.getCode(), tier.getName(), tier.getRank(), eligible,
                tier.getCriteriaMatchPolicy(), outcomes);
    }
}
