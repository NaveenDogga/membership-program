package com.firstclub.membership.domain.repository;

import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.domain.model.Subscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByActiveUserKey(Long userId);

    List<Subscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Subscription s where s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select s from Subscription s
            where s.status = :status and s.endAt <= :now
            order by s.endAt asc
            """)
    List<Subscription> findDueForLifecycleSweep(@Param("status") SubscriptionStatus status,
                                                @Param("now") Instant now,
                                                Pageable pageable);

    @Query("""
            select s from Subscription s
            where s.status = :status
            order by s.id asc
            """)
    List<Subscription> findActiveForTierEvaluation(@Param("status") SubscriptionStatus status,
                                                   Pageable pageable);
}
