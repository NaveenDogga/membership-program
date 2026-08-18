package com.firstclub.membership.benefit;

import java.math.BigDecimal;

public record CartItem(String sku, String category, BigDecimal unitPrice, int quantity) {

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
