package com.firstclub.membership.subscription;

import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.domain.model.User;
import com.firstclub.membership.domain.repository.SubscriptionRepository;
import com.firstclub.membership.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(properties = "membership.seed-demo-data=false")
class ConcurrentSubscriptionTest {

    private static final int THREADS = 12;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private TestDataFactory fixtures;

    @BeforeEach
    void setUp() {
        fixtures.reset();
        fixtures.seedCatalogue();
    }

    @Test
    @DisplayName("twelve simultaneous subscribe calls create exactly one active membership")
    void concurrentSubscribeCreatesOneMembership() throws Exception {
        User user = fixtures.newUser();
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        runConcurrently(() -> {
            try {
                subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", null);
                succeeded.incrementAndGet();
            } catch (RuntimeException expected) {
                rejected.incrementAndGet();
            }
        });

        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(THREADS - 1);

        List<Subscription> active = subscriptionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .toList();
        assertThat(active).hasSize(1);
    }

    @Test
    @DisplayName("retries carrying the same idempotency key never double-charge")
    void concurrentSubscribeWithSharedIdempotencyKey() throws Exception {
        User user = fixtures.newUser();
        String key = "payment-gateway-retry-7";
        AtomicInteger succeeded = new AtomicInteger();

        runConcurrently(() -> {
            try {
                subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", key);
                succeeded.incrementAndGet();
            } catch (RuntimeException ignored) {

            }
        });

        assertThat(subscriptionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).hasSize(1);
        assertThat(succeeded.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("competing upgrades on one membership resolve to a single tier change")
    void concurrentUpgradesSerialise() throws Exception {
        User user = fixtures.newUser();
        fixtures.placeOrders(user, 2, "1000.00");
        Subscription subscription = subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", null);

        AtomicInteger succeeded = new AtomicInteger();
        runConcurrently(() -> {
            try {
                subscriptionService.upgrade(subscription.getId(), "GOLD", null);
                succeeded.incrementAndGet();
            } catch (RuntimeException ignored) {

            }
        });

        Subscription reloaded = subscriptionRepository.findById(subscription.getId()).orElseThrow();
        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(reloaded.getTier().getCode()).isEqualTo("GOLD");

        assertThat(reloaded.getAmountPaid()).isLessThan(new java.math.BigDecimal("501.00"));
    }

    private void runConcurrently(Runnable action) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(THREADS);

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    action.run();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(finishGate.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
    }
}
