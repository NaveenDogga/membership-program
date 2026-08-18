package com.firstclub.membership.tier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public interface UserActivity {

    Long userId();

    long orderCountSince(Instant since);

    BigDecimal orderValueSince(Instant since);

    Set<String> cohorts();
}
