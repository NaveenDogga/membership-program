package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.CriteriaMatchPolicy;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "tiers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tier extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "tier_rank", nullable = false, unique = true)
    private int rank;

    @Enumerated(EnumType.STRING)
    @Column(name = "criteria_match_policy", nullable = false, length = 10)
    @Builder.Default
    private CriteriaMatchPolicy criteriaMatchPolicy = CriteriaMatchPolicy.ALL;

    @Column(name = "inherits_lower_tier_benefits", nullable = false)
    @Builder.Default
    private boolean inheritsLowerTierBenefits = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "tier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private Set<TierBenefit> benefits = new LinkedHashSet<>();

    @OneToMany(mappedBy = "tier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private Set<TierCriterion> criteria = new LinkedHashSet<>();

    public void addBenefit(TierBenefit benefit) {
        benefit.setTier(this);
        this.benefits.add(benefit);
    }

    public void addCriterion(TierCriterion criterion) {
        criterion.setTier(this);
        this.criteria.add(criterion);
    }
}
