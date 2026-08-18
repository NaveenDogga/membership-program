package com.firstclub.membership.benefit;

import com.firstclub.membership.benefit.handler.BenefitHandler;
import com.firstclub.membership.domain.enums.BenefitType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class BenefitHandlerRegistry {

    private final Map<BenefitType, BenefitHandler> handlers = new EnumMap<>(BenefitType.class);

    public BenefitHandlerRegistry(List<BenefitHandler> discovered) {
        for (BenefitHandler handler : discovered) {
            for (BenefitType type : handler.supportedTypes()) {
                BenefitHandler previous = handlers.put(type, handler);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate BenefitHandler for " + type);
                }
            }
        }
        for (BenefitType type : BenefitType.values()) {
            if (!handlers.containsKey(type)) {
                throw new IllegalStateException("No BenefitHandler registered for " + type);
            }
        }
    }

    public Optional<BenefitHandler> resolve(BenefitType type) {
        return Optional.ofNullable(handlers.get(type));
    }

    public List<EffectiveBenefit> inApplicationOrder(List<EffectiveBenefit> benefits) {
        return benefits.stream()
                .sorted(Comparator
                        .comparingInt((EffectiveBenefit benefit) -> resolve(benefit.type())
                                .map(BenefitHandler::order)
                                .orElse(Integer.MAX_VALUE))
                        .thenComparing(benefit -> benefit.type().name()))
                .toList();
    }
}
