package com.firstclub.membership.api.controller;

import com.firstclub.membership.api.dto.CheckoutDtos;
import com.firstclub.membership.benefit.BenefitApplicationService;
import com.firstclub.membership.benefit.CartItem;
import com.firstclub.membership.benefit.CheckoutContext;
import com.firstclub.membership.benefit.CheckoutResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout", description = "Price a cart with the user's membership applied")
public class CheckoutController {

    private final BenefitApplicationService benefitApplicationService;

    @PostMapping("/preview")
    @Operation(summary = "Apply membership benefits to a cart",
            description = "Safe to call for non-members: they simply pay list price.")
    public ResponseEntity<CheckoutResult> preview(
            @Valid @RequestBody CheckoutDtos.CheckoutPreviewRequest request) {

        CheckoutContext context = new CheckoutContext(
                request.userId(),
                request.items().stream()
                        .map(item -> new CartItem(item.sku(), item.category(), item.unitPrice(), item.quantity()))
                        .toList(),
                request.deliveryFeeOrZero());

        return ResponseEntity.ok(benefitApplicationService.priceCart(context));
    }
}
