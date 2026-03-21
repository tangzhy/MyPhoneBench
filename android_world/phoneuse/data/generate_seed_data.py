#!/usr/bin/env python3
"""Generate expanded mZocdoc seed data (80 doctors across 10 specialties, 8 hospitals).

Uses a fixed random seed (42) for full reproducibility.

WARNING: The committed e2e_mzocdoc_seed.json is the canonical benchmark data.
Re-running this script will overwrite it. Use --force to confirm.
"""

import json
import random
import sys
from pathlib import Path

# Keep first 5 doctors exactly as original (backward compatibility)
ORIGINAL_DOCTORS = [
    {
        "id": 1,
        "name": "Dr. Sarah Smith",
        "specialty": "Dermatology",
        "hospital": "NYC Medical Center",
        "city": "New York",
        "rating": 4.8,
        "available_slots": [
            "2026-02-11 09:00", "2026-02-11 10:00", "2026-02-11 14:00",
            "2026-02-12 09:00", "2026-02-12 11:00",
            "2026-02-13 09:00", "2026-02-13 14:00",
        ],
    },
    {
        "id": 2,
        "name": "Dr. Michael Johnson",
        "specialty": "General Practice",
        "hospital": "Manhattan Health Clinic",
        "city": "New York",
        "rating": 4.6,
        "available_slots": [
            "2026-02-11 08:00", "2026-02-11 11:00", "2026-02-11 15:00",
            "2026-02-12 10:00", "2026-02-12 14:00",
        ],
    },
    {
        "id": 3,
        "name": "Dr. Emily Chen",
        "specialty": "Cardiology",
        "hospital": "NYC Medical Center",
        "city": "New York",
        "rating": 4.9,
        "available_slots": [
            "2026-02-11 13:00", "2026-02-12 08:00",
            "2026-02-12 13:00", "2026-02-13 10:00",
        ],
    },
    {
        "id": 4,
        "name": "Dr. James Wilson",
        "specialty": "Pediatrics",
        "hospital": "Children's Hospital NYC",
        "city": "New York",
        "rating": 4.7,
        "available_slots": [
            "2026-02-11 09:00", "2026-02-11 14:00",
            "2026-02-12 09:00", "2026-02-12 15:00",
        ],
    },
    {
        "id": 5,
        "name": "Dr. Lisa Anderson",
        "specialty": "Dermatology",
        "hospital": "Manhattan Health Clinic",
        "city": "New York",
        "rating": 4.5,
        "available_slots": [
            "2026-02-11 10:00", "2026-02-11 15:00",
            "2026-02-12 11:00", "2026-02-13 09:00",
        ],
    },
]

SPECIALTIES = [
    "Dermatology", "General Practice", "Cardiology", "Pediatrics",
    "Orthopedics", "Neurology", "Ophthalmology", "Psychiatry",
    "ENT", "Gynecology",
]

HOSPITALS = [
    "NYC Medical Center", "Manhattan Health Clinic", "Children's Hospital NYC",
    "Brooklyn General Hospital", "Queens Medical Center", "Bronx Health Center",
    "NYU Langone", "Mount Sinai",
]

FIRST_NAMES = [
    "Robert", "Maria", "David", "Jennifer", "William", "Linda",
    "Richard", "Patricia", "Joseph", "Elizabeth", "Thomas", "Barbara",
    "Charles", "Susan", "Christopher", "Jessica", "Daniel", "Sarah",
    "Matthew", "Karen", "Anthony", "Nancy", "Mark", "Betty",
    "Donald", "Margaret", "Steven", "Sandra", "Paul", "Ashley",
    "Andrew", "Dorothy", "Joshua", "Kimberly", "Kenneth", "Emily",
    "Kevin", "Donna", "Brian", "Michelle", "George", "Carol",
    "Timothy", "Amanda", "Ronald", "Melissa", "Edward", "Deborah",
    "Jason", "Stephanie", "Jeffrey", "Rebecca", "Ryan", "Sharon",
    "Jacob", "Laura", "Gary", "Cynthia", "Nicholas", "Kathleen",
    "Eric", "Amy", "Jonathan", "Angela", "Stephen", "Shirley",
    "Larry", "Anna", "Justin", "Brenda", "Scott", "Pamela",
    "Brandon", "Emma", "Benjamin", "Nicole", "Samuel", "Helen",
]

LAST_NAMES = [
    "Garcia", "Martinez", "Rodriguez", "Lopez", "Hernandez",
    "Gonzalez", "Perez", "Taylor", "Brown", "Davis",
    "Miller", "Moore", "Jackson", "Martin", "Lee",
    "Thompson", "White", "Harris", "Clark", "Lewis",
    "Robinson", "Walker", "Young", "Allen", "King",
    "Wright", "Scott", "Torres", "Nguyen", "Hill",
    "Flores", "Green", "Adams", "Nelson", "Baker",
    "Hall", "Rivera", "Campbell", "Mitchell", "Carter",
    "Roberts", "Gomez", "Phillips", "Evans", "Turner",
    "Diaz", "Parker", "Cruz", "Edwards", "Collins",
    "Reyes", "Stewart", "Morris", "Morales", "Murphy",
    "Cook", "Rogers", "Morgan", "Peterson", "Cooper",
    "Reed", "Bailey", "Bell", "Howard", "Ward",
    "Cox", "Richardson", "Wood", "Watson", "Brooks",
    "Bennett", "Gray", "James", "Ramirez", "Kim",
]

TIME_SLOTS = [
    "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
    "11:00", "11:30", "13:00", "13:30", "14:00", "14:30",
    "15:00", "15:30", "16:00", "16:30",
]

DATES = [f"2026-02-{d:02d}" for d in range(11, 21)]  # Feb 11-20


def generate_slots(rng: random.Random, n_slots: int) -> list[str]:
    """Generate random available time slots."""
    slots = set()
    while len(slots) < n_slots:
        date = rng.choice(DATES)
        time = rng.choice(TIME_SLOTS)
        slots.add(f"{date} {time}")
    return sorted(slots)


def generate_doctors() -> list[dict]:
    """Generate 80 doctors (5 original + 75 new)."""
    rng = random.Random(42)  # Deterministic for reproducibility
    doctors = list(ORIGINAL_DOCTORS)

    used_names = {d["name"] for d in doctors}

    # Distribute specialties evenly: 8 doctors per specialty (80 total)
    # First 5 cover Dermatology(2), General Practice(1), Cardiology(1), Pediatrics(1)
    # Remaining 75 fill the rest
    specialty_counts = {s: 0 for s in SPECIALTIES}
    for d in doctors:
        specialty_counts[d["specialty"]] += 1

    doctor_id = 6
    for specialty in SPECIALTIES:
        target = 8
        while specialty_counts[specialty] < target and doctor_id <= 80:
            # Pick unique name
            while True:
                first = rng.choice(FIRST_NAMES)
                last = rng.choice(LAST_NAMES)
                name = f"Dr. {first} {last}"
                if name not in used_names:
                    used_names.add(name)
                    break

            hospital = rng.choice(HOSPITALS)
            rating = round(rng.uniform(3.5, 5.0), 1)
            n_slots = rng.randint(3, 8)
            slots = generate_slots(rng, n_slots)

            doctors.append({
                "id": doctor_id,
                "name": name,
                "specialty": specialty,
                "hospital": hospital,
                "city": "New York",
                "rating": rating,
                "available_slots": slots,
            })
            doctor_id += 1
            specialty_counts[specialty] += 1

    return doctors


def main():
    # Safety guard: committed JSON is canonical benchmark data
    output_path = Path(__file__).parent / "e2e_mzocdoc_seed.json"
    if output_path.exists():
        if "--force" not in sys.argv:
            print(
                "ERROR: e2e_mzocdoc_seed.json already exists.\n"
                "This file is the canonical benchmark seed data.\n"
                "Re-running will overwrite it. Use --force to confirm."
            )
            sys.exit(1)

    doctors = generate_doctors()

    seed_data = {
        "doctors": doctors,
        "appointments": [],
    }

    output_path = Path(__file__).parent / "e2e_mzocdoc_seed.json"
    with open(output_path, "w", encoding='utf-8') as f:
        json.dump(seed_data, f, indent=2, ensure_ascii=False)

    print(f"Generated {len(doctors)} doctors across "
          f"{len(set(d['specialty'] for d in doctors))} specialties and "
          f"{len(set(d['hospital'] for d in doctors))} hospitals")
    print(f"Saved to {output_path}")

    # Print distribution
    from collections import Counter
    spec_count = Counter(d["specialty"] for d in doctors)
    hosp_count = Counter(d["hospital"] for d in doctors)
    print("\nSpecialty distribution:")
    for s, c in sorted(spec_count.items()):
        print(f"  {s}: {c}")
    print("\nHospital distribution:")
    for h, c in sorted(hosp_count.items()):
        print(f"  {h}: {c}")


if __name__ == "__main__":
    main()
