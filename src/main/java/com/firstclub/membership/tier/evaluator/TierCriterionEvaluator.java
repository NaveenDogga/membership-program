package com.firstclub.membership.tier.evaluator;

import com.firstclub.membership.domain.enums.CriterionType;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.tier.UserActivity;

import java.time.Instant;

public interface TierCriterionEvaluator {

    CriterionType supportedType();

    boolean isSatisfied(TierCriterion criterion, UserActivity activity, Instant evaluatedAt);

    String describeProgress(TierCriterion criterion, UserActivity activity, Instant evaluatedAt);
}
