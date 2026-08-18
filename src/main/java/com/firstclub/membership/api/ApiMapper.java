package com.firstclub.membership.api;

import com.firstclub.membership.api.dto.CatalogDtos;
import com.firstclub.membership.api.dto.SubscriptionDtos;
import com.firstclub.membership.benefit.BenefitCatalogService;
import com.firstclub.membership.benefit.EffectiveBenefit;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.PlanTierOffering;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.domain.model.SubscriptionEvent;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.model.TierCriterion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApiMapper {

    private final BenefitCatalogService benefitCatalogService;

    public CatalogDtos.PlanResponse toPlanResponse(MembershipPlan plan, List<PlanTierOffering> offerings) {
        return new CatalogDtos.PlanResponse(
                plan.getCode(),
                plan.getName(),
                plan.getBillingCycle(),
                plan.getBillingCycle().getPeriod().toTotalMonths() > 0
                        ? (int) plan.getBillingCycle().getPeriod().toTotalMonths()
                        : 0,
                offerings.stream().map(this::toOfferingResponse).toList());
    }

    public CatalogDtos.OfferingResponse toOfferingResponse(PlanTierOffering offering) {
        Tier tier = offering.getTier();
        return new CatalogDtos.OfferingResponse(
                tier.getCode(), tier.getName(), tier.getRank(), offering.getPrice(), offering.getCurrency());
    }

    public CatalogDtos.TierResponse toTierResponse(Tier tier) {
        List<CatalogDtos.BenefitResponse> benefits = benefitCatalogService.benefitsForTier(tier.getCode())
                .stream()
                .map(this::toBenefitResponse)
                .toList();

        List<CatalogDtos.CriterionResponse> criteria = tier.getCriteria().stream()
                .filter(TierCriterion::isActive)
                .map(criterion -> new CatalogDtos.CriterionResponse(
                        criterion.getCriterionType(),
                        criterion.getDescription(),
                        Map.copyOf(criterion.getConfig())))
                .toList();

        return new CatalogDtos.TierResponse(
                tier.getCode(), tier.getName(), tier.getRank(), tier.getCriteriaMatchPolicy(),
                tier.isInheritsLowerTierBenefits(), benefits, criteria);
    }

    public CatalogDtos.BenefitResponse toBenefitResponse(EffectiveBenefit benefit) {
        return new CatalogDtos.BenefitResponse(
                benefit.type(), benefit.description(), benefit.sourceTierCode(), benefit.config());
    }

    public SubscriptionDtos.SubscriptionResponse toSubscriptionResponse(Subscription subscription) {
        Instant now = Instant.now();
        long daysRemaining = subscription.getEndAt().isAfter(now)
                ? Duration.between(now, subscription.getEndAt()).toDays()
                : 0L;

        return new SubscriptionDtos.SubscriptionResponse(
                subscription.getId(),
                subscription.getUser().getId(),
                subscription.getPlan().getCode(),
                subscription.getPlan().getBillingCycle(),
                subscription.getTier().getCode(),
                subscription.getTier().getName(),
                subscription.getStatus(),
                subscription.getStartAt(),
                subscription.getEndAt(),
                daysRemaining,
                subscription.isAutoRenew(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getPendingTier() == null ? null : subscription.getPendingTier().getCode(),
                subscription.getPendingTierEffectiveAt(),
                subscription.getAmountPaid(),
                subscription.getCurrency());
    }

    public SubscriptionDtos.SubscriptionEventResponse toEventResponse(SubscriptionEvent event) {
        return new SubscriptionDtos.SubscriptionEventResponse(
                event.getEventType(), event.getFromTierCode(), event.getToTierCode(),
                event.getAmount(), event.getNote(), event.getOccurredAt());
    }
}
