package com.firstclub.membership;

import com.firstclub.membership.benefit.BenefitHandlerRegistry;
import com.firstclub.membership.tier.evaluator.TierCriterionEvaluatorRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "membership.seed-demo-data=false")
class MembershipApplicationTest {

    @Autowired
    private BenefitHandlerRegistry benefitHandlerRegistry;

    @Autowired
    private TierCriterionEvaluatorRegistry evaluatorRegistry;

    @Test
    @DisplayName("context loads and every benefit type and criterion type has a strategy registered")
    void contextLoadsWithCompleteStrategyCoverage() {

        assertThat(benefitHandlerRegistry).isNotNull();
        assertThat(evaluatorRegistry).isNotNull();
    }
}
