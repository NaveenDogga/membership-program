package com.firstclub.membership.benefit;

import com.firstclub.membership.domain.enums.BenefitType;

import java.math.BigDecimal;
import java.util.Map;

public record EffectiveBenefit(
        BenefitType type,
        String description,
        String sourceTierCode,
        Map<String, String> config
) {

    public String string(String key, String defaultValue) {
        String value = config.get(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    public BigDecimal decimal(String key, BigDecimal defaultValue) {
        String value = config.get(key);
        return value == null || value.isBlank() ? defaultValue : new BigDecimal(value.trim());
    }

    public int integer(String key, int defaultValue) {
        String value = config.get(key);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value.trim());
    }
}
