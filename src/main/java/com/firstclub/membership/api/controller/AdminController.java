package com.firstclub.membership.api.controller;

import com.firstclub.membership.api.ApiMapper;
import com.firstclub.membership.api.dto.AdminDtos;
import com.firstclub.membership.api.dto.CatalogDtos;
import com.firstclub.membership.benefit.BenefitCatalogService;
import com.firstclub.membership.catalog.TierAdminService;
import com.firstclub.membership.subscription.MembershipScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Reconfigure tiers and trigger background jobs on demand")
public class AdminController {

    private final TierAdminService tierAdminService;
    private final BenefitCatalogService benefitCatalogService;
    private final MembershipScheduler membershipScheduler;
    private final ApiMapper mapper;

    @PutMapping("/tiers/{tierCode}/benefits")
    @Operation(summary = "Create or update a benefit on a tier, then refresh the live catalogue")
    public ResponseEntity<CatalogDtos.TierResponse> upsertBenefit(
            @PathVariable String tierCode,
            @Valid @RequestBody AdminDtos.UpsertBenefitRequest request) {
        return ResponseEntity.ok(mapper.toTierResponse(tierAdminService.upsertBenefit(tierCode, request)));
    }

    @PutMapping("/tiers/{tierCode}/criteria")
    @Operation(summary = "Create or update a tier unlock criterion")
    public ResponseEntity<CatalogDtos.TierResponse> upsertCriterion(
            @PathVariable String tierCode,
            @Valid @RequestBody AdminDtos.UpsertCriterionRequest request) {
        return ResponseEntity.ok(mapper.toTierResponse(tierAdminService.upsertCriterion(tierCode, request)));
    }

    @PostMapping("/catalogue/refresh")
    @Operation(summary = "Rebuild the cached benefit catalogue")
    public ResponseEntity<Map<String, String>> refreshCatalogue() {
        benefitCatalogService.refresh();
        return ResponseEntity.ok(Map.of("status", "refreshed"));
    }

    @PostMapping("/sweeps/lifecycle")
    @Operation(summary = "Run the expiry/renewal sweep now (demo convenience)")
    public ResponseEntity<Map<String, String>> runLifecycleSweep() {
        membershipScheduler.sweepLifecycle();
        return ResponseEntity.ok(Map.of("status", "completed"));
    }

    @PostMapping("/sweeps/tier-evaluation")
    @Operation(summary = "Run the tier re-evaluation sweep now (demo convenience)")
    public ResponseEntity<Map<String, String>> runTierSweep() {
        membershipScheduler.sweepTierEligibility();
        return ResponseEntity.ok(Map.of("status", "completed"));
    }
}
