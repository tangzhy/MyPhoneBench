#!/usr/bin/env python3
"""Generate deterministic seed data for mGeico insurance plans."""

import json
import os
import random

SEED = 42
OUTPUT_PATH = os.path.join(
    os.path.dirname(__file__), "apps", "mgeico", "seed.json"
)

COVERAGE_TYPES = [
    "Liability Only",
    "Basic",
    "Standard",
    "Premium",
    "Comprehensive",
]

PROVIDERS = [
    "GEICO", "State Farm", "Progressive", "Allstate", "USAA",
    "Liberty Mutual", "Nationwide", "Farmers", "Travelers", "American Family",
]

COVERAGE_LIMITS = {
    "Liability Only": ["15/30/15", "25/50/25", "50/100/50"],
    "Basic": ["25/50/25", "50/100/50"],
    "Standard": ["50/100/50", "100/300/100"],
    "Premium": ["100/300/100", "250/500/250"],
    "Comprehensive": ["100/300/100", "250/500/250", "500/500/500"],
}

DEDUCTIBLES = {
    "Liability Only": [0],
    "Basic": [500, 1000],
    "Standard": [250, 500, 1000],
    "Premium": [250, 500],
    "Comprehensive": [250, 500],
}

BASE_MONTHLY = {
    "Liability Only": (45, 85),
    "Basic": (80, 130),
    "Standard": (110, 180),
    "Premium": (160, 250),
    "Comprehensive": (200, 350),
}

PLAN_NAME_TEMPLATES = {
    "Liability Only": [
        "{provider} Liability Shield",
        "{provider} Basic Liability",
        "{provider} Minimum Coverage",
    ],
    "Basic": [
        "{provider} Essentials",
        "{provider} Basic Plus",
        "{provider} Starter Plan",
    ],
    "Standard": [
        "{provider} Standard Coverage",
        "{provider} Value Plan",
        "{provider} Standard Auto",
    ],
    "Premium": [
        "{provider} Premium Shield",
        "{provider} Gold Coverage",
        "{provider} Premium Auto",
    ],
    "Comprehensive": [
        "{provider} Full Coverage",
        "{provider} Platinum Plan",
        "{provider} Total Protection",
    ],
}


def generate_plans(n: int = 50) -> list[dict]:
    rng = random.Random(SEED)
    plans = []
    plan_id = 1

    # Ensure good distribution across types and providers
    type_cycle = COVERAGE_TYPES * (n // len(COVERAGE_TYPES) + 1)
    rng.shuffle(type_cycle)

    for i in range(n):
        ctype = type_cycle[i]
        provider = rng.choice(PROVIDERS)
        templates = PLAN_NAME_TEMPLATES[ctype]
        plan_name = rng.choice(templates).format(provider=provider)

        deductible = rng.choice(DEDUCTIBLES[ctype])
        lo, hi = BASE_MONTHLY[ctype]
        monthly = round(rng.uniform(lo, hi), 2)
        annual = round(monthly * 12, 2)
        limit = rng.choice(COVERAGE_LIMITS[ctype])

        includes_collision = ctype in ("Standard", "Premium", "Comprehensive")
        includes_comprehensive = ctype in ("Premium", "Comprehensive")

        rating = round(rng.uniform(3.5, 5.0), 1)

        plans.append({
            "id": plan_id,
            "plan_name": plan_name,
            "coverage_type": ctype,
            "deductible": deductible,
            "monthly_premium": monthly,
            "annual_premium": annual,
            "coverage_limit": limit,
            "includes_collision": includes_collision,
            "includes_comprehensive": includes_comprehensive,
            "provider": provider,
            "rating": rating,
        })
        plan_id += 1

    return plans


def main():
    plans = generate_plans(50)
    seed_data = {"plans": plans}

    with open(OUTPUT_PATH, "w") as f:
        json.dump(seed_data, f, indent=2)

    print(f"Generated {len(plans)} insurance plans -> {OUTPUT_PATH}")

    # Stats
    by_type = {}
    for p in plans:
        by_type.setdefault(p["coverage_type"], []).append(p)
    for t, ps in sorted(by_type.items()):
        print(f"  {t}: {len(ps)} plans")


if __name__ == "__main__":
    main()
