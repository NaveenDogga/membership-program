package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.BenefitType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "tier_benefits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierBenefit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tier_id", nullable = false)
    private Tier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_type", nullable = false, length = 40)
    private BenefitType benefitType;

    @Column(nullable = false)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tier_benefit_config", joinColumns = @JoinColumn(name = "benefit_id"))
    @MapKeyColumn(name = "config_key", length = 60)
    @Column(name = "config_value", length = 500)
    @Builder.Default
    private Map<String, String> config = new HashMap<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public String config(String key, String defaultValue) {
        String value = config.get(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public BigDecimal decimalConfig(String key, BigDecimal defaultValue) {
        String value = config.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return new BigDecimal(value.trim());
    }

    public int intConfig(String key, int defaultValue) {
        String value = config.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }
}
