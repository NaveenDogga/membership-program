package com.firstclub.membership.api.controller;

import com.firstclub.membership.api.ApiMapper;
import com.firstclub.membership.api.dto.CatalogDtos;
import com.firstclub.membership.benefit.BenefitApplicationService;
import com.firstclub.membership.common.exception.ResourceNotFoundException;
import com.firstclub.membership.domain.repository.UserRepository;
import com.firstclub.membership.tier.TierEligibility;
import com.firstclub.membership.tier.TierEligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
@Tag(name = "Member view", description = "What a specific user has unlocked and is entitled to")
public class MembershipBenefitController {

    private final BenefitApplicationService benefitApplicationService;
    private final TierEligibilityService tierEligibilityService;
    private final UserRepository userRepository;
    private final ApiMapper mapper;

    @GetMapping("/benefits")
    @Operation(summary = "Benefits currently active for the user, flattened across inherited tiers")
    public ResponseEntity<List<CatalogDtos.BenefitResponse>> benefits(@PathVariable Long userId) {
        return ResponseEntity.ok(benefitApplicationService.activeBenefits(userId).stream()
                .map(mapper::toBenefitResponse)
                .toList());
    }

    @GetMapping("/tier-eligibility")
    @Operation(summary = "Per-tier eligibility with progress against each unlock criterion")
    public ResponseEntity<List<TierEligibility>> tierEligibility(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(tierEligibilityService.evaluateAllTiers(user)))
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }
}
