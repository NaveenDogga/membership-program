package com.firstclub.membership.domain.enums;

public enum SubscriptionStatus {
    ACTIVE,
    CANCELLED,
    EXPIRED;

    public boolean isTerminal() {
        return this != ACTIVE;
    }
}
