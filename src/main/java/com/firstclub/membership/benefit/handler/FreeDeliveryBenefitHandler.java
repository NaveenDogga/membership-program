package com.firstclub.membership.benefit.handler;

import com.firstclub.membership.benefit.CheckoutComputation;
import com.firstclub.membership.benefit.CheckoutContext;
import com.firstclub.membership.benefit.EffectiveBenefit;
import com.firstclub.membership.domain.enums.BenefitType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
public class FreeDeliveryBenefitHandler implements BenefitHandler {

    @Override
    public Set<BenefitType> supportedTypes() {
        return Set.of(BenefitType.FREE_DELIVERY);
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void apply(EffectiveBenefit benefit, CheckoutContext context, CheckoutComputation computation) {
        BigDecimal minOrderValue = benefit.decimal("minOrderValue", BigDecimal.ZERO);
        if (computation.discountedSubtotal().compareTo(minOrderValue) < 0) {
            return;
        }
        computation.waiveDelivery(BenefitType.FREE_DELIVERY, benefit.sourceTierCode(),
                minOrderValue.signum() > 0
                        ? "Free delivery on orders above " + minOrderValue.toPlainString()
                        : "Free delivery on all eligible orders");
    }
}
