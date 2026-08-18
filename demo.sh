#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE:-http://localhost:8080/api/v1}"
JQ=$(command -v jq || true)

pretty() { if [ -n "$JQ" ]; then jq .; else cat; fi; }
step()   { printf "\n\033[1;36m== %s\033[0m\n" "$1"; }

step "1. Available plans"
curl -s "$BASE/plans" | pretty

step "2. Tiers and benefits"
curl -s "$BASE/tiers" | pretty

step "3. Demo users"
curl -s "$BASE/users" | pretty

step "4. Check Aarav's tier eligibility"
curl -s "$BASE/users/1/tier-eligibility" | pretty

step "5. Subscribe Aarav to Monthly / Silver"
curl -s -X POST "$BASE/subscriptions" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-aarav-001' \
  -d '{"userId":1,"planCode":"MONTHLY","tierCode":"SILVER"}' | pretty

step "6. Retry the same request with the same idempotency key"
curl -s -X POST "$BASE/subscriptions" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-aarav-001' \
  -d '{"userId":1,"planCode":"MONTHLY","tierCode":"SILVER"}' | pretty

step "7. Check current membership"
curl -s "$BASE/users/1/membership" | pretty

step "8. Checkout a cart below the free-delivery threshold"
curl -s -X POST "$BASE/checkout/preview" \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"deliveryFee":49,"items":[{"sku":"SKU-1","category":"GROCERY","unitPrice":400,"quantity":1}]}' | pretty

step "9. Checkout a cart above the free-delivery threshold"
curl -s -X POST "$BASE/checkout/preview" \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"deliveryFee":49,"items":[{"sku":"SKU-2","category":"GROCERY","unitPrice":1000,"quantity":1}]}' | pretty

step "10. Try to upgrade to Gold before it is unlocked"
curl -s -X POST "$BASE/subscriptions/1/upgrade" \
  -H 'Content-Type: application/json' \
  -d '{"tierCode":"GOLD"}' | pretty

step "11. Place orders for Aarav"
for i in 1 2 3; do
  curl -s -X POST "$BASE/orders" -H 'Content-Type: application/json' \
    -d '{"userId":1,"totalAmount":2000}' | pretty
done

step "12. Check Gold eligibility"
curl -s "$BASE/users/1/tier-eligibility" | pretty

step "13. Run the tier evaluation sweep"
curl -s -X POST "$BASE/admin/sweeps/tier-evaluation" | pretty
curl -s "$BASE/users/1/membership" | pretty

step "14. Checkout as a Gold member"
curl -s -X POST "$BASE/checkout/preview" \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"deliveryFee":49,"items":[{"sku":"SKU-3","category":"ELECTRONICS","unitPrice":5000,"quantity":1}]}' | pretty

step "15. Update the Gold discount"
curl -s -X PUT "$BASE/admin/tiers/GOLD/benefits" \
  -H 'Content-Type: application/json' \
  -d '{"type":"EXTRA_DISCOUNT","description":"9% member discount","config":{"percentage":"9","maxDiscount":"900"}}' | pretty

step "16. Checkout with the new benefit configuration"
curl -s -X POST "$BASE/checkout/preview" \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"deliveryFee":49,"items":[{"sku":"SKU-3","category":"ELECTRONICS","unitPrice":5000,"quantity":1}]}' | pretty

step "17. Schedule a downgrade to Silver"
curl -s -X POST "$BASE/subscriptions/1/downgrade" \
  -H 'Content-Type: application/json' -d '{"tierCode":"SILVER"}' | pretty

step "18. Cancel at the end of the billing period"
curl -s -X POST "$BASE/subscriptions/1/cancel" \
  -H 'Content-Type: application/json' -d '{"immediate":false}' | pretty

step "19. View the membership history"
curl -s "$BASE/subscriptions/1/events" | pretty

step "20. Check Ishita's cohort-based eligibility"
curl -s "$BASE/users/4/tier-eligibility" | pretty

printf "\n\033[1;32mDemo complete.\033[0m\n"
