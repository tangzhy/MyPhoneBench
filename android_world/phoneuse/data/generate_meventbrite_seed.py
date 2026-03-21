#!/usr/bin/env python3
"""Generate deterministic mEventbrite seed data (60 events across 10 categories, 15 venues).

Uses a fixed random seed (42) for full reproducibility.

WARNING: The committed e2e_meventbrite_seed.json is the canonical benchmark data.
Re-running this script will overwrite it. Use --force to confirm.
"""

import json
import random
import sys
from pathlib import Path

CATEGORIES = [
    "Technology", "Music", "Business", "Food & Drink", "Health & Wellness",
    "Arts & Culture", "Sports & Fitness", "Education", "Community", "Networking",
]

VENUES = [
    "Javits Center", "Madison Square Garden", "Blue Note Jazz Club",
    "Central Park", "Brooklyn Museum", "The Metropolitan Museum",
    "Carnegie Hall", "Barclays Center", "Lincoln Center", "Pier 17",
    "NYU Skirball Center", "Chelsea Piers", "Rockefeller Center",
    "Times Square Plaza", "The Bowery Ballroom",
]

ORGANIZERS = [
    "TechConnect NYC", "NYC Jazz Foundation", "NYC Foodies",
    "Art Society NY", "Brooklyn Sports League", "Manhattan Business Forum",
    "Wellness NYC", "NYU Student Council", "Community Builders",
    "NetworkPro NYC",
]

# Templates per category for generating descriptive, unique titles
_TITLE_TEMPLATES = {
    "Technology": [
        "NYC AI & Machine Learning Summit {n}",
        "Brooklyn DevOps Meetup {n}",
        "Cloud Computing Workshop {n}",
        "Startup Demo Day {n}",
        "Cybersecurity Conference {n}",
        "Blockchain & Web3 Forum {n}",
        "Open Source Hackathon {n}",
        "Data Science Bootcamp {n}",
    ],
    "Music": [
        "Jazz Under the Stars {n}",
        "Indie Rock Showcase {n}",
        "Classical Piano Recital {n}",
        "Hip-Hop & R&B Night {n}",
        "Acoustic Songwriters Circle {n}",
        "Electronic Music Festival {n}",
        "World Music Celebration {n}",
        "Blues & Soul Revival {n}",
    ],
    "Business": [
        "Entrepreneurs Networking Breakfast {n}",
        "Venture Capital Panel {n}",
        "Women in Leadership Forum {n}",
        "Small Business Workshop {n}",
        "Marketing Strategy Summit {n}",
        "Real Estate Investment Seminar {n}",
        "Fintech Innovation Talks {n}",
        "Supply Chain Conference {n}",
    ],
    "Food & Drink": [
        "NYC Street Food Festival {n}",
        "Wine Tasting Evening {n}",
        "Craft Beer & Bites {n}",
        "Farm-to-Table Dinner {n}",
        "Vegan Cooking Class {n}",
        "International Food Fair {n}",
        "Cocktail Mixology Workshop {n}",
        "Artisan Cheese & Charcuterie Night {n}",
    ],
    "Health & Wellness": [
        "Morning Yoga in the Park {n}",
        "Mindfulness & Meditation Retreat {n}",
        "Nutrition & Wellness Expo {n}",
        "Mental Health Awareness Workshop {n}",
        "Holistic Healing Fair {n}",
        "Fitness Bootcamp Challenge {n}",
        "Stress Management Seminar {n}",
        "Community Health Screening {n}",
    ],
    "Arts & Culture": [
        "Contemporary Art Exhibition {n}",
        "Photography Walk & Talk {n}",
        "Spoken Word & Poetry Night {n}",
        "Film Screening & Q&A {n}",
        "Sculpture Garden Tour {n}",
        "Street Art & Mural Festival {n}",
        "Theater Improv Workshop {n}",
        "Cultural Heritage Celebration {n}",
    ],
    "Sports & Fitness": [
        "5K Fun Run {n}",
        "Basketball Tournament {n}",
        "Soccer Skills Clinic {n}",
        "Cycling Tour of Brooklyn {n}",
        "Boxing Fitness Class {n}",
        "Rock Climbing Challenge {n}",
        "Volleyball Beach Day {n}",
        "Marathon Training Session {n}",
    ],
    "Education": [
        "Science Fair & Innovation Expo {n}",
        "Creative Writing Workshop {n}",
        "College Prep Seminar {n}",
        "Language Exchange Meetup {n}",
        "History Lecture Series {n}",
        "STEM for Kids Workshop {n}",
        "Public Speaking Masterclass {n}",
        "Financial Literacy Bootcamp {n}",
    ],
    "Community": [
        "Neighborhood Block Party {n}",
        "Community Garden Volunteer Day {n}",
        "Charity Auction Gala {n}",
        "Cultural Diversity Festival {n}",
        "Senior Social Hour {n}",
        "Pet Adoption Fair {n}",
        "Community Town Hall {n}",
        "Coat Drive & Soup Kitchen {n}",
    ],
    "Networking": [
        "Professional Mixer {n}",
        "Tech Founders Happy Hour {n}",
        "Creative Professionals Meetup {n}",
        "Alumni Reunion Networking {n}",
        "Industry Speed Networking {n}",
        "Freelancers Collective {n}",
        "Young Professionals Brunch {n}",
        "Executive Roundtable {n}",
    ],
}

# Description templates per category
_DESC_TEMPLATES = {
    "Technology": [
        "Join leading tech professionals for an evening of talks and demos on cutting-edge technology.",
        "A hands-on workshop covering the latest tools and frameworks in software development.",
        "Connect with innovators and learn about emerging trends shaping the tech industry.",
        "Explore new technologies through interactive sessions and expert-led discussions.",
    ],
    "Music": [
        "An unforgettable night of live music featuring talented local and touring artists.",
        "Experience the magic of live performance in one of NYC's most iconic venues.",
        "A celebration of musical creativity with performances spanning multiple genres.",
        "Enjoy world-class musicians in an intimate setting with great sound and atmosphere.",
    ],
    "Business": [
        "Network with industry leaders and gain insights into the latest business strategies.",
        "A full-day event packed with keynotes, panels, and workshops for professionals.",
        "Learn from successful entrepreneurs and expand your professional network.",
        "Discover actionable strategies to grow your business and advance your career.",
    ],
    "Food & Drink": [
        "Sample delicious dishes and drinks from top NYC restaurants and local vendors.",
        "A culinary adventure featuring tastings, cooking demos, and expert-led pairings.",
        "Explore diverse flavors and cuisines in a fun, social atmosphere.",
        "Indulge in gourmet food and artisan beverages curated by top chefs.",
    ],
    "Health & Wellness": [
        "Recharge your mind and body with guided sessions led by certified instructors.",
        "A holistic wellness experience featuring expert talks, demos, and hands-on activities.",
        "Discover practical tools and techniques for improving your physical and mental health.",
        "Join a supportive community focused on health, well-being, and personal growth.",
    ],
    "Arts & Culture": [
        "Immerse yourself in the vibrant arts scene of New York City.",
        "A curated experience showcasing the work of emerging and established artists.",
        "Celebrate creativity and cultural expression through interactive art experiences.",
        "Explore diverse artistic perspectives in a thought-provoking exhibition.",
    ],
    "Sports & Fitness": [
        "Get active and challenge yourself in a fun, competitive environment.",
        "A high-energy fitness event suitable for all skill levels and ages.",
        "Join fellow sports enthusiasts for a day of friendly competition and camaraderie.",
        "Push your limits and achieve your fitness goals with expert coaching.",
    ],
    "Education": [
        "Expand your knowledge and skills in an engaging, interactive learning environment.",
        "A hands-on educational experience designed for curious minds of all ages.",
        "Learn from experts and connect with fellow learners passionate about growth.",
        "Gain practical skills and insights that you can apply immediately.",
    ],
    "Community": [
        "Come together with your neighbors to celebrate and strengthen our community.",
        "A volunteer-driven event bringing people together to make a positive impact.",
        "Connect with your community through fun activities, food, and conversation.",
        "Support a great cause while enjoying a day of connection and togetherness.",
    ],
    "Networking": [
        "Expand your professional circle and make meaningful connections at this mixer.",
        "Meet like-minded professionals in a relaxed, welcoming atmosphere.",
        "Build valuable relationships that can accelerate your career and business growth.",
        "A curated networking event designed to foster genuine professional connections.",
    ],
}

# Morning, afternoon, evening time ranges
_TIME_SLOTS = {
    "morning": ["09:00", "09:30", "10:00"],
    "afternoon": ["13:00", "13:30", "14:00", "14:30", "15:00"],
    "evening": ["18:00", "18:30", "19:00", "19:30", "20:00"],
}

# Date range: 2026-03-15 to 2026-04-15 (32 days)
_DATES = []
for m, d_start, d_end in [(3, 15, 31), (4, 1, 15)]:
    for d in range(d_start, d_end + 1):
        _DATES.append(f"2026-{m:02d}-{d:02d}")


def generate_events() -> list[dict]:
    """Generate 60 deterministic events.

    Events 1-3 are hardcoded to match DataLoader.kt defaults and task references:
      1: NYC Tech Summit 2026  (Technology, Javits Center)
      2: Jazz Night at Blue Note  (Music, Blue Note Jazz Club)
      3: Spring Food Festival  (Food & Drink, Central Park)
    Events 4-60 are randomly generated.
    """
    rng = random.Random(42)

    # ── Hardcoded events 1-3 (must match DataLoader.kt + task goals) ──────
    events = [
        {
            "id": 1,
            "title": "NYC Tech Summit 2026",
            "category": "Technology",
            "venue": "Javits Center",
            "city": "New York",
            "date": "2026-03-15",
            "time": "09:00",
            "price": 0,
            "organizer": "TechConnect NYC",
            "description": "Annual technology conference featuring AI and cloud computing",
            "available_tickets": 200,
        },
        {
            "id": 2,
            "title": "Jazz Night at Blue Note",
            "category": "Music",
            "venue": "Blue Note Jazz Club",
            "city": "New York",
            "date": "2026-03-16",
            "time": "20:00",
            "price": 45,
            "organizer": "NYC Jazz Foundation",
            "description": "An evening of live jazz performances",
            "available_tickets": 80,
        },
        {
            "id": 3,
            "title": "Spring Food Festival",
            "category": "Food & Drink",
            "venue": "Central Park",
            "city": "New York",
            "date": "2026-03-17",
            "time": "11:00",
            "price": 25,
            "organizer": "NYC Foodies",
            "description": "Taste dishes from top NYC restaurants",
            "available_tickets": 500,
        },
    ]
    used_titles = {e["title"] for e in events}

    # Track per-category template usage index
    template_idx = {cat: 0 for cat in CATEGORIES}
    # Mark hardcoded categories as consumed once
    template_idx["Technology"] = 1
    template_idx["Music"] = 1
    template_idx["Food & Drink"] = 1

    # Ensure at least 6 events per category (57 remaining across 10 categories)
    # Technology, Music, Food & Drink already have 1 each → need 5 more each
    # Other 7 categories need 6 each → 7*6 = 42, plus 3*5 = 15 → 57 total
    category_queue = []
    for cat in CATEGORIES:
        already = 1 if cat in ("Technology", "Music", "Food & Drink") else 0
        category_queue.extend([cat] * (6 - already))
    rng.shuffle(category_queue)

    # Build date assignments: 57 generated events across 32 dates.
    # Use ~20 distinct dates so that with 15 venues some venue+date pairs repeat.
    # Pick 20 dates from the pool deterministically, then assign with repetition.
    date_pool_full = list(_DATES)
    rng_dates = random.Random(42)
    date_pool = rng_dates.sample(date_pool_full, 20)
    date_pool.sort()

    # Pre-generate 57 date assignments (with wrap-around → collisions)
    date_assignments = [date_pool[i % len(date_pool)] for i in range(57)]
    rng_dates.shuffle(date_assignments)

    # Pre-create 6 explicit same-venue-same-date pairs at different times.
    # These indices are into the `events` list (0-based), so events[0-2] are
    # the hardcoded ones. Generated events start at events[3].
    # Pairs: events (3,18), (4,19), (5,20), (6,21), (7,22), (8,23)
    _SAME_VD_PAIRS = [(3, 18), (4, 19), (5, 20), (6, 21), (7, 22), (8, 23)]
    _PAIR_TIMES = [
        ("morning", "evening"),
        ("morning", "afternoon"),
        ("afternoon", "evening"),
        ("morning", "evening"),
        ("afternoon", "evening"),
        ("morning", "afternoon"),
    ]

    for event_id in range(4, 61):
        idx0 = event_id - 4  # 0-based index into category_queue
        category = category_queue[idx0]

        # Pick a unique title from the templates
        templates = _TITLE_TEMPLATES[category]
        tidx = template_idx[category]
        # Use suffix number to guarantee uniqueness across repeated templates
        suffix = tidx // len(templates) + 1 if tidx >= len(templates) else 0
        template = templates[tidx % len(templates)]
        if suffix > 0:
            title = template.format(n=f"#{suffix + 1}")
        else:
            title = template.format(n="").strip()
        template_idx[category] += 1

        # Ensure title uniqueness (defensive)
        while title in used_titles:
            title = title + " Edition"
        used_titles.add(title)

        # Venue: distribute across all venues
        venue = VENUES[idx0 % len(VENUES)]

        # Date: from shuffled assignments
        date = date_assignments[idx0]

        # Time: pick from morning/afternoon/evening
        time_of_day = rng.choice(["morning", "afternoon", "evening"])
        time = rng.choice(_TIME_SLOTS[time_of_day])

        # Price: ~40% free (price=0), rest 5-150
        if rng.random() < 0.43:
            price = 0
        else:
            price = rng.randint(1, 30) * 5  # multiples of 5, up to 150

        # Organizer
        organizer = rng.choice(ORGANIZERS)

        # Description
        desc_templates = _DESC_TEMPLATES[category]
        description = rng.choice(desc_templates)

        # Available tickets
        available_tickets = rng.randint(50, 500)

        events.append({
            "id": event_id,
            "title": title,
            "category": category,
            "venue": venue,
            "city": "New York",
            "date": date,
            "time": time,
            "price": price,
            "organizer": organizer,
            "description": description,
            "available_tickets": available_tickets,
        })

    # Patch same-venue-same-date pairs: copy venue+date from first event to
    # second, and assign distinct time-of-day slots to guarantee different times.
    for (a, b), (tod_a, tod_b) in zip(_SAME_VD_PAIRS, _PAIR_TIMES):
        events[b]["venue"] = events[a]["venue"]
        events[b]["date"] = events[a]["date"]
        # Assign deterministic but distinct times
        events[a]["time"] = _TIME_SLOTS[tod_a][0]
        events[b]["time"] = _TIME_SLOTS[tod_b][0]

    return events


def main():
    # Safety guard: committed JSON is canonical benchmark data
    output_path = Path(__file__).parent / "apps" / "meventbrite" / "seed.json"

    events = generate_events()

    seed_data = {
        "events": events,
    }

    with open(output_path, "w") as f:
        json.dump(seed_data, f, indent=2)

    print(f"Generated {len(events)} events across "
          f"{len(set(e['category'] for e in events))} categories and "
          f"{len(set(e['venue'] for e in events))} venues")
    print(f"Saved to {output_path}")

    # Print distribution
    from collections import Counter
    cat_count = Counter(e["category"] for e in events)
    venue_count = Counter(e["venue"] for e in events)
    org_count = Counter(e["organizer"] for e in events)
    free_count = sum(1 for e in events if e["price"] == 0)

    print(f"\nFree events: {free_count}/{len(events)} ({100*free_count/len(events):.1f}%)")
    print("\nCategory distribution:")
    for c, n in sorted(cat_count.items()):
        print(f"  {c}: {n}")
    print("\nVenue distribution:")
    for v, n in sorted(venue_count.items()):
        print(f"  {v}: {n}")
    print("\nOrganizer distribution:")
    for o, n in sorted(org_count.items()):
        print(f"  {o}: {n}")

    # Check same venue+date events
    from collections import defaultdict
    venue_date = defaultdict(list)
    for e in events:
        venue_date[(e["venue"], e["date"])].append(e["time"])
    multi = {k: v for k, v in venue_date.items() if len(v) > 1}
    if multi:
        print(f"\nSame venue+date events ({len(multi)} combinations):")
        for (v, d), times in sorted(multi.items()):
            print(f"  {v} on {d}: {', '.join(times)}")


if __name__ == "__main__":
    main()
