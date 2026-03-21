#!/usr/bin/env python3
"""Generate deterministic DMV office seed data for mDMV app."""

import json
import random
from pathlib import Path


def generate_offices(rng: random.Random) -> list:
    """Generate 50 DMV offices across multiple states."""

    # State/city data
    locations = [
        # NY
        ("Manhattan DMV Office", "11 Greenwich St", "New York", "NY", "10004"),
        ("Brooklyn DMV Office", "625 Atlantic Ave", "Brooklyn", "NY", "11217"),
        ("Queens DMV Office", "168-46 91st Ave", "Queens", "NY", "11432"),
        ("Bronx DMV Office", "696 E Fordham Rd", "Bronx", "NY", "10458"),
        ("Staten Island DMV Office", "1775 South Ave", "Staten Island", "NY", "10314"),
        ("Harlem DMV Office", "159 E 125th St", "New York", "NY", "10035"),
        ("Midtown DMV Office", "300 W 34th St", "New York", "NY", "10001"),
        ("Long Island City DMV Office", "47-40 21st St", "Queens", "NY", "11101"),
        ("Yonkers DMV Office", "1 Larkin Center", "Yonkers", "NY", "10701"),
        ("Buffalo DMV Office", "100 N Pearl St", "Buffalo", "NY", "14202"),
        # NJ
        ("Newark DMV Office", "228 Frelinghuysen Ave", "Newark", "NJ", "07114"),
        ("Jersey City DMV Office", "438 Summit Ave", "Jersey City", "NJ", "07306"),
        ("Trenton DMV Office", "120 S Stockton St", "Trenton", "NJ", "08608"),
        ("Paterson DMV Office", "125 Ellison St", "Paterson", "NJ", "07505"),
        ("Edison DMV Office", "45 Kilmer Rd", "Edison", "NJ", "08817"),
        # CT
        ("Hartford DMV Office", "60 State St", "Hartford", "CT", "06103"),
        ("New Haven DMV Office", "333 State St", "New Haven", "CT", "06510"),
        ("Stamford DMV Office", "888 Washington Blvd", "Stamford", "CT", "06901"),
        ("Bridgeport DMV Office", "990 Main St", "Bridgeport", "CT", "06604"),
        ("Waterbury DMV Office", "236 Grand St", "Waterbury", "CT", "06702"),
        # CA
        ("Los Angeles DMV Office", "3615 S Hope St", "Los Angeles", "CA", "90007"),
        ("San Francisco DMV Office", "1377 Fell St", "San Francisco", "CA", "94117"),
        ("San Diego DMV Office", "3960 Normal St", "San Diego", "CA", "92103"),
        ("Sacramento DMV Office", "4700 Broadway", "Sacramento", "CA", "95820"),
        ("Oakland DMV Office", "5300 Claremont Ave", "Oakland", "CA", "94618"),
        ("San Jose DMV Office", "111 W Alma Ave", "San Jose", "CA", "95110"),
        ("Fresno DMV Office", "665 E Belmont Ave", "Fresno", "CA", "93701"),
        ("Long Beach DMV Office", "3700 E Willow St", "Long Beach", "CA", "90815"),
        # TX
        ("Houston DMV Office", "7011 Harwin Dr", "Houston", "TX", "77036"),
        ("Dallas DMV Office", "4445 N Central Expy", "Dallas", "TX", "75205"),
        ("Austin DMV Office", "6121 N Lamar Blvd", "Austin", "TX", "78752"),
        ("San Antonio DMV Office", "7106 Culebra Rd", "San Antonio", "TX", "78238"),
        ("Fort Worth DMV Office", "4901 S Freeway", "Fort Worth", "TX", "76115"),
        ("El Paso DMV Office", "3731 Buckner St", "El Paso", "TX", "79930"),
        # FL
        ("Miami DMV Office", "7900 NW 27th Ave", "Miami", "FL", "33147"),
        ("Orlando DMV Office", "6050 Metrowest Blvd", "Orlando", "FL", "32835"),
        ("Tampa DMV Office", "1101 E Hillsborough Ave", "Tampa", "FL", "33604"),
        ("Jacksonville DMV Office", "7020 AC Skinner Pkwy", "Jacksonville", "FL", "32256"),
        # IL
        ("Chicago DMV Office", "100 W Randolph St", "Chicago", "IL", "60601"),
        ("Springfield DMV Office", "2701 S Dirksen Pkwy", "Springfield", "IL", "62703"),
        # PA
        ("Philadelphia DMV Office", "8011 Roosevelt Blvd", "Philadelphia", "PA", "19152"),
        ("Pittsburgh DMV Office", "5164 Penn Ave", "Pittsburgh", "PA", "15224"),
        # MA
        ("Boston DMV Office", "630 Washington St", "Boston", "MA", "02111"),
        ("Cambridge DMV Office", "141 Portland St", "Cambridge", "MA", "02139"),
        # GA
        ("Atlanta DMV Office", "2801 Candler Rd", "Atlanta", "GA", "30034"),
        # WA
        ("Seattle DMV Office", "12006 Aurora Ave N", "Seattle", "WA", "98133"),
        # CO
        ("Denver DMV Office", "4685 Peoria St", "Denver", "CO", "80239"),
        # AZ
        ("Phoenix DMV Office", "3434 W Clarendon Ave", "Phoenix", "AZ", "85017"),
        # OH
        ("Columbus DMV Office", "4738 Riverside Dr", "Columbus", "OH", "43220"),
        # MI
        ("Detroit DMV Office", "11700 Merriman Rd", "Detroit", "MI", "48150"),
    ]

    all_services = [
        "License Renewal",
        "Vehicle Registration",
        "Title Transfer",
        "State ID",
        "Permit Test",
        "Road Test",
        "Learner Permit",
        "Name Change",
        "Address Change",
        "Duplicate License",
    ]

    hours_options = [
        "Mon-Fri 8:00-16:00",
        "Mon-Fri 8:30-16:30",
        "Mon-Fri 8:00-15:30",
        "Mon-Fri 9:00-17:00",
        "Mon-Sat 8:00-14:00",
    ]

    offices = []
    for idx, (name, address, city, state, zip_code) in enumerate(locations, 1):
        # Each office offers 3-7 services
        num_services = rng.randint(3, 7)
        services = rng.sample(all_services, min(num_services, len(all_services)))
        # Ensure common services are included
        for must_have in ["License Renewal", "Vehicle Registration"]:
            if must_have not in services:
                services[0] = must_have
        services.sort()

        phone_area = rng.choice(["212", "718", "646", "201", "203", "213", "214", "305", "312", "617"])
        phone = f"({phone_area}) 555-{rng.randint(1000, 9999):04d}"

        offices.append({
            "id": idx,
            "name": name,
            "address": address,
            "city": city,
            "state": state,
            "zip_code": zip_code,
            "phone": phone,
            "services_offered": services,
            "hours": rng.choice(hours_options),
            "wait_time_minutes": rng.randint(15, 90),
        })

    return offices


def main():
    rng = random.Random(42)
    offices = generate_offices(rng)

    output_path = Path(__file__).parent / "apps" / "mdmv" / "seed.json"
    data = {"offices": offices}

    with open(output_path, "w") as f:
        json.dump(data, f, indent=2)

    print(f"Generated {len(offices)} DMV offices → {output_path}")

    # Stats
    states = set(o["state"] for o in offices)
    cities = set(o["city"] for o in offices)
    print(f"States: {len(states)} ({', '.join(sorted(states))})")
    print(f"Cities: {len(cities)}")


if __name__ == "__main__":
    main()
