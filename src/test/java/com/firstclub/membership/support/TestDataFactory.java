package com.firstclub.membership.support;

import com.firstclub.membership.benefit.BenefitCatalogService;
import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.enums.BillingCycle;
import com.firstclub.membership.domain.enums.CriteriaMatchPolicy;
import com.firstclub.membership.domain.enums.CriterionType;
import com.firstclub.membership.domain.enums.OrderStatus;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.OrderRecord;
import com.firstclub.membership.domain.model.PlanTierOffering;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.model.TierBenefit;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.domain.model.User;
import com.firstclub.membership.domain.repository.IdempotencyRecordRepository;
import com.firstclub.membership.domain.repository.MembershipPlanRepository;
import com.firstclub.membership.domain.repository.OrderRepository;
import com.firstclub.membership.domain.repository.PlanTierOfferingRepository;
import com.firstclub.membership.domain.repository.SubscriptionEventRepository;
import com.firstclub.membership.domain.repository.SubscriptionRepository;
import com.firstclub.membership.domain.repository.TierRepository;
import com.firstclub.membership.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class TestDataFactory {

    private final MembershipPlanRepository planRepository;
    private final TierRepository tierRepository;
    private final PlanTierOfferingRepository offeringRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionEventRepository eventRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final BenefitCatalogService benefitCatalogService;

    private final AtomicInteger emailSequence = new AtomicInteger();

    @Transactional
    public void reset() {
        eventRepository.deleteAllInBatch();
        subscriptionRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        offeringRepository.deleteAllInBatch();
        idempotencyRepository.deleteAllInBatch();
        tierRepository.deleteAll();
        planRepository.deleteAllInBatch();
        userRepository.deleteAll();
        benefitCatalogService.refresh();
    }

    @Transactional
    public void seedCatalogue() {
        Tier silver = Tier.builder()
                .code("SILVER").name("Silver").rank(1)
                .criteriaMatchPolicy(CriteriaMatchPolicy.ALL)
                .inheritsLowerTierBenefits(true).active(true).build();
        silver.addBenefit(TierBenefit.builder()
                .benefitType(BenefitType.FREE_DELIVERY)
                .description("Free delivery above 499")
                .config(new HashMap<>(Map.of("minOrderValue", "499")))
                .active(true).build());
        silver.addBenefit(TierBenefit.builder()
                .benefitType(BenefitType.EXTRA_DISCOUNT)
                .description("3% off")
                .config(new HashMap<>(Map.of("percentage", "3", "maxDiscount", "150")))
                .active(true).build());

        Tier gold = Tier.builder()
                .code("GOLD").name("Gold").rank(2)
                .criteriaMatchPolicy(CriteriaMatchPolicy.ANY)
                .inheritsLowerTierBenefits(true).active(true).build();
        gold.addCriterion(TierCriterion.builder()
                .criterionType(CriterionType.MIN_LIFETIME_ORDERS)
                .description("2 lifetime orders")
                .config(new HashMap<>(Map.of("minOrders", "2")))
                .active(true).build());
        gold.addBenefit(TierBenefit.builder()
                .benefitType(BenefitType.FREE_DELIVERY)
                .description("Free delivery always")
                .config(new HashMap<>(Map.of("minOrderValue", "0")))
                .active(true).build());
        gold.addBenefit(TierBenefit.builder()
                .benefitType(BenefitType.EXTRA_DISCOUNT)
                .description("10% off")
                .config(new HashMap<>(Map.of("percentage", "10", "maxDiscount", "500")))
                .active(true).build());

        tierRepository.saveAll(java.util.List.of(silver, gold));

        MembershipPlan monthly = planRepository.save(MembershipPlan.builder()
                .code("MONTHLY").name("Monthly").billingCycle(BillingCycle.MONTHLY).active(true).build());

        offeringRepository.save(PlanTierOffering.builder()
                .plan(monthly).tier(silver).price(new BigDecimal("200.00")).currency("INR").active(true).build());
        offeringRepository.save(PlanTierOffering.builder()
                .plan(monthly).tier(gold).price(new BigDecimal("500.00")).currency("INR").active(true).build());

        benefitCatalogService.refresh();
    }

    @Transactional
    public User newUser(String... cohorts) {
        return userRepository.save(User.builder()
                .name("Test User")
                .email("user" + emailSequence.incrementAndGet() + "@firstclub.test")
                .cohorts(Set.of(cohorts))
                .build());
    }

    @Transactional
    public Subscription saveSubscription(Subscription subscription) {
        return subscriptionRepository.saveAndFlush(subscription);
    }

    @Transactional
    public void placeOrders(User user, int count, String amount) {
        for (int i = 0; i < count; i++) {
            orderRepository.save(OrderRecord.builder()
                    .user(user)
                    .totalAmount(new BigDecimal(amount))
                    .status(OrderStatus.DELIVERED)
                    .placedAt(Instant.now().minusSeconds(3600L * (i + 1)))
                    .build());
        }
    }
}
