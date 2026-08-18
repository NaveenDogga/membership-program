package com.firstclub.membership.benefit;

import com.firstclub.membership.common.Money;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.domain.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BenefitApplicationService {

    private final SubscriptionRepository subscriptionRepository;
    private final BenefitCatalogService benefitCatalogService;
    private final BenefitHandlerRegistry handlerRegistry;

    @Transactional(readOnly = true)
    public CheckoutResult priceCart(CheckoutContext context) {
        Optional<Subscription> membership = activeMembership(context.userId());

        CheckoutComputation computation = new CheckoutComputation(context.subtotal(), context.deliveryFee());

        membership.ifPresent(subscription -> {
            List<EffectiveBenefit> benefits =
                    benefitCatalogService.benefitsForTier(subscription.getTier().getCode());
            for (EffectiveBenefit benefit : handlerRegistry.inApplicationOrder(benefits)) {
                handlerRegistry.resolve(benefit.type())
                        .ifPresent(handler -> handler.apply(benefit, context, computation));
            }
        });

        return new CheckoutResult(
                context.userId(),
                membership.map(subscription -> subscription.getTier().getCode()).orElse(null),
                membership.map(subscription -> subscription.getPlan().getCode()).orElse(null),
                Money.normalize(computation.subtotal()),
                computation.itemDiscount(),
                computation.deliveryFee(),
                computation.deliveryDiscount(),
                computation.totalSavings(),
                computation.totalPayable(),
                computation.appliedBenefits());
    }

    @Transactional(readOnly = true)
    public List<EffectiveBenefit> activeBenefits(Long userId) {
        return activeMembership(userId)
                .map(subscription -> benefitCatalogService.benefitsForTier(subscription.getTier().getCode()))
                .orElseGet(List::of);
    }

    private Optional<Subscription> activeMembership(Long userId) {
        Instant now = Instant.now();
        return subscriptionRepository.findByActiveUserKey(userId)
                .filter(subscription -> subscription.isLive(now));
    }
}
