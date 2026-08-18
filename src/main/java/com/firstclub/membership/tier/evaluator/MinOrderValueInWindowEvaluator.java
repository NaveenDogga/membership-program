package com.firstclub.membership.tier.evaluator;

import com.firstclub.membership.domain.enums.CriterionType;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.tier.UserActivity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Component
public class MinOrderValueInWindowEvaluator implements TierCriterionEvaluator {

    private static final String MIN_VALUE = "minValue";
    private static final String WINDOW_DAYS = "windowDays";
    private static final int DEFAULT_WINDOW_DAYS = 30;

    @Override
    public CriterionType supportedType() {
        return CriterionType.MIN_ORDER_VALUE_IN_WINDOW;
    }

    @Override
    public boolean isSatisfied(TierCriterion criterion, UserActivity activity, Instant evaluatedAt) {
        BigDecimal spent = activity.orderValueSince(windowStart(criterion, evaluatedAt));
        return spent.compareTo(criterion.decimalConfig(MIN_VALUE, BigDecimal.ZERO)) >= 0;
    }

    @Override
    public String describeProgress(TierCriterion criterion, UserActivity activity, Instant evaluatedAt) {
        BigDecimal spent = activity.orderValueSince(windowStart(criterion, evaluatedAt));
        return spent.toPlainString() + " of " + criterion.decimalConfig(MIN_VALUE, BigDecimal.ZERO).toPlainString()
                + " spent in the last " + criterion.intConfig(WINDOW_DAYS, DEFAULT_WINDOW_DAYS) + " days";
    }

    private Instant windowStart(TierCriterion criterion, Instant evaluatedAt) {
        return evaluatedAt.minus(Duration.ofDays(criterion.intConfig(WINDOW_DAYS, DEFAULT_WINDOW_DAYS)));
    }
}
