package com.firstclub.membership.api.controller;

import com.firstclub.membership.api.ApiMapper;
import com.firstclub.membership.api.dto.CatalogDtos;
import com.firstclub.membership.catalog.MembershipCatalogService;
import com.firstclub.membership.domain.model.PlanTierOffering;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Catalogue", description = "Browse membership plans and tiers")
public class CatalogController {

    private final MembershipCatalogService catalogService;
    private final ApiMapper mapper;

    @GetMapping("/plans")
    @Operation(summary = "List membership plans with the tiers and prices available on each")
    public ResponseEntity<List<CatalogDtos.PlanResponse>> plans() {
        Map<Long, List<PlanTierOffering>> offerings = catalogService.offeringsByPlan();
        List<CatalogDtos.PlanResponse> response = catalogService.activePlans().stream()
                .map(plan -> mapper.toPlanResponse(plan, offerings.getOrDefault(plan.getId(), List.of())))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tiers")
    @Operation(summary = "List tiers with their effective benefits and unlock criteria")
    public ResponseEntity<List<CatalogDtos.TierResponse>> tiers() {
        return ResponseEntity.ok(catalogService.activeTiers().stream()
                .map(mapper::toTierResponse)
                .toList());
    }

    @GetMapping("/tiers/{tierCode}")
    @Operation(summary = "Fetch a single tier")
    public ResponseEntity<CatalogDtos.TierResponse> tier(@PathVariable String tierCode) {
        return ResponseEntity.ok(mapper.toTierResponse(catalogService.requireTier(tierCode)));
    }
}
