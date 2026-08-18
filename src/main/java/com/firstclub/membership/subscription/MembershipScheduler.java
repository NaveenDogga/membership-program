package com.firstclub.membership.subscription;

import com.firstclub.membership.config.MembershipProperties;
import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.domain.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
@RequiredArgsConstructor
public class MembershipScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final MembershipProperties properties;

    private final AtomicBoolean lifecycleRunning = new AtomicBoolean(false);
    private final AtomicBoolean tierEvaluationRunning = new AtomicBoolean(false);

    @Scheduled(cron = "${membership.scheduler.lifecycle-cron}")
    public void sweepLifecycle() {
        if (!lifecycleRunning.compareAndSet(false, true)) {
            log.debug("Lifecycle sweep already in progress; skipping this tick");
            return;
        }
        try {
            int batchSize = properties.scheduler().batchSizeOrDefault();
            List<Subscription> due = subscriptionRepository.findDueForLifecycleSweep(
                    SubscriptionStatus.ACTIVE, Instant.now(), PageRequest.of(0, batchSize));
            for (Subscription subscription : due) {
                safely(() -> subscriptionService.runLifecycleFor(subscription.getId()),
                        "lifecycle", subscription.getId());
            }
            if (!due.isEmpty()) {
                log.info("Lifecycle sweep processed {} subscription(s)", due.size());
            }
        } finally {
            lifecycleRunning.set(false);
        }
    }

    @Scheduled(cron = "${membership.scheduler.tier-evaluation-cron}")
    public void sweepTierEligibility() {
        if (!tierEvaluationRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            int batchSize = properties.scheduler().batchSizeOrDefault();
            List<Subscription> active = subscriptionRepository.findActiveForTierEvaluation(
                    SubscriptionStatus.ACTIVE, PageRequest.of(0, batchSize));
            for (Subscription subscription : active) {
                safely(() -> subscriptionService.autoUpgradeIfEarned(subscription.getId()),
                        "tier-evaluation", subscription.getId());
            }
        } finally {
            tierEvaluationRunning.set(false);
        }
    }

    private void safely(Runnable action, String sweep, Long subscriptionId) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.warn("{} sweep failed for subscription {}: {}", sweep, subscriptionId, ex.getMessage());
        }
    }
}
