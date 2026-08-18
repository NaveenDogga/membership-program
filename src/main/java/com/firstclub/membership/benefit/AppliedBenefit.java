package com.firstclub.membership.benefit;

import com.firstclub.membership.domain.enums.BenefitType;

import java.math.BigDecimal;

public record AppliedBenefit(
        BenefitType type,
        String sourceTierCode,
        String description,
        BigDecimal amount
) {
}
