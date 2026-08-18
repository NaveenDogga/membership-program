package com.firstclub.membership.order;

import com.firstclub.membership.domain.repository.SubscriptionRepository;
import com.firstclub.membership.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class TierPromotionListener {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    @Async("membershipTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        subscriptionRepository.findByActiveUserKey(event.userId()).ifPresent(subscription -> {
            try {
                subscriptionService.autoUpgradeIfEarned(subscription.getId());
            } catch (RuntimeException ex) {

                log.warn("Tier re-evaluation failed for subscription {}: {}",
                        subscription.getId(), ex.getMessage());
            }
        });
    }
}
