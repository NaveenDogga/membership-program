package com.firstclub.membership.tier;

import com.firstclub.membership.domain.enums.CriterionType;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.tier.evaluator.CohortMembershipEvaluator;
import com.firstclub.membership.tier.evaluator.MinOrderValueInWindowEvaluator;
import com.firstclub.membership.tier.evaluator.MinOrdersInWindowEvaluator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class TierCriterionEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-03-15T10:00:00Z");

    @Test
    @DisplayName("order-count criterion only counts orders inside the configured window")
    void ordersInWindow() {
        MinOrdersInWindowEvaluator evaluator = new MinOrdersInWindowEvaluator();
        TierCriterion criterion = criterion(CriterionType.MIN_ORDERS_IN_WINDOW,
                Map.of("minOrders", "3", "windowDays", "30"));

        StubActivity fewOrders = new StubActivity(2, BigDecimal.ZERO, Set.of());
        StubActivity enoughOrders = new StubActivity(3, BigDecimal.ZERO, Set.of());

        assertThat(evaluator.isSatisfied(criterion, fewOrders, NOW)).isFalse();
        assertThat(evaluator.isSatisfied(criterion, enoughOrders, NOW)).isTrue();
        assertThat(fewOrders.lastWindowStart).isEqualTo(NOW.minus(Duration.ofDays(30)));
    }

    @Test
    @DisplayName("spend criterion compares on value, boundary inclusive")
    void spendInWindow() {
        MinOrderValueInWindowEvaluator evaluator = new MinOrderValueInWindowEvaluator();
        TierCriterion criterion = criterion(CriterionType.MIN_ORDER_VALUE_IN_WINDOW,
                Map.of("minValue", "5000", "windowDays", "30"));

        assertThat(evaluator.isSatisfied(criterion, new StubActivity(0, new BigDecimal("4999.99"), Set.of()), NOW))
                .isFalse();
        assertThat(evaluator.isSatisfied(criterion, new StubActivity(0, new BigDecimal("5000.00"), Set.of()), NOW))
                .isTrue();
    }

    @Test
    @DisplayName("cohort criterion is case-insensitive and needs only one match")
    void cohortMembership() {
        CohortMembershipEvaluator evaluator = new CohortMembershipEvaluator();
        TierCriterion criterion = criterion(CriterionType.COHORT_MEMBERSHIP,
                Map.of("cohorts", "firstclub_elite, partner_bank"));

        assertThat(evaluator.isSatisfied(criterion, new StubActivity(0, BigDecimal.ZERO, Set.of("FIRSTCLUB_ELITE")), NOW))
                .isTrue();
        assertThat(evaluator.isSatisfied(criterion, new StubActivity(0, BigDecimal.ZERO, Set.of("RANDOM")), NOW))
                .isFalse();
    }

    private TierCriterion criterion(CriterionType type, Map<String, String> config) {
        return TierCriterion.builder()
                .criterionType(type)
                .description(type.name())
                .config(new HashMap<>(config))
                .active(true)
                .build();
    }

    private static final class StubActivity implements UserActivity {

        private final long orderCount;
        private final BigDecimal orderValue;
        private final Set<String> cohorts;
        private Instant lastWindowStart;

        private StubActivity(long orderCount, BigDecimal orderValue, Set<String> cohorts) {
            this.orderCount = orderCount;
            this.orderValue = orderValue;
            this.cohorts = cohorts;
        }

        @Override
        public Long userId() {
            return 1L;
        }

        @Override
        public long orderCountSince(Instant since) {
            this.lastWindowStart = since;
            return orderCount;
        }

        @Override
        public BigDecimal orderValueSince(Instant since) {
            this.lastWindowStart = since;
            return orderValue;
        }

        @Override
        public Set<String> cohorts() {
            return cohorts;
        }
    }
}
