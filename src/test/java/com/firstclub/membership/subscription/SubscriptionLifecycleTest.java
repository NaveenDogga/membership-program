package com.firstclub.membership.subscription;

import com.firstclub.membership.common.exception.BusinessRuleException;
import com.firstclub.membership.common.exception.ConflictException;
import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.domain.model.User;
import com.firstclub.membership.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SpringBootTest(properties = "membership.seed-demo-data=false")
class SubscriptionLifecycleTest {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private TestDataFactory fixtures;

    @BeforeEach
    void setUp() {
        fixtures.reset();
        fixtures.seedCatalogue();
    }

    @Test
    @DisplayName("subscribing sets an active membership with a one-month expiry")
    void subscribeCreatesActiveMembership() {
        User user = fixtures.newUser();

        Subscription subscription = subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", null);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getTier().getCode()).isEqualTo("SILVER");
        assertThat(subscription.getAmountPaid()).isEqualByComparingTo("200.00");
        assertThat(ChronoUnit.DAYS.between(subscription.getStartAt(), subscription.getEndAt()))
                .isBetween(28L, 31L);
        assertThat(subscriptionService.currentMembership(user.getId())).isPresent();
    }

    @Test
    @DisplayName("a second subscription for the same user is rejected")
    void oneActiveSubscriptionPerUser() {
        User user = fixtures.newUser();
        subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", null);

        assertThatThrownBy(() -> subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active membership");
    }

    @Test
    @DisplayName("a tier the user has not unlocked cannot be purchased")
    void lockedTierCannotBeSubscribed() {
        User user = fixtures.newUser();

        assertThatThrownBy(() -> subscriptionService.subscribe(user.getId(), "MONTHLY", "GOLD", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not unlocked");
    }

    @Test
    @DisplayName("upgrading charges only the prorated difference and keeps the cycle end date")
    void upgradeIsProrated() {
        User user = fixtures.newUser();
        Subscription silver = subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", null);
        fixtures.placeOrders(user, 2, "1000.00");

        Subscription gold = subscriptionService.upgrade(silver.getId(), "GOLD", null);

        assertThat(gold.getTier().getCode()).isEqualTo("GOLD");
        assertThat(gold.getEndAt()).isEqualTo(silver.getEndAt());

        assertThat(gold.getAmountPaid()).isBetween(new BigDecimal("480.00"), new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("downgrading is scheduled for period end, not applied immediately")
    void downgradeIsScheduled() {
        User user = fixtures.newUser();
        fixtures.placeOrders(user, 2, "1000.00");
        Subscription gold = subscriptionService.subscribe(user.getId(), "MONTHLY", "GOLD", null);

        Subscription result = subscriptionService.downgrade(gold.getId(), "SILVER");

        assertThat(result.getTier().getCode()).isEqualTo("GOLD");
        assertThat(result.getPendingTier().getCode()).isEqualTo("SILVER");
        assertThat(result.getPendingTierEffectiveAt()).isEqualTo(gold.getEndAt());
    }

    @Test
    @DisplayName("upgrading cancels a queued downgrade")
    void upgradeSupersedesScheduledDowngrade() {
        User user = fixtures.newUser();
        fixtures.placeOrders(user, 2, "1000.00");
        Subscription gold = subscriptionService.subscribe(user.getId(), "MONTHLY", "GOLD", null);
        subscriptionService.downgrade(gold.getId(), "SILVER");
        subscriptionService.cancel(gold.getId(), true);

        Subscription silver = subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", null);
        Subscription upgraded = subscriptionService.upgrade(silver.getId(), "GOLD", null);

        assertThat(upgraded.getPendingTier()).isNull();
    }

    @Test
    @DisplayName("immediate cancellation frees the user to subscribe again")
    void immediateCancellationReleasesTheSlot() {
        User user = fixtures.newUser();
        Subscription subscription = subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", null);

        Subscription cancelled = subscriptionService.cancel(subscription.getId(), true);
        assertThat(cancelled.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(subscriptionService.currentMembership(user.getId())).isEmpty();

        assertThat(subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", null).getId())
                .isNotEqualTo(subscription.getId());
    }

    @Test
    @DisplayName("cancel-at-period-end keeps benefits alive until expiry")
    void deferredCancellationKeepsBenefits() {
        User user = fixtures.newUser();
        Subscription subscription = subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", null);

        Subscription cancelled = subscriptionService.cancel(subscription.getId(), false);

        assertThat(cancelled.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(cancelled.isCancelAtPeriodEnd()).isTrue();
        assertThat(cancelled.isAutoRenew()).isFalse();
        assertThat(subscriptionService.currentMembership(user.getId())).isPresent();
    }

    @Test
    @DisplayName("repeating a subscribe call with the same idempotency key returns the same membership")
    void idempotentSubscribe() {
        User user = fixtures.newUser();
        String key = "checkout-attempt-42";

        Subscription first = subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", key);
        Subscription replay = subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", key);

        assertThat(replay.getId()).isEqualTo(first.getId());
    }

    @Test
    @DisplayName("reusing an idempotency key for different request parameters is rejected")
    void idempotencyKeyCannotBeReusedForDifferentRequest() {
        User user = fixtures.newUser();
        String key = "checkout-attempt-43";

        subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", key);

        assertThatThrownBy(() -> subscriptionService.subscribe(user.getId(), "MONTHLY", "GOLD", key))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("different request");
    }

    @Test
    @DisplayName("current membership is empty when the active row has already expired")
    void currentMembershipIgnoresExpiredActiveRow() {
        User user = fixtures.newUser();
        Subscription subscription = subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", null);

        subscription.setEndAt(Instant.now().minusSeconds(1));
        fixtures.saveSubscription(subscription);

        assertThat(subscriptionService.currentMembership(user.getId())).isEmpty();
    }

    @Test
    @DisplayName("earning a higher tier promotes the member for free")
    void autoUpgradeOnEarnedTier() {
        User user = fixtures.newUser();
        Subscription silver = subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", null);
        BigDecimal paid = silver.getAmountPaid();

        fixtures.placeOrders(user, 3, "1000.00");
        Subscription promoted = subscriptionService.autoUpgradeIfEarned(silver.getId()).orElseThrow();

        assertThat(promoted.getTier().getCode()).isEqualTo("GOLD");
        assertThat(promoted.getAmountPaid()).isEqualByComparingTo(paid);
    }
}
