package com.firstclub.membership.tier.evaluator;

import com.firstclub.membership.domain.enums.CriterionType;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.tier.UserActivity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MinLifetimeOrdersEvaluator implements TierCriterionEvaluator {

    private static final String MIN_ORDERS = "minOrders";

    @Override
    public CriterionType supportedType() {
        return CriterionType.MIN_LIFETIME_ORDERS;
    }

    @Override
    public boolean isSatisfied(TierCriterion criterion, UserActivity activity, Instant evaluatedAt) {
        return activity.orderCountSince(Instant.EPOCH) >= criterion.intConfig(MIN_ORDERS, 0);
    }

    @Override
    public String describeProgress(TierCriterion criterion, UserActivity activity, Instant evaluatedAt) {
        return activity.orderCountSince(Instant.EPOCH) + " of " + criterion.intConfig(MIN_ORDERS, 0)
                + " lifetime orders";
    }
}
