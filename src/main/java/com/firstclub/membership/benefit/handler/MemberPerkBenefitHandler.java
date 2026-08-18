package com.firstclub.membership.benefit.handler;

import com.firstclub.membership.benefit.CheckoutComputation;
import com.firstclub.membership.benefit.CheckoutContext;
import com.firstclub.membership.benefit.EffectiveBenefit;
import com.firstclub.membership.domain.enums.BenefitType;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class MemberPerkBenefitHandler implements BenefitHandler {

    @Override
    public Set<BenefitType> supportedTypes() {
        return Set.of(
                BenefitType.FASTER_DELIVERY,
                BenefitType.EXCLUSIVE_DEALS,
                BenefitType.EARLY_ACCESS,
                BenefitType.PRIORITY_SUPPORT,
                BenefitType.EXCLUSIVE_COUPONS);
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public void apply(EffectiveBenefit benefit, CheckoutContext context, CheckoutComputation computation) {
        computation.addPerk(benefit.type(), benefit.sourceTierCode(), describe(benefit));
    }

    private String describe(EffectiveBenefit benefit) {
        return switch (benefit.type()) {
            case FASTER_DELIVERY -> benefit.description()
                    + " (" + benefit.integer("slaHours", 48) + "h delivery SLA)";
            case EARLY_ACCESS -> benefit.description()
                    + " (" + benefit.integer("hoursBeforePublic", 24) + "h before public)";
            case PRIORITY_SUPPORT -> benefit.description()
                    + " (" + benefit.string("queue", "PRIORITY") + " queue)";
            default -> benefit.description();
        };
    }
}
