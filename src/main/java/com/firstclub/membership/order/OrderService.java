package com.firstclub.membership.order;

import com.firstclub.membership.common.Money;
import com.firstclub.membership.common.exception.ResourceNotFoundException;
import com.firstclub.membership.domain.enums.OrderStatus;
import com.firstclub.membership.domain.model.OrderRecord;
import com.firstclub.membership.domain.model.User;
import com.firstclub.membership.domain.repository.OrderRepository;
import com.firstclub.membership.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderRecord placeOrder(Long userId, BigDecimal totalAmount, Instant placedAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        OrderRecord order = orderRepository.save(OrderRecord.builder()
                .user(user)
                .totalAmount(Money.normalize(totalAmount))
                .status(OrderStatus.PLACED)
                .placedAt(placedAt == null ? Instant.now() : placedAt)
                .build());

        eventPublisher.publishEvent(new OrderPlacedEvent(userId, order.getId()));
        return order;
    }
}
