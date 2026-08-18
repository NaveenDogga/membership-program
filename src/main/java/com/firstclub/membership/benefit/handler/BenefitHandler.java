package com.firstclub.membership.benefit.handler;

import com.firstclub.membership.benefit.CheckoutComputation;
import com.firstclub.membership.benefit.CheckoutContext;
import com.firstclub.membership.benefit.EffectiveBenefit;
import com.firstclub.membership.domain.enums.BenefitType;

import java.util.Set;

public interface BenefitHandler {

    Set<BenefitType> supportedTypes();

    default int order() {
        return 100;
    }

    void apply(EffectiveBenefit benefit, CheckoutContext context, CheckoutComputation computation);
}
