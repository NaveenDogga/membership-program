package com.firstclub.membership.domain.repository;

import com.firstclub.membership.domain.enums.OrderStatus;
import com.firstclub.membership.domain.model.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface OrderRepository extends JpaRepository<OrderRecord, Long> {

    @Query("""
            select count(o) from OrderRecord o
            where o.user.id = :userId
              and o.status <> :excluded
              and o.placedAt >= :since
            """)
    long countOrdersSince(@Param("userId") Long userId,
                          @Param("since") Instant since,
                          @Param("excluded") OrderStatus excluded);

    @Query("""
            select coalesce(sum(o.totalAmount), 0) from OrderRecord o
            where o.user.id = :userId
              and o.status <> :excluded
              and o.placedAt >= :since
            """)
    BigDecimal sumOrderValueSince(@Param("userId") Long userId,
                                  @Param("since") Instant since,
                                  @Param("excluded") OrderStatus excluded);
}
