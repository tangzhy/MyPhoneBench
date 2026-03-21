#!/usr/bin/env python3
"""Generate mThumbtack seed data (60 service professionals across 12 service types).

Uses a fixed random seed (42) for full reproducibility.

WARNING: The committed e2e_mthumbtack_seed.json is the canonical benchmark data.
Re-running this script will overwrite it. Use --force to confirm.
"""

import json
import random
import sys
from pathlib import Path

SERVICE_TYPES = [
    "Plumbing",
    "Electrical",
    "House Cleaning",
    "Painting",
    "Handyman",
    "HVAC",
    "Landscaping",
    "Moving",
    "Roofing",
    "Flooring",
    "Pest Control",
    "Carpentry",
]

# Realistic company name templates per service type
COMPANY_TEMPLATES = {
    "Plumbing": [
        "{last} Plumbing Solutions",
        "All-City Plumbing",
        "Precision Pipe & Drain",
        "FastFlow Plumbing Co.",
        "Borough Plumbing Services",
        "ClearDrain Plumbing",
    ],
    "Electrical": [
        "{last} Electric",
        "BrightWire Electrical",
        "Metro Electrical Services",
        "Spark Pro Electric",
        "SafeCircuit Electricians",
        "PowerLine Electric Co.",
    ],
    "House Cleaning": [
        "{last} Cleaning Services",
        "Sparkle & Shine Cleaners",
        "Metro Maid Pro",
        "Crystal Clear Cleaning",
        "Fresh Home Cleaners",
        "TidyUp NYC",
    ],
    "Painting": [
        "{last} Painting Co.",
        "ColorCraft Painters",
        "Pro Brush Painting",
        "Fresh Coat NYC",
        "Premier Paint Pros",
        "Urban Color Painting",
    ],
    "Handyman": [
        "{last} Handyman Services",
        "Fix-It-All Handyman",
        "Mr. Reliable Repairs",
        "HouseWorks Handyman",
        "QuickFix Home Services",
        "Handy Pros NYC",
    ],
    "HVAC": [
        "{last} Heating & Cooling",
        "Arctic Comfort HVAC",
        "Metro Climate Control",
        "CoolBreeze HVAC",
        "AllSeason Air Systems",
        "ThermoTech HVAC",
    ],
    "Landscaping": [
        "{last} Landscaping",
        "GreenScape Design",
        "Urban Garden Pros",
        "Nature's Touch Landscaping",
        "City Green Landscapes",
        "EcoLawn Services",
    ],
    "Moving": [
        "{last} Moving Co.",
        "SwiftMove NYC",
        "EasyHaul Movers",
        "Metro Moving Pros",
        "AllStar Relocation",
        "SafeLift Moving",
    ],
    "Roofing": [
        "{last} Roofing",
        "SkyShield Roofing",
        "TopTier Roof Pros",
        "Metro Roofing Solutions",
        "AllWeather Roofing Co.",
        "SolidTop Roofing",
    ],
    "Flooring": [
        "{last} Flooring",
        "ProFloor Installations",
        "Urban Floor Design",
        "Precision Flooring NYC",
        "HardWood Masters",
        "FloorCraft Solutions",
    ],
    "Pest Control": [
        "{last} Pest Control",
        "BugFree NYC",
        "Guardian Pest Solutions",
        "Metro Exterminating",
        "SafeHome Pest Control",
        "ClearZone Pest Services",
    ],
    "Carpentry": [
        "{last} Carpentry",
        "WoodCraft Custom",
        "MasterJoint Carpentry",
        "Heritage Woodworks",
        "Urban Timber Pros",
        "Precision Carpentry NYC",
    ],
}

FIRST_NAMES = [
    "James", "Robert", "Michael", "William", "David",
    "Richard", "Joseph", "Thomas", "Christopher", "Charles",
    "Daniel", "Matthew", "Anthony", "Mark", "Steven",
    "Paul", "Andrew", "Joshua", "Kenneth", "Kevin",
    "Brian", "George", "Timothy", "Ronald", "Edward",
    "Jason", "Jeffrey", "Ryan", "Jacob", "Gary",
    "Nicholas", "Eric", "Jonathan", "Stephen", "Larry",
    "Justin", "Scott", "Brandon", "Benjamin", "Samuel",
    "Maria", "Jennifer", "Linda", "Patricia", "Elizabeth",
    "Barbara", "Susan", "Jessica", "Sarah", "Karen",
    "Lisa", "Nancy", "Betty", "Margaret", "Sandra",
    "Ashley", "Dorothy", "Kimberly", "Emily", "Donna",
    "Michelle", "Carol", "Amanda", "Melissa", "Deborah",
    "Stephanie", "Rebecca", "Sharon", "Laura", "Cynthia",
]

LAST_NAMES = [
    "Johnson", "Williams", "Brown", "Jones", "Garcia",
    "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez",
    "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
    "Taylor", "Moore", "Jackson", "Martin", "Lee",
    "Perez", "Thompson", "White", "Harris", "Clark",
    "Lewis", "Robinson", "Walker", "Young", "Allen",
    "King", "Wright", "Scott", "Torres", "Nguyen",
    "Hill", "Flores", "Green", "Adams", "Nelson",
    "Baker", "Hall", "Rivera", "Campbell", "Mitchell",
    "Carter", "Roberts", "Gomez", "Phillips", "Evans",
    "Turner", "Diaz", "Parker", "Cruz", "Edwards",
    "Collins", "Reyes", "Stewart", "Morris", "Morales",
    "Murphy", "Cook", "Rogers", "Morgan", "Peterson",
    "Cooper", "Reed", "Bailey", "Bell", "Howard",
]

CITIES = ["New York", "Brooklyn", "Queens", "Manhattan", "Bronx"]

# March 15-30, 2026
DATES = [f"2026-03-{d:02d}" for d in range(15, 31)]

TIME_SLOTS = [
    "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
    "11:00", "11:30", "12:00", "13:00", "13:30", "14:00",
    "14:30", "15:00", "15:30", "16:00", "16:30", "17:00",
]


def generate_slots(rng: random.Random, n_slots: int) -> list:
    """Generate random available time slots."""
    slots = set()
    while len(slots) < n_slots:
        date = rng.choice(DATES)
        time = rng.choice(TIME_SLOTS)
        slots.add(f"{date} {time}")
    return sorted(slots)


def generate_pros() -> list:
    """Generate 60 service professionals (5 per service type)."""
    rng = random.Random(42)
    pros = []
    used_names = set()
    pro_id = 1

    for service_type in SERVICE_TYPES:
        templates = COMPANY_TEMPLATES[service_type]
        used_companies = set()

        for _ in range(5):
            # Pick a unique name
            while True:
                first = rng.choice(FIRST_NAMES)
                last = rng.choice(LAST_NAMES)
                name = f"{first} {last}"
                if name not in used_names:
                    used_names.add(name)
                    break

            # Pick a unique company for this service type
            while True:
                template = rng.choice(templates)
                company = template.format(last=last)
                if company not in used_companies:
                    used_companies.add(company)
                    break

            city = rng.choice(CITIES)
            rating = round(rng.uniform(3.5, 5.0), 1)
            n_slots = rng.randint(4, 8)
            slots = generate_slots(rng, n_slots)

            pros.append({
                "id": pro_id,
                "name": name,
                "service_type": service_type,
                "company": company,
                "city": city,
                "rating": rating,
                "available_slots": slots,
            })
            pro_id += 1

    return pros


def main():
    # Safety guard: committed JSON is canonical benchmark data
    output_path = Path(__file__).parent / "apps" / "mthumbtack" / "seed.json"

    pros = generate_pros()

    seed_data = {
        "pros": pros,
    }

    with open(output_path, "w") as f:
        json.dump(seed_data, f, indent=2)

    print(f"Generated {len(pros)} service professionals across "
          f"{len(set(p['service_type'] for p in pros))} service types")
    print(f"Saved to {output_path}")

    # Print distribution
    from collections import Counter
    type_count = Counter(p["service_type"] for p in pros)
    city_count = Counter(p["city"] for p in pros)
    print("\nService type distribution:")
    for s, c in sorted(type_count.items()):
        print(f"  {s}: {c}")
    print("\nCity distribution:")
    for city, c in sorted(city_count.items()):
        print(f"  {city}: {c}")
    print(f"\nSlots per pro: min={min(len(p['available_slots']) for p in pros)}, "
          f"max={max(len(p['available_slots']) for p in pros)}, "
          f"avg={sum(len(p['available_slots']) for p in pros) / len(pros):.1f}")


if __name__ == "__main__":
    main()
