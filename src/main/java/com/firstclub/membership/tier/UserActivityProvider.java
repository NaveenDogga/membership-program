package com.firstclub.membership.tier;

import com.firstclub.membership.domain.enums.OrderStatus;
import com.firstclub.membership.domain.model.User;
import com.firstclub.membership.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class UserActivityProvider {

    private final OrderRepository orderRepository;

    public UserActivity forUser(User user) {
        return new MemoizingUserActivity(user, orderRepository);
    }

    private static final class MemoizingUserActivity implements UserActivity {

        private final User user;
        private final OrderRepository orderRepository;
        private final Map<Instant, Long> countCache = new ConcurrentHashMap<>();
        private final Map<Instant, BigDecimal> valueCache = new ConcurrentHashMap<>();

        private MemoizingUserActivity(User user, OrderRepository orderRepository) {
            this.user = user;
            this.orderRepository = orderRepository;
        }

        @Override
        public Long userId() {
            return user.getId();
        }

        @Override
        public long orderCountSince(Instant since) {
            return countCache.computeIfAbsent(since,
                    key -> orderRepository.countOrdersSince(user.getId(), key, OrderStatus.CANCELLED));
        }

        @Override
        public BigDecimal orderValueSince(Instant since) {
            return valueCache.computeIfAbsent(since,
                    key -> orderRepository.sumOrderValueSince(user.getId(), key, OrderStatus.CANCELLED));
        }

        @Override
        public Set<String> cohorts() {
            return Set.copyOf(user.getCohorts());
        }
    }
}
