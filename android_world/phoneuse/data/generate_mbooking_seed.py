#!/usr/bin/env python3
"""Generate mBooking hotel seed data (deterministic)."""

import json
import random
from pathlib import Path

SEED = 42
OUTPUT = Path(__file__).parent / "apps" / "mbooking" / "seed.json"

NEIGHBORHOODS = [
    "Midtown", "Times Square", "Upper West Side", "Upper East Side",
    "SoHo", "Chelsea", "Greenwich Village", "Brooklyn Heights",
    "Williamsburg", "Financial District",
]

HOTEL_PREFIXES = [
    "The", "Grand", "Royal", "Park", "City", "Metro", "Harbor",
    "Skyline", "Boutique", "Premier",
]

HOTEL_SUFFIXES = [
    "Hotel", "Inn", "Suites", "Lodge", "Plaza", "Resort",
    "Residence", "Place", "Tower", "Court",
]

AMENITIES_POOL = [
    "WiFi", "Pool", "Gym", "Spa", "Restaurant", "Bar",
    "Breakfast", "Parking", "Concierge", "Room Service",
    "Business Center", "Laundry", "Airport Shuttle", "Pet Friendly",
]

ROOM_TYPES_POOL = [
    "Standard Double", "King Suite", "Deluxe Queen", "Twin Room",
    "Executive Suite", "Family Room", "Penthouse", "Studio",
]


def generate():
    rng = random.Random(SEED)

    hotels = []
    for i in range(1, 81):
        neighborhood = NEIGHBORHOODS[(i - 1) % len(NEIGHBORHOODS)]
        prefix = HOTEL_PREFIXES[(i - 1) % len(HOTEL_PREFIXES)]
        suffix = HOTEL_SUFFIXES[((i - 1) // len(HOTEL_PREFIXES)) % len(HOTEL_SUFFIXES)]
        name = f"{prefix} {neighborhood} {suffix}"

        star_rating = rng.choice([3, 3, 4, 4, 4, 5])
        base_price = {3: 120, 4: 220, 5: 380}[star_rating]
        price = base_price + rng.randint(-30, 50)

        n_amenities = rng.randint(3, 8)
        amenities = rng.sample(AMENITIES_POOL, min(n_amenities, len(AMENITIES_POOL)))

        n_room_types = rng.randint(2, 5)
        room_types = rng.sample(ROOM_TYPES_POOL, min(n_room_types, len(ROOM_TYPES_POOL)))

        # Generate available dates in March 2026
        n_dates = rng.randint(4, 8)
        base_day = 10
        days = sorted(rng.sample(range(base_day, base_day + 20), min(n_dates, 20)))
        available_dates = [f"2026-03-{d:02d}" for d in days]

        hotels.append({
            "id": i,
            "name": name,
            "neighborhood": neighborhood,
            "city": "New York",
            "star_rating": star_rating,
            "price_per_night": price,
            "amenities": amenities,
            "room_types": room_types,
            "available_dates": available_dates,
        })

    data = {
        "hotels": hotels,
        "reservations": [],
    }

    OUTPUT.write_text(json.dumps(data, indent=2) + "\n")
    print(f"Generated {len(hotels)} hotels -> {OUTPUT}")


if __name__ == "__main__":
    generate()
