package com.firstclub.membership.subscription;

import com.firstclub.membership.common.Money;
import com.firstclub.membership.common.exception.BusinessRuleException;
import com.firstclub.membership.common.exception.ResourceNotFoundException;
import com.firstclub.membership.domain.enums.SubscriptionEventType;
import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.PlanTierOffering;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.domain.model.SubscriptionEvent;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.model.User;
import com.firstclub.membership.domain.repository.MembershipPlanRepository;
import com.firstclub.membership.domain.repository.PlanTierOfferingRepository;
import com.firstclub.membership.domain.repository.SubscriptionEventRepository;
import com.firstclub.membership.domain.repository.SubscriptionRepository;
import com.firstclub.membership.domain.repository.TierRepository;
import com.firstclub.membership.domain.repository.UserRepository;
import com.firstclub.membership.tier.TierEligibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserRepository userRepository;
    private final MembershipPlanRepository planRepository;
    private final TierRepository tierRepository;
    private final PlanTierOfferingRepository offeringRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionEventRepository eventRepository;
    private final TierEligibilityService tierEligibilityService;
    private final PricingService pricingService;
    private final IdempotencyService idempotencyService;

    @Transactional(readOnly = true)
    public Optional<Subscription> currentMembership(Long userId) {
        requireUser(userId);
        Instant now = Instant.now();
        return subscriptionRepository.findByActiveUserKey(userId)
                .filter(subscription -> subscription.isLive(now));
    }

    @Transactional(readOnly = true)
    public Subscription getSubscription(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Subscription", subscriptionId));
    }

    @Transactional(readOnly = true)
    public List<Subscription> subscriptionsOf(Long userId) {
        requireUser(userId);
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionEvent> history(Long subscriptionId) {
        getSubscription(subscriptionId);
        return eventRepository.findBySubscriptionIdOrderByOccurredAtAsc(subscriptionId);
    }

    @Transactional
    public Subscription subscribe(Long userId, String planCode, String tierCode, String idempotencyKey) {
        String requestHash = idempotencyService.fingerprint(
                "userId=" + userId,
                "planCode=" + normalize(planCode),
                "tierCode=" + normalize(tierCode));
        Optional<Long> replay = idempotencyService.findExistingResource(
                idempotencyKey, "SUBSCRIBE", requestHash);
        if (replay.isPresent()) {
            return getSubscription(replay.get());
        }

        User user = requireUser(userId);
        MembershipPlan plan = requirePlan(planCode);
        Tier tier = requireTier(tierCode);
        PlanTierOffering offering = requireOffering(plan, tier);

        subscriptionRepository.findByActiveUserKey(userId).ifPresent(existing -> {
            throw new BusinessRuleException("ALREADY_SUBSCRIBED",
                    "User already holds an active membership (subscription " + existing.getId() + ")");
        });

        tierEligibilityService.assertEligible(user, tier);

        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .tier(tier)
                .amountPaid(Money.normalize(offering.getPrice()))
                .currency(offering.getCurrency())
                .startAt(now)
                .endAt(endOfCycle(now, plan))
                .autoRenew(true)
                .build();
        subscription.transitionTo(SubscriptionStatus.ACTIVE);

        Subscription saved = subscriptionRepository.saveAndFlush(subscription);
        recordEvent(saved, SubscriptionEventType.SUBSCRIBED, null, tier.getCode(), saved.getAmountPaid(),
                "Subscribed to " + plan.getCode() + " / " + tier.getCode());
        idempotencyService.record(idempotencyKey, "SUBSCRIBE", requestHash, saved.getId());
        return saved;
    }

    @Transactional
    public Subscription upgrade(Long subscriptionId, String targetTierCode, String idempotencyKey) {
        String requestHash = idempotencyService.fingerprint(
                "subscriptionId=" + subscriptionId,
                "tierCode=" + normalize(targetTierCode));
        Optional<Long> replay = idempotencyService.findExistingResource(
                idempotencyKey, "UPGRADE", requestHash);
        if (replay.isPresent()) {
            return getSubscription(replay.get());
        }

        Subscription subscription = lockActive(subscriptionId);
        Tier targetTier = requireTier(targetTierCode);
        Tier currentTier = subscription.getTier();

        if (targetTier.getRank() <= currentTier.getRank()) {
            throw new BusinessRuleException("NOT_AN_UPGRADE",
                    "Tier " + targetTier.getCode() + " is not above the current tier " + currentTier.getCode());
        }

        PlanTierOffering offering = requireOffering(subscription.getPlan(), targetTier);
        tierEligibilityService.assertEligible(subscription.getUser(), targetTier);

        Instant now = Instant.now();
        BigDecimal charge = pricingService.upgradeCharge(subscription, offering, now);

        subscription.setTier(targetTier);
        subscription.setAmountPaid(Money.normalize(subscription.getAmountPaid().add(charge)));

        subscription.setPendingTier(null);
        subscription.setPendingTierEffectiveAt(null);

        Subscription saved = subscriptionRepository.saveAndFlush(subscription);
        recordEvent(saved, SubscriptionEventType.UPGRADED, currentTier.getCode(), targetTier.getCode(), charge,
                "Immediate upgrade, prorated charge " + charge.toPlainString());
        idempotencyService.record(idempotencyKey, "UPGRADE", requestHash, saved.getId());
        return saved;
    }

    @Transactional
    public Subscription downgrade(Long subscriptionId, String targetTierCode) {
        Subscription subscription = lockActive(subscriptionId);
        Tier targetTier = requireTier(targetTierCode);
        Tier currentTier = subscription.getTier();

        if (targetTier.getRank() >= currentTier.getRank()) {
            throw new BusinessRuleException("NOT_A_DOWNGRADE",
                    "Tier " + targetTier.getCode() + " is not below the current tier " + currentTier.getCode());
        }
        requireOffering(subscription.getPlan(), targetTier);

        subscription.setPendingTier(targetTier);
        subscription.setPendingTierEffectiveAt(subscription.getEndAt());

        Subscription saved = subscriptionRepository.saveAndFlush(subscription);
        recordEvent(saved, SubscriptionEventType.DOWNGRADE_SCHEDULED, currentTier.getCode(), targetTier.getCode(),
                null, "Takes effect at " + subscription.getEndAt());
        return saved;
    }

    @Transactional
    public Subscription cancel(Long subscriptionId, boolean immediate) {
        Subscription subscription = lockActive(subscriptionId);
        Instant now = Instant.now();
        subscription.setAutoRenew(false);

        if (immediate) {
            subscription.transitionTo(SubscriptionStatus.CANCELLED);
            subscription.setCancelledAt(now);
            subscription.setEndAt(now);
            Subscription saved = subscriptionRepository.saveAndFlush(subscription);
            recordEvent(saved, SubscriptionEventType.CANCELLED, subscription.getTier().getCode(), null, null,
                    "Cancelled immediately; benefits revoked");
            return saved;
        }

        subscription.setCancelAtPeriodEnd(true);
        Subscription saved = subscriptionRepository.saveAndFlush(subscription);
        recordEvent(saved, SubscriptionEventType.CANCELLATION_SCHEDULED, subscription.getTier().getCode(), null,
                null, "Benefits continue until " + subscription.getEndAt());
        return saved;
    }

    @Transactional
    public void runLifecycleFor(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId).orElse(null);
        if (subscription == null || subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            return;
        }
        Instant now = Instant.now();
        if (subscription.getEndAt().isAfter(now)) {
            return;
        }

        applyPendingTierIfDue(subscription, now);

        if (!subscription.isAutoRenew() || subscription.isCancelAtPeriodEnd()) {
            subscription.transitionTo(subscription.isCancelAtPeriodEnd()
                    ? SubscriptionStatus.CANCELLED
                    : SubscriptionStatus.EXPIRED);
            if (subscription.isCancelAtPeriodEnd()) {
                subscription.setCancelledAt(now);
            }
            Subscription saved = subscriptionRepository.saveAndFlush(subscription);
            recordEvent(saved, subscription.isCancelAtPeriodEnd()
                            ? SubscriptionEventType.CANCELLED
                            : SubscriptionEventType.EXPIRED,
                    saved.getTier().getCode(), null, null, "Period ended");
            return;
        }

        PlanTierOffering offering = offeringRepository
                .findByPlanIdAndTierIdAndActiveTrue(subscription.getPlan().getId(), subscription.getTier().getId())
                .orElse(null);
        if (offering == null) {

            subscription.transitionTo(SubscriptionStatus.EXPIRED);
            Subscription saved = subscriptionRepository.saveAndFlush(subscription);
            recordEvent(saved, SubscriptionEventType.EXPIRED, saved.getTier().getCode(), null, null,
                    "Offering no longer available for renewal");
            return;
        }

        subscription.setStartAt(subscription.getEndAt());
        subscription.setEndAt(endOfCycle(subscription.getStartAt(), subscription.getPlan()));
        subscription.setAmountPaid(Money.normalize(offering.getPrice()));
        Subscription saved = subscriptionRepository.saveAndFlush(subscription);
        recordEvent(saved, SubscriptionEventType.RENEWED, saved.getTier().getCode(), saved.getTier().getCode(),
                saved.getAmountPaid(), "Renewed until " + saved.getEndAt());
    }

    @Transactional
    public Optional<Subscription> autoUpgradeIfEarned(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId).orElse(null);
        if (subscription == null || !subscription.isLive(Instant.now())) {
            return Optional.empty();
        }

        Optional<Tier> earned = tierEligibilityService.highestEligibleTier(subscription.getUser());
        if (earned.isEmpty() || earned.get().getRank() <= subscription.getTier().getRank()) {
            return Optional.empty();
        }
        Tier targetTier = earned.get();
        if (offeringRepository.findByPlanIdAndTierIdAndActiveTrue(
                subscription.getPlan().getId(), targetTier.getId()).isEmpty()) {
            return Optional.empty();
        }

        String fromTier = subscription.getTier().getCode();
        subscription.setTier(targetTier);
        Subscription saved = subscriptionRepository.saveAndFlush(subscription);
        recordEvent(saved, SubscriptionEventType.AUTO_UPGRADED, fromTier, targetTier.getCode(), null,
                "Tier criteria met; promoted at no extra charge");
        log.info("Auto-upgraded subscription {} from {} to {}", subscriptionId, fromTier, targetTier.getCode());
        return Optional.of(saved);
    }

    private void applyPendingTierIfDue(Subscription subscription, Instant now) {
        Tier pending = subscription.getPendingTier();
        if (pending == null || subscription.getPendingTierEffectiveAt() == null
                || subscription.getPendingTierEffectiveAt().isAfter(now)) {
            return;
        }
        String fromTier = subscription.getTier().getCode();
        subscription.setTier(pending);
        subscription.setPendingTier(null);
        subscription.setPendingTierEffectiveAt(null);
        recordEvent(subscription, SubscriptionEventType.DOWNGRADE_APPLIED, fromTier, pending.getCode(), null,
                "Scheduled downgrade applied at period end");
    }

    private Subscription lockActive(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Subscription", subscriptionId));
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessRuleException("SUBSCRIPTION_NOT_ACTIVE",
                    "Subscription " + subscriptionId + " is " + subscription.getStatus());
        }
        if (!subscription.getEndAt().isAfter(Instant.now())) {
            throw new BusinessRuleException("SUBSCRIPTION_EXPIRED",
                    "Subscription " + subscriptionId + " ended at " + subscription.getEndAt());
        }
        return subscription;
    }

    private Instant endOfCycle(Instant start, MembershipPlan plan) {
        return start.atZone(ZoneOffset.UTC).plus(plan.getBillingCycle().getPeriod()).toInstant();
    }

    private void recordEvent(Subscription subscription, SubscriptionEventType type, String fromTier,
                             String toTier, BigDecimal amount, String note) {
        eventRepository.save(SubscriptionEvent.builder()
                .subscription(subscription)
                .eventType(type)
                .fromTierCode(fromTier)
                .toTierCode(toTier)
                .amount(amount)
                .note(note)
                .occurredAt(Instant.now())
                .build());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }

    private MembershipPlan requirePlan(String planCode) {
        MembershipPlan plan = planRepository.findByCodeIgnoreCase(planCode)
                .orElseThrow(() -> ResourceNotFoundException.of("MembershipPlan", planCode));
        if (!plan.isActive()) {
            throw new BusinessRuleException("PLAN_INACTIVE", "Plan " + planCode + " is not available");
        }
        return plan;
    }

    private Tier requireTier(String tierCode) {
        Tier tier = tierRepository.findByCodeIgnoreCase(tierCode)
                .orElseThrow(() -> ResourceNotFoundException.of("Tier", tierCode));
        if (!tier.isActive()) {
            throw new BusinessRuleException("TIER_INACTIVE", "Tier " + tierCode + " is not available");
        }
        return tier;
    }

    private PlanTierOffering requireOffering(MembershipPlan plan, Tier tier) {
        return offeringRepository.findByPlanIdAndTierIdAndActiveTrue(plan.getId(), tier.getId())
                .orElseThrow(() -> new BusinessRuleException("OFFERING_UNAVAILABLE",
                        "Tier " + tier.getCode() + " is not offered on the " + plan.getCode() + " plan"));
    }
}
