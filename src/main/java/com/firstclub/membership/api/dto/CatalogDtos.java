package com.firstclub.membership.api.dto;

import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.enums.BillingCycle;
import com.firstclub.membership.domain.enums.CriteriaMatchPolicy;
import com.firstclub.membership.domain.enums.CriterionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record PlanResponse(
            String code,
            String name,
            BillingCycle billingCycle,
            int durationMonths,
            List<OfferingResponse> tierOptions
    ) {
    }

    public record OfferingResponse(
            String tierCode,
            String tierName,
            int rank,
            BigDecimal price,
            String currency
    ) {
    }

    public record TierResponse(
            String code,
            String name,
            int rank,
            CriteriaMatchPolicy criteriaMatchPolicy,
            boolean inheritsLowerTierBenefits,
            List<BenefitResponse> benefits,
            List<CriterionResponse> unlockCriteria
    ) {
    }

    public record BenefitResponse(
            BenefitType type,
            String description,
            String sourceTierCode,
            Map<String, String> config
    ) {
    }

    public record CriterionResponse(
            CriterionType type,
            String description,
            Map<String, String> config
    ) {
    }
}
