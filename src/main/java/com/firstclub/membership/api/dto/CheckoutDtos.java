package com.firstclub.membership.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CheckoutDtos {

    private CheckoutDtos() {
    }

    public record CartItemRequest(
            @NotNull String sku,
            String category,
            @NotNull @PositiveOrZero BigDecimal unitPrice,
            @Positive int quantity
    ) {
    }

    public record CheckoutPreviewRequest(
            @NotNull Long userId,
            @NotEmpty @Valid List<CartItemRequest> items,
            @PositiveOrZero BigDecimal deliveryFee
    ) {
        public BigDecimal deliveryFeeOrZero() {
            return deliveryFee == null ? BigDecimal.ZERO : deliveryFee;
        }
    }

    public record PlaceOrderRequest(
            @NotNull Long userId,
            @NotNull @Positive BigDecimal totalAmount,
            Instant placedAt
    ) {
    }

    public record OrderResponse(Long orderId, Long userId, BigDecimal totalAmount, Instant placedAt) {
    }
}
