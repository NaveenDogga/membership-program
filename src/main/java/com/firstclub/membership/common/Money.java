package com.firstclub.membership.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final int SCALE = 2;
    public static final BigDecimal HUNDRED = new BigDecimal("100");

    private Money() {
    }

    public static BigDecimal normalize(BigDecimal value) {
        return value == null ? zero() : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentageOf(BigDecimal base, BigDecimal percentage) {
        return normalize(base.multiply(percentage).divide(HUNDRED, 6, RoundingMode.HALF_UP));
    }

    public static BigDecimal atLeastZero(BigDecimal value) {
        return value.signum() < 0 ? zero() : normalize(value);
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? normalize(a) : normalize(b);
    }
}
