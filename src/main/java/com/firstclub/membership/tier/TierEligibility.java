package com.firstclub.membership.tier;

import com.firstclub.membership.domain.enums.CriteriaMatchPolicy;

import java.util.List;

public record TierEligibility(
        String tierCode,
        String tierName,
        int rank,
        boolean eligible,
        CriteriaMatchPolicy matchPolicy,
        List<CriterionOutcome> criteria
) {
}
