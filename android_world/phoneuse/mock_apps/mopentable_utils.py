# Copyright 2025 PhoneUse Privacy Protocol Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Utilities for interacting with mOpenTable app database."""

import dataclasses
from typing import List, Optional, Dict, Any
from android_world.env import adb_utils
from android_world.env import interface

MOPENTABLE_DB_PATH = "/data/data/com.phoneuse.mopentable/databases/mopentable.db"

_db_ensured = False


def _ensure_mopentable_db(env: interface.AsyncEnv) -> None:
    """Ensure mOpenTable DB directory and tables exist (idempotent)."""
    global _db_ensured
    if _db_ensured:
        return
    db_dir = "/data/data/com.phoneuse.mopentable/databases"
    adb_utils.issue_generic_request(["shell", f"mkdir -p {db_dir}"], env)

    create_stmts = [
        (
            "CREATE TABLE IF NOT EXISTS reservations ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            "restaurant_id INTEGER NOT NULL, "
            "guest_name TEXT NOT NULL, "
            "guest_phone TEXT, "
            "guest_dob TEXT, "
            "guest_email TEXT, "
            "guest_gender TEXT, "
            "party_size TEXT, "
            "insurance_provider TEXT, "
            "insurance_id TEXT, "
            "blood_type TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT, "
            "special_requests TEXT NOT NULL, "
            "reservation_time TEXT NOT NULL, "
            "status TEXT DEFAULT 'confirmed', "
            "created_at TEXT DEFAULT (datetime('now')));"
        ),
        (
            "CREATE TABLE IF NOT EXISTS form_drafts ("
            "restaurant_id INTEGER NOT NULL, "
            "reservation_time TEXT NOT NULL, "
            "guest_name TEXT, "
            "guest_phone TEXT, "
            "guest_dob TEXT, "
            "guest_email TEXT, "
            "guest_gender TEXT, "
            "party_size TEXT, "
            "occupation TEXT, "
            "insurance_provider TEXT, "
            "insurance_id TEXT, "
            "blood_type TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT, "
            "priority_seating_phone TEXT, "
            "waitlist_email TEXT, "
            "special_requests TEXT, "
            "updated_at TEXT, "
            "PRIMARY KEY (restaurant_id, reservation_time));"
        ),
    ]
    for stmt in create_stmts:
        adb_utils.execute_sql_command(MOPENTABLE_DB_PATH, stmt, env)

    adb_utils.issue_generic_request(
        [
            "shell",
            "owner=$(stat -c '%u:%g' /data/data/com.phoneuse.mopentable) && "
            f"chown -R $owner {db_dir}",
        ],
        env,
    )
    _db_ensured = True


@dataclasses.dataclass(frozen=True)
class Reservation:
    """Reservation data class matching the database schema."""

    id: int
    restaurant_id: int
    guest_name: str
    guest_phone: Optional[str] = None
    guest_dob: Optional[str] = None
    guest_email: Optional[str] = None
    guest_gender: Optional[str] = None
    party_size: Optional[str] = None
    insurance_provider: Optional[str] = None
    insurance_id: Optional[str] = None
    blood_type: Optional[str] = None
    emergency_contact_name: Optional[str] = None
    emergency_contact_phone: Optional[str] = None
    special_requests: str = ""
    reservation_time: str = ""
    status: str = "confirmed"
    created_at: str = ""


def get_reservations(env: interface.AsyncEnv) -> List[Reservation]:
    """Get all reservations from mOpenTable database."""
    _ensure_mopentable_db(env)
    query = "SELECT * FROM reservations ORDER BY created_at DESC;"
    response = adb_utils.execute_sql_command(MOPENTABLE_DB_PATH, query, env)

    reservations = []
    if response.output:
        for line in response.output.strip().split("\n"):
            if "|" in line:
                parts = line.split("|")
                if len(parts) >= 15:
                    reservations.append(
                        Reservation(
                            id=int(parts[0].strip()) if parts[0].strip() else 0,
                            restaurant_id=int(parts[1].strip()) if parts[1].strip() else 0,
                            guest_name=parts[2].strip() if parts[2].strip() else "",
                            guest_phone=parts[3].strip() if parts[3].strip() else None,
                            guest_dob=parts[4].strip() if parts[4].strip() else None,
                            guest_email=parts[5].strip() if parts[5].strip() else None,
                            guest_gender=parts[6].strip() if parts[6].strip() else None,
                            party_size=parts[7].strip() if parts[7].strip() else None,
                            insurance_provider=parts[8].strip() if parts[8].strip() else None,
                            insurance_id=parts[9].strip() if parts[9].strip() else None,
                            blood_type=parts[10].strip() if parts[10].strip() else None,
                            emergency_contact_name=parts[11].strip() if parts[11].strip() else None,
                            emergency_contact_phone=parts[12].strip() if parts[12].strip() else None,
                            special_requests=parts[13].strip() if parts[13].strip() else "",
                            reservation_time=parts[14].strip() if parts[14].strip() else "",
                            status=parts[15].strip() if len(parts) > 15 and parts[15].strip() else "confirmed",
                            created_at=parts[16].strip() if len(parts) > 16 and parts[16].strip() else "",
                        )
                    )
    return reservations


def clear_reservations(env: interface.AsyncEnv) -> None:
    """Clear all reservations from the database."""
    _ensure_mopentable_db(env)
    adb_utils.execute_sql_command(MOPENTABLE_DB_PATH, "DELETE FROM reservations;", env)


def clear_form_drafts(env: interface.AsyncEnv) -> None:
    """Clear all form drafts from the database."""
    try:
        adb_utils.execute_sql_command(MOPENTABLE_DB_PATH, "DELETE FROM form_drafts;", env)
    except Exception:
        pass


def load_mopentable_data(data: Dict[str, Any], env: interface.AsyncEnv) -> bool:
    """Load mOpenTable seed data into the device database.

    Clears all existing reservations and form drafts, then inserts any
    pre-loaded reservations from data["reservations"].
    """
    try:
        clear_reservations(env)
        clear_form_drafts(env)

        for res in data.get("reservations", []):
            restaurant_id = res.get("restaurant_id", 0)
            guest_name = res.get("guest_name", "").replace("'", "''")
            guest_phone = res.get("guest_phone", "").replace("'", "''")
            guest_dob = res.get("guest_dob", "").replace("'", "''")
            guest_email = res.get("guest_email", "").replace("'", "''")
            guest_gender = res.get("guest_gender", "").replace("'", "''")
            party_size = res.get("party_size", "").replace("'", "''")
            insurance_provider = res.get("insurance_provider", "").replace("'", "''")
            insurance_id = res.get("insurance_id", "").replace("'", "''")
            blood_type = res.get("blood_type", "").replace("'", "''")
            ec_name = res.get("emergency_contact_name", "").replace("'", "''")
            ec_phone = res.get("emergency_contact_phone", "").replace("'", "''")
            special_requests = res.get("special_requests", "").replace("'", "''")
            reservation_time = res.get("reservation_time", "").replace("'", "''")
            status = res.get("status", "confirmed").replace("'", "''")

            query = (
                "INSERT INTO reservations "
                "(restaurant_id, guest_name, guest_phone, guest_dob, "
                "guest_email, guest_gender, party_size, insurance_provider, "
                "insurance_id, blood_type, emergency_contact_name, "
                "emergency_contact_phone, special_requests, reservation_time, status) VALUES "
                f"({restaurant_id}, '{guest_name}', '{guest_phone}', '{guest_dob}', "
                f"'{guest_email}', '{guest_gender}', '{party_size}', '{insurance_provider}', "
                f"'{insurance_id}', '{blood_type}', '{ec_name}', "
                f"'{ec_phone}', '{special_requests}', '{reservation_time}', '{status}');"
            )
            adb_utils.execute_sql_command(MOPENTABLE_DB_PATH, query, env)

        return True
    except Exception as e:
        print(f"Error loading mOpenTable data: {e}")
        return False
