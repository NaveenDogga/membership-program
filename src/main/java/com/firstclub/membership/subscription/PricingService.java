package com.firstclub.membership.subscription;

import com.firstclub.membership.common.Money;
import com.firstclub.membership.domain.model.PlanTierOffering;
import com.firstclub.membership.domain.model.Subscription;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Service
public class PricingService {

    public BigDecimal upgradeCharge(Subscription current, PlanTierOffering targetOffering, Instant now) {
        BigDecimal remainingRatio = remainingRatio(current, now);
        BigDecimal unusedCurrentValue = current.getAmountPaid().multiply(remainingRatio);
        BigDecimal newTierValueForRemainder = targetOffering.getPrice().multiply(remainingRatio);
        return Money.atLeastZero(newTierValueForRemainder.subtract(unusedCurrentValue));
    }

    public BigDecimal remainingRatio(Subscription subscription, Instant now) {
        long totalSeconds = Duration.between(subscription.getStartAt(), subscription.getEndAt()).getSeconds();
        if (totalSeconds <= 0) {
            return BigDecimal.ZERO;
        }
        long remainingSeconds = Duration.between(now, subscription.getEndAt()).getSeconds();
        if (remainingSeconds <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal ratio = BigDecimal.valueOf(remainingSeconds)
                .divide(BigDecimal.valueOf(totalSeconds), 8, RoundingMode.HALF_UP);
        return ratio.min(BigDecimal.ONE);
    }
}
