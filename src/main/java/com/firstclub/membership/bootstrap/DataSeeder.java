package com.firstclub.membership.bootstrap;

import com.firstclub.membership.benefit.BenefitCatalogService;
import com.firstclub.membership.config.MembershipProperties;
import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.enums.BillingCycle;
import com.firstclub.membership.domain.enums.CriteriaMatchPolicy;
import com.firstclub.membership.domain.enums.CriterionType;
import com.firstclub.membership.domain.enums.OrderStatus;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.OrderRecord;
import com.firstclub.membership.domain.model.PlanTierOffering;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.model.TierBenefit;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.domain.model.User;
import com.firstclub.membership.domain.repository.MembershipPlanRepository;
import com.firstclub.membership.domain.repository.OrderRepository;
import com.firstclub.membership.domain.repository.PlanTierOfferingRepository;
import com.firstclub.membership.domain.repository.TierRepository;
import com.firstclub.membership.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final MembershipPlanRepository planRepository;
    private final TierRepository tierRepository;
    private final PlanTierOfferingRepository offeringRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final BenefitCatalogService benefitCatalogService;
    private final MembershipProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.seedDemoData() || planRepository.count() > 0) {
            return;
        }

        Tier silver = seedSilver();
        Tier gold = seedGold();
        Tier platinum = seedPlatinum();
        tierRepository.saveAll(List.of(silver, gold, platinum));

        MembershipPlan monthly = planRepository.save(MembershipPlan.builder()
                .code("MONTHLY").name("Monthly").billingCycle(BillingCycle.MONTHLY).active(true).build());
        MembershipPlan quarterly = planRepository.save(MembershipPlan.builder()
                .code("QUARTERLY").name("Quarterly").billingCycle(BillingCycle.QUARTERLY).active(true).build());
        MembershipPlan yearly = planRepository.save(MembershipPlan.builder()
                .code("YEARLY").name("Yearly").billingCycle(BillingCycle.YEARLY).active(true).build());

        offer(monthly, silver, "199.00");
        offer(monthly, gold, "349.00");
        offer(monthly, platinum, "599.00");
        offer(quarterly, silver, "499.00");
        offer(quarterly, gold, "899.00");
        offer(quarterly, platinum, "1599.00");
        offer(yearly, silver, "1499.00");
        offer(yearly, gold, "2799.00");
        offer(yearly, platinum, "4999.00");

        seedUsers();
        benefitCatalogService.refresh();

        log.info("Seeded membership catalogue: 3 plans x 3 tiers, and 4 demo users");
    }

    private Tier seedSilver() {
        Tier silver = Tier.builder()
                .code("SILVER").name("Silver").rank(1)
                .criteriaMatchPolicy(CriteriaMatchPolicy.ALL)
                .inheritsLowerTierBenefits(true)
                .active(true)
                .build();

        silver.addBenefit(benefit(BenefitType.FREE_DELIVERY,
                "Free delivery on orders above INR 499", Map.of("minOrderValue", "499")));
        silver.addBenefit(benefit(BenefitType.EXTRA_DISCOUNT,
                "3% member discount", Map.of("percentage", "3", "maxDiscount", "150")));
        silver.addBenefit(benefit(BenefitType.EXCLUSIVE_DEALS,
                "Access to member-only deals", Map.of()));
        return silver;
    }

    private Tier seedGold() {
        Tier gold = Tier.builder()
                .code("GOLD").name("Gold").rank(2)
                .criteriaMatchPolicy(CriteriaMatchPolicy.ANY)
                .inheritsLowerTierBenefits(true)
                .active(true)
                .build();
        gold.addCriterion(criterion(CriterionType.MIN_ORDERS_IN_WINDOW,
                "At least 3 orders in the last 30 days",
                Map.of("minOrders", "3", "windowDays", "30")));
        gold.addCriterion(criterion(CriterionType.MIN_ORDER_VALUE_IN_WINDOW,
                "At least INR 5,000 spent in the last 30 days",
                Map.of("minValue", "5000", "windowDays", "30")));

        gold.addBenefit(benefit(BenefitType.FREE_DELIVERY,
                "Free delivery on every order", Map.of("minOrderValue", "0")));
        gold.addBenefit(benefit(BenefitType.EXTRA_DISCOUNT,
                "7% member discount", Map.of("percentage", "7", "maxDiscount", "500")));
        gold.addBenefit(benefit(BenefitType.EARLY_ACCESS,
                "Early access to sales", Map.of("hoursBeforePublic", "24")));
        gold.addBenefit(benefit(BenefitType.FASTER_DELIVERY,
                "Priority dispatch", Map.of("slaHours", "48")));
        return gold;
    }

    private Tier seedPlatinum() {
        Tier platinum = Tier.builder()
                .code("PLATINUM").name("Platinum").rank(3)
                .criteriaMatchPolicy(CriteriaMatchPolicy.ANY)
                .inheritsLowerTierBenefits(true)
                .active(true)
                .build();
        platinum.addCriterion(criterion(CriterionType.MIN_ORDER_VALUE_IN_WINDOW,
                "At least INR 20,000 spent in the last 30 days",
                Map.of("minValue", "20000", "windowDays", "30")));
        platinum.addCriterion(criterion(CriterionType.MIN_LIFETIME_ORDERS,
                "At least 25 lifetime orders", Map.of("minOrders", "25")));
        platinum.addCriterion(criterion(CriterionType.COHORT_MEMBERSHIP,
                "Invited FirstClub Elite cohort", Map.of("cohorts", "FIRSTCLUB_ELITE")));

        platinum.addBenefit(benefit(BenefitType.EXTRA_DISCOUNT,
                "12% member discount", Map.of("percentage", "12", "maxDiscount", "2000")));
        platinum.addBenefit(benefit(BenefitType.FASTER_DELIVERY,
                "Same-day dispatch", Map.of("slaHours", "24")));
        platinum.addBenefit(benefit(BenefitType.PRIORITY_SUPPORT,
                "Dedicated support line", Map.of("queue", "PLATINUM", "responseMinutes", "5")));
        platinum.addBenefit(benefit(BenefitType.EXCLUSIVE_COUPONS,
                "Monthly exclusive coupons", Map.of("couponsPerCycle", "3")));
        return platinum;
    }

    private void seedUsers() {
        Instant now = Instant.now();

        userRepository.save(User.builder()
                .name("Aarav Sharma").email("aarav@firstclub.test").cohorts(Set.of()).build());

        User diya = userRepository.save(User.builder()
                .name("Diya Nair").email("diya@firstclub.test").cohorts(Set.of()).build());
        for (int i = 1; i <= 4; i++) {
            order(diya, "1250.00", now.minus(Duration.ofDays(i * 3L)));
        }

        User rohan = userRepository.save(User.builder()
                .name("Rohan Mehta").email("rohan@firstclub.test").cohorts(Set.of()).build());
        order(rohan, "12500.00", now.minus(Duration.ofDays(5)));
        order(rohan, "9800.00", now.minus(Duration.ofDays(12)));

        userRepository.save(User.builder()
                .name("Ishita Rao").email("ishita@firstclub.test")
                .cohorts(Set.of("FIRSTCLUB_ELITE")).build());
    }

    private void offer(MembershipPlan plan, Tier tier, String price) {
        offeringRepository.save(PlanTierOffering.builder()
                .plan(plan).tier(tier).price(new BigDecimal(price)).currency("INR").active(true).build());
    }

    private void order(User user, String amount, Instant placedAt) {
        orderRepository.save(OrderRecord.builder()
                .user(user).totalAmount(new BigDecimal(amount))
                .status(OrderStatus.DELIVERED).placedAt(placedAt).build());
    }

    private TierBenefit benefit(BenefitType type, String description, Map<String, String> config) {
        return TierBenefit.builder()
                .benefitType(type).description(description).config(new HashMap<>(config)).active(true).build();
    }

    private TierCriterion criterion(CriterionType type, String description, Map<String, String> config) {
        return TierCriterion.builder()
                .criterionType(type).description(description).config(new HashMap<>(config)).active(true).build();
    }
}
