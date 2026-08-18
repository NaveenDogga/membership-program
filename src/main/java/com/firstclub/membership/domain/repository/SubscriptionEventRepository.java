package com.firstclub.membership.domain.repository;

import com.firstclub.membership.domain.model.SubscriptionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionEventRepository extends JpaRepository<SubscriptionEvent, Long> {

    List<SubscriptionEvent> findBySubscriptionIdOrderByOccurredAtAsc(Long subscriptionId);
}
