package com.firstclub.membership.benefit.handler;

import com.firstclub.membership.benefit.CartItem;
import com.firstclub.membership.benefit.CheckoutComputation;
import com.firstclub.membership.benefit.CheckoutContext;
import com.firstclub.membership.benefit.EffectiveBenefit;
import com.firstclub.membership.common.Money;
import com.firstclub.membership.domain.enums.BenefitType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ExtraDiscountBenefitHandler implements BenefitHandler {

    @Override
    public Set<BenefitType> supportedTypes() {
        return Set.of(BenefitType.EXTRA_DISCOUNT);
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public void apply(EffectiveBenefit benefit, CheckoutContext context, CheckoutComputation computation) {
        BigDecimal percentage = benefit.decimal("percentage", BigDecimal.ZERO);
        if (percentage.signum() <= 0) {
            return;
        }
        BigDecimal minOrderValue = benefit.decimal("minOrderValue", BigDecimal.ZERO);
        if (computation.subtotal().compareTo(minOrderValue) < 0) {
            return;
        }

        Set<String> categories = csv(benefit.string("categories", ""));
        Set<String> skus = csv(benefit.string("skus", ""));

        BigDecimal eligibleValue = context.items().stream()
                .filter(item -> matches(item, categories, skus))
                .map(CartItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (eligibleValue.signum() <= 0) {
            return;
        }

        BigDecimal discount = Money.percentageOf(eligibleValue, percentage);
        BigDecimal cap = benefit.decimal("maxDiscount", null);
        if (cap != null && cap.signum() > 0) {
            discount = Money.min(discount, cap);
        }

        computation.addItemDiscount(BenefitType.EXTRA_DISCOUNT, benefit.sourceTierCode(),
                percentage.stripTrailingZeros().toPlainString() + "% member discount"
                        + (categories.isEmpty() && skus.isEmpty() ? "" : " on eligible items"),
                discount);
    }

    private boolean matches(CartItem item, Set<String> categories, Set<String> skus) {
        if (categories.isEmpty() && skus.isEmpty()) {
            return true;
        }
        boolean categoryMatch = item.category() != null
                && categories.contains(item.category().toUpperCase(Locale.ROOT));
        boolean skuMatch = item.sku() != null && skus.contains(item.sku().toUpperCase(Locale.ROOT));
        return categoryMatch || skuMatch;
    }

    private Set<String> csv(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
