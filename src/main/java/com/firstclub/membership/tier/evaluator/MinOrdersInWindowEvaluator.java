package com.firstclub.membership.tier.evaluator;

import com.firstclub.membership.domain.enums.CriterionType;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.tier.UserActivity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class MinOrdersInWindowEvaluator implements TierCriterionEvaluator {

    private static final String MIN_ORDERS = "minOrders";
    private static final String WINDOW_DAYS = "windowDays";
    private static final int DEFAULT_WINDOW_DAYS = 30;

    @Override
    public CriterionType supportedType() {
        return CriterionType.MIN_ORDERS_IN_WINDOW;
    }

    @Override
    public boolean isSatisfied(TierCriterion criterion, UserActivity activity, Instant evaluatedAt) {
        return activity.orderCountSince(windowStart(criterion, evaluatedAt))
                >= criterion.intConfig(MIN_ORDERS, 0);
    }

    @Override
    public String describeProgress(TierCriterion criterion, UserActivity activity, Instant evaluatedAt) {
        long placed = activity.orderCountSince(windowStart(criterion, evaluatedAt));
        return placed + " of " + criterion.intConfig(MIN_ORDERS, 0) + " orders in the last "
                + criterion.intConfig(WINDOW_DAYS, DEFAULT_WINDOW_DAYS) + " days";
    }

    private Instant windowStart(TierCriterion criterion, Instant evaluatedAt) {
        return evaluatedAt.minus(Duration.ofDays(criterion.intConfig(WINDOW_DAYS, DEFAULT_WINDOW_DAYS)));
    }
}
