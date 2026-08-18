package com.firstclub.membership.benefit;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutContext(Long userId, List<CartItem> items, BigDecimal deliveryFee) {

    public BigDecimal subtotal() {
        return items.stream()
                .map(CartItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
