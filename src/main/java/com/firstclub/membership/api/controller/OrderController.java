package com.firstclub.membership.api.controller;

import com.firstclub.membership.api.dto.CheckoutDtos;
import com.firstclub.membership.domain.model.OrderRecord;
import com.firstclub.membership.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement, which is what drives tier progression")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place an order",
            description = "Triggers asynchronous tier re-evaluation once the order commits.")
    public ResponseEntity<CheckoutDtos.OrderResponse> place(
            @Valid @RequestBody CheckoutDtos.PlaceOrderRequest request) {

        OrderRecord order = orderService.placeOrder(request.userId(), request.totalAmount(), request.placedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CheckoutDtos.OrderResponse(
                order.getId(), order.getUser().getId(), order.getTotalAmount(), order.getPlacedAt()));
    }
}
