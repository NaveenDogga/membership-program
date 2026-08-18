package com.firstclub.membership.tier;

import com.firstclub.membership.domain.enums.CriterionType;

public record CriterionOutcome(
        CriterionType type,
        String description,
        boolean satisfied,
        String progress
) {
}
