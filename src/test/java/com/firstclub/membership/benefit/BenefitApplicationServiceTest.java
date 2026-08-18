package com.firstclub.membership.benefit;

import com.firstclub.membership.domain.model.User;
import com.firstclub.membership.subscription.SubscriptionService;
import com.firstclub.membership.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest(properties = "membership.seed-demo-data=false")
class BenefitApplicationServiceTest {

    @Autowired
    private BenefitApplicationService benefitApplicationService;

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
    @DisplayName("non-members pay list price and keep the delivery fee")
    void nonMemberPaysListPrice() {
        User user = fixtures.newUser();

        CheckoutResult result = benefitApplicationService.priceCart(new CheckoutContext(
                user.getId(),
                List.of(new CartItem("SKU-1", "ELECTRONICS", new BigDecimal("1000.00"), 1)),
                new BigDecimal("49.00")));

        assertThat(result.membershipTier()).isNull();
        assertThat(result.appliedBenefits()).isEmpty();
        assertThat(result.totalPayable()).isEqualByComparingTo("1049.00");
    }

    @Test
    @DisplayName("Silver applies its 3% discount but only waives delivery above the threshold")
    void silverBenefitsRespectThresholds() {
        User user = fixtures.newUser();
        subscriptionService.subscribe(user.getId(), "MONTHLY", "SILVER", null);

        CheckoutResult belowThreshold = benefitApplicationService.priceCart(new CheckoutContext(
                user.getId(),
                List.of(new CartItem("SKU-1", "GROCERY", new BigDecimal("400.00"), 1)),
                new BigDecimal("49.00")));

        assertThat(belowThreshold.itemDiscount()).isEqualByComparingTo("12.00");
        assertThat(belowThreshold.deliveryFee()).isEqualByComparingTo("49.00");
        assertThat(belowThreshold.totalPayable()).isEqualByComparingTo("437.00");

        CheckoutResult aboveThreshold = benefitApplicationService.priceCart(new CheckoutContext(
                user.getId(),
                List.of(new CartItem("SKU-2", "GROCERY", new BigDecimal("1000.00"), 1)),
                new BigDecimal("49.00")));

        assertThat(aboveThreshold.deliveryFee()).isEqualByComparingTo("0.00");
        assertThat(aboveThreshold.totalSavings()).isEqualByComparingTo("79.00");
    }

    @Test
    @DisplayName("Gold inherits Silver's benefit set but its own discount overrides")
    void goldInheritsAndOverrides() {
        User user = fixtures.newUser();
        fixtures.placeOrders(user, 2, "1000.00");
        subscriptionService.subscribe(user.getId(), "MONTHLY", "GOLD", null);

        CheckoutResult result = benefitApplicationService.priceCart(new CheckoutContext(
                user.getId(),
                List.of(new CartItem("SKU-1", "FASHION", new BigDecimal("2000.00"), 1)),
                new BigDecimal("49.00")));

        assertThat(result.itemDiscount()).isEqualByComparingTo("200.00");
        assertThat(result.deliveryFee()).isEqualByComparingTo("0.00");
        assertThat(result.totalPayable()).isEqualByComparingTo("1800.00");
        assertThat(result.membershipTier()).isEqualTo("GOLD");
    }

    @Test
    @DisplayName("percentage discounts honour their configured cap")
    void discountCapApplies() {
        User user = fixtures.newUser();
        fixtures.placeOrders(user, 2, "1000.00");
        subscriptionService.subscribe(user.getId(), "MONTHLY", "GOLD", null);

        CheckoutResult result = benefitApplicationService.priceCart(new CheckoutContext(
                user.getId(),
                List.of(new CartItem("SKU-1", "ELECTRONICS", new BigDecimal("100000.00"), 1)),
                BigDecimal.ZERO));

        assertThat(result.itemDiscount()).isEqualByComparingTo("500.00");
    }
}
