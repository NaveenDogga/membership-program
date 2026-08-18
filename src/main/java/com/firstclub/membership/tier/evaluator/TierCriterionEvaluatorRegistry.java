package com.firstclub.membership.tier.evaluator;

import com.firstclub.membership.domain.enums.CriterionType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class TierCriterionEvaluatorRegistry {

    private final Map<CriterionType, TierCriterionEvaluator> evaluators = new EnumMap<>(CriterionType.class);

    public TierCriterionEvaluatorRegistry(List<TierCriterionEvaluator> discovered) {
        for (TierCriterionEvaluator evaluator : discovered) {
            TierCriterionEvaluator previous = evaluators.put(evaluator.supportedType(), evaluator);
            if (previous != null) {
                throw new IllegalStateException("Duplicate evaluator for criterion type "
                        + evaluator.supportedType() + ": " + previous.getClass() + " and " + evaluator.getClass());
            }
        }
        for (CriterionType type : CriterionType.values()) {
            if (!evaluators.containsKey(type)) {
                throw new IllegalStateException("No TierCriterionEvaluator registered for " + type);
            }
        }
    }

    public TierCriterionEvaluator resolve(CriterionType type) {
        TierCriterionEvaluator evaluator = evaluators.get(type);
        if (evaluator == null) {
            throw new IllegalStateException("No TierCriterionEvaluator registered for " + type);
        }
        return evaluator;
    }
}
