package com.firstclub.membership.benefit;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutResult(
        Long userId,
        String membershipTier,
        String membershipPlan,
        BigDecimal subtotal,
        BigDecimal itemDiscount,
        BigDecimal deliveryFee,
        BigDecimal deliveryDiscount,
        BigDecimal totalSavings,
        BigDecimal totalPayable,
        List<AppliedBenefit> appliedBenefits
) {
}
