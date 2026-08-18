package com.firstclub.membership.api.dto;

import com.firstclub.membership.domain.enums.BillingCycle;
import com.firstclub.membership.domain.enums.SubscriptionEventType;
import com.firstclub.membership.domain.enums.SubscriptionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public final class SubscriptionDtos {

    private SubscriptionDtos() {
    }

    public record SubscribeRequest(
            @NotNull(message = "userId is required") Long userId,
            @NotBlank(message = "planCode is required") String planCode,
            @NotBlank(message = "tierCode is required") String tierCode
    ) {
    }

    public record TierChangeRequest(
            @NotBlank(message = "tierCode is required") String tierCode
    ) {
    }

    public record CancelRequest(boolean immediate) {
    }

    public record SubscriptionResponse(
            Long subscriptionId,
            Long userId,
            String planCode,
            BillingCycle billingCycle,
            String tierCode,
            String tierName,
            SubscriptionStatus status,
            Instant startAt,
            Instant endAt,
            long daysRemaining,
            boolean autoRenew,
            boolean cancelAtPeriodEnd,
            String pendingTierCode,
            Instant pendingTierEffectiveAt,
            BigDecimal amountPaid,
            String currency
    ) {
    }

    public record SubscriptionEventResponse(
            SubscriptionEventType eventType,
            String fromTierCode,
            String toTierCode,
            BigDecimal amount,
            String note,
            Instant occurredAt
    ) {
    }
}
