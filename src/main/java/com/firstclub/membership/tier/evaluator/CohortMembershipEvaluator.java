package com.firstclub.membership.tier.evaluator;

import com.firstclub.membership.domain.enums.CriterionType;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.tier.UserActivity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CohortMembershipEvaluator implements TierCriterionEvaluator {

    private static final String COHORTS = "cohorts";

    @Override
    public CriterionType supportedType() {
        return CriterionType.COHORT_MEMBERSHIP;
    }

    @Override
    public boolean isSatisfied(TierCriterion criterion, UserActivity activity, Instant evaluatedAt) {
        Set<String> required = requiredCohorts(criterion);
        if (required.isEmpty()) {
            return false;
        }
        return activity.cohorts().stream()
                .map(cohort -> cohort.toUpperCase(Locale.ROOT))
                .anyMatch(required::contains);
    }

    @Override
    public String describeProgress(TierCriterion criterion, UserActivity activity, Instant evaluatedAt) {
        return "member of " + activity.cohorts() + "; qualifying cohorts " + requiredCohorts(criterion);
    }

    private Set<String> requiredCohorts(TierCriterion criterion) {
        return Arrays.stream(criterion.config(COHORTS, "").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
