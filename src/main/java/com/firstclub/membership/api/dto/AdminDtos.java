package com.firstclub.membership.api.dto;

import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.enums.CriterionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record UpsertBenefitRequest(
            @NotNull BenefitType type,
            @NotBlank String description,
            Map<String, String> config,
            Boolean active
    ) {
        public Map<String, String> configOrEmpty() {
            return config == null ? Map.of() : config;
        }

        public boolean activeOrTrue() {
            return active == null || active;
        }
    }

    public record UpsertCriterionRequest(
            @NotNull CriterionType type,
            @NotBlank String description,
            Map<String, String> config,
            Boolean active
    ) {
        public Map<String, String> configOrEmpty() {
            return config == null ? Map.of() : config;
        }

        public boolean activeOrTrue() {
            return active == null || active;
        }
    }
}
