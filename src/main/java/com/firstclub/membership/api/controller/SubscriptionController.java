package com.firstclub.membership.api.controller;

import com.firstclub.membership.api.ApiMapper;
import com.firstclub.membership.api.dto.SubscriptionDtos;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.subscription.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Subscribe, upgrade, downgrade, cancel and track memberships")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final ApiMapper mapper;

    @PostMapping("/subscriptions")
    @Operation(summary = "Subscribe a user to a plan + tier",
            description = "Send an Idempotency-Key header to make retries safe.")
    public ResponseEntity<SubscriptionDtos.SubscriptionResponse> subscribe(
            @Valid @RequestBody SubscriptionDtos.SubscribeRequest request,
            @Parameter(description = "Client-generated key that makes the call exactly-once")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        Subscription subscription = subscriptionService.subscribe(
                request.userId(), request.planCode(), request.tierCode(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toSubscriptionResponse(subscription));
    }

    @GetMapping("/subscriptions/{subscriptionId}")
    @Operation(summary = "Fetch a subscription")
    public ResponseEntity<SubscriptionDtos.SubscriptionResponse> get(@PathVariable Long subscriptionId) {
        return ResponseEntity.ok(mapper.toSubscriptionResponse(
                subscriptionService.getSubscription(subscriptionId)));
    }

    @PostMapping("/subscriptions/{subscriptionId}/upgrade")
    @Operation(summary = "Upgrade tier immediately for a prorated charge")
    public ResponseEntity<SubscriptionDtos.SubscriptionResponse> upgrade(
            @PathVariable Long subscriptionId,
            @Valid @RequestBody SubscriptionDtos.TierChangeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(mapper.toSubscriptionResponse(
                subscriptionService.upgrade(subscriptionId, request.tierCode(), idempotencyKey)));
    }

    @PostMapping("/subscriptions/{subscriptionId}/downgrade")
    @Operation(summary = "Schedule a tier downgrade for the end of the current cycle")
    public ResponseEntity<SubscriptionDtos.SubscriptionResponse> downgrade(
            @PathVariable Long subscriptionId,
            @Valid @RequestBody SubscriptionDtos.TierChangeRequest request) {

        return ResponseEntity.ok(mapper.toSubscriptionResponse(
                subscriptionService.downgrade(subscriptionId, request.tierCode())));
    }

    @PostMapping("/subscriptions/{subscriptionId}/cancel")
    @Operation(summary = "Cancel a membership, immediately or at period end")
    public ResponseEntity<SubscriptionDtos.SubscriptionResponse> cancel(
            @PathVariable Long subscriptionId,
            @RequestBody(required = false) SubscriptionDtos.CancelRequest request) {

        boolean immediate = request != null && request.immediate();
        return ResponseEntity.ok(mapper.toSubscriptionResponse(
                subscriptionService.cancel(subscriptionId, immediate)));
    }

    @GetMapping("/subscriptions/{subscriptionId}/events")
    @Operation(summary = "Audit trail of everything that happened to this membership")
    public ResponseEntity<List<SubscriptionDtos.SubscriptionEventResponse>> events(
            @PathVariable Long subscriptionId) {
        return ResponseEntity.ok(subscriptionService.history(subscriptionId).stream()
                .map(mapper::toEventResponse)
                .toList());
    }

    @GetMapping("/users/{userId}/membership")
    @Operation(summary = "Current membership and expiry; 204 if the user is not a member")
    public ResponseEntity<SubscriptionDtos.SubscriptionResponse> currentMembership(@PathVariable Long userId) {
        return subscriptionService.currentMembership(userId)
                .map(mapper::toSubscriptionResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/users/{userId}/subscriptions")
    @Operation(summary = "Full subscription history for a user")
    public ResponseEntity<List<SubscriptionDtos.SubscriptionResponse>> userSubscriptions(
            @PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.subscriptionsOf(userId).stream()
                .map(mapper::toSubscriptionResponse)
                .toList());
    }
}
