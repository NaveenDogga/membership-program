package com.firstclub.membership.benefit;

import com.firstclub.membership.common.Money;
import com.firstclub.membership.domain.enums.BenefitType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class CheckoutComputation {

    private final BigDecimal subtotal;
    private BigDecimal itemDiscount = Money.zero();
    private BigDecimal deliveryFee;
    private BigDecimal deliveryDiscount = Money.zero();
    private final List<AppliedBenefit> appliedBenefits = new ArrayList<>();

    public CheckoutComputation(BigDecimal subtotal, BigDecimal deliveryFee) {
        this.subtotal = Money.normalize(subtotal);
        this.deliveryFee = Money.normalize(deliveryFee);
    }

    public BigDecimal subtotal() {
        return subtotal;
    }

    public BigDecimal deliveryFee() {
        return deliveryFee;
    }

    public BigDecimal discountedSubtotal() {
        return Money.atLeastZero(subtotal.subtract(itemDiscount));
    }

    public void addItemDiscount(BenefitType type, String sourceTierCode, String description, BigDecimal amount) {
        BigDecimal capped = Money.min(Money.atLeastZero(amount), discountedSubtotal());
        if (capped.signum() == 0) {
            return;
        }
        this.itemDiscount = Money.normalize(itemDiscount.add(capped));
        appliedBenefits.add(new AppliedBenefit(type, sourceTierCode, description, capped));
    }

    public void waiveDelivery(BenefitType type, String sourceTierCode, String description) {
        if (deliveryFee.signum() == 0) {
            return;
        }
        this.deliveryDiscount = Money.normalize(deliveryDiscount.add(deliveryFee));
        BigDecimal waived = deliveryFee;
        this.deliveryFee = Money.zero();
        appliedBenefits.add(new AppliedBenefit(type, sourceTierCode, description, waived));
    }

    public void addPerk(BenefitType type, String sourceTierCode, String description) {
        appliedBenefits.add(new AppliedBenefit(type, sourceTierCode, description, Money.zero()));
    }

    public BigDecimal itemDiscount() {
        return itemDiscount;
    }

    public BigDecimal deliveryDiscount() {
        return deliveryDiscount;
    }

    public BigDecimal totalPayable() {
        return Money.atLeastZero(discountedSubtotal().add(deliveryFee));
    }

    public List<AppliedBenefit> appliedBenefits() {
        return List.copyOf(appliedBenefits);
    }

    public BigDecimal totalSavings() {
        return Money.normalize(itemDiscount.add(deliveryDiscount));
    }
}
