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

"""Utilities for interacting with mEventbrite app database."""

import dataclasses
from typing import List, Optional, Dict, Any
from android_world.env import adb_utils
from android_world.env import interface

MEVENTBRITE_DB_PATH = "/data/data/com.phoneuse.meventbrite/databases/meventbrite.db"

_db_ensured = False


def _ensure_meventbrite_db(env) -> None:
    """Ensure mEventbrite DB directory and tables exist (idempotent).

    After ``pm clear com.phoneuse.meventbrite`` the entire databases/
    directory is deleted.  We recreate it and the required tables so
    that seed-data loading and verification queries succeed.

    Args:
        env: AsyncEnv or controller — we extract the controller automatically.
    """
    global _db_ensured
    if _db_ensured:
        return
    controller = getattr(env, 'controller', env)
    db_dir = "/data/data/com.phoneuse.meventbrite/databases"
    adb_utils.issue_generic_request(["shell", f"mkdir -p {db_dir}"], controller)

    create_stmts = [
        (
            "CREATE TABLE IF NOT EXISTS registrations ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            "event_id INTEGER NOT NULL, "
            "attendee_name TEXT NOT NULL, "
            "attendee_phone TEXT, "
            "attendee_dob TEXT, "
            "attendee_gender TEXT, "
            "blood_type TEXT, "
            "attendee_email TEXT, "
            "home_address TEXT, "
            "num_tickets INTEGER DEFAULT 1, "
            "occupation TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT, "
            "reason_for_attending TEXT NOT NULL, "
            "sms_notification_phone TEXT, "
            "recommendation_email TEXT, "
            "status TEXT DEFAULT 'confirmed', "
            "created_at TEXT DEFAULT (datetime('now')));"
        ),
        (
            "CREATE TABLE IF NOT EXISTS form_drafts ("
            "event_id INTEGER PRIMARY KEY, "
            "attendee_name TEXT, "
            "attendee_phone TEXT, "
            "attendee_dob TEXT, "
            "attendee_gender TEXT, "
            "blood_type TEXT, "
            "attendee_email TEXT, "
            "home_address TEXT, "
            "num_tickets TEXT, "
            "occupation TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT, "
            "sms_notification_phone TEXT, "
            "recommendation_email TEXT, "
            "reason_for_attending TEXT, "
            "updated_at TEXT);"
        ),
    ]
    for stmt in create_stmts:
        adb_utils.execute_sql_command(MEVENTBRITE_DB_PATH, stmt, controller)

    # Fix ownership: mkdir/sqlite3 run as root, but the app needs write access.
    # Derive the app UID from the parent /data/data/com.phoneuse.meventbrite dir.
    adb_utils.issue_generic_request(
        ["shell",
         "owner=$(stat -c '%u:%g' /data/data/com.phoneuse.meventbrite) && "
         f"chown -R $owner {db_dir}"],
        controller,
    )
    _db_ensured = True


@dataclasses.dataclass(frozen=True)
class Registration:
    """Registration data class matching the database schema."""
    id: int
    event_id: int
    attendee_name: str
    attendee_phone: Optional[str] = None
    attendee_dob: Optional[str] = None
    attendee_gender: Optional[str] = None
    blood_type: Optional[str] = None
    attendee_email: Optional[str] = None
    home_address: Optional[str] = None
    num_tickets: int = 1
    occupation: Optional[str] = None
    emergency_contact_name: Optional[str] = None
    emergency_contact_phone: Optional[str] = None
    reason_for_attending: str = ""
    sms_notification_phone: Optional[str] = None
    recommendation_email: Optional[str] = None
    status: str = "confirmed"
    created_at: str = ""


def get_registrations(env) -> List[Registration]:
    """Get all registrations from mEventbrite database.

    Args:
        env: AsyncEnv or controller.

    Returns:
        List of Registration objects.
    """
    controller = getattr(env, 'controller', env)
    _ensure_meventbrite_db(controller)
    query = "SELECT * FROM registrations ORDER BY created_at DESC;"
    response = adb_utils.execute_sql_command(MEVENTBRITE_DB_PATH, query, controller)

    registrations = []
    if response.output:
        for line in response.output.strip().split('\n'):
            if '|' in line:
                parts = line.split('|')
                if len(parts) >= 14:
                    registrations.append(Registration(
                        id=int(parts[0].strip()) if parts[0].strip() else 0,
                        event_id=int(parts[1].strip()) if parts[1].strip() else 0,
                        attendee_name=parts[2].strip() if parts[2].strip() else "",
                        attendee_phone=parts[3].strip() if parts[3].strip() else None,
                        attendee_dob=parts[4].strip() if parts[4].strip() else None,
                        attendee_gender=parts[5].strip() if parts[5].strip() else None,
                        blood_type=parts[6].strip() if parts[6].strip() else None,
                        attendee_email=parts[7].strip() if parts[7].strip() else None,
                        home_address=parts[8].strip() if parts[8].strip() else None,
                        num_tickets=int(parts[9].strip()) if parts[9].strip() else 1,
                        occupation=parts[10].strip() if parts[10].strip() else None,
                        emergency_contact_name=parts[11].strip() if parts[11].strip() else None,
                        emergency_contact_phone=parts[12].strip() if parts[12].strip() else None,
                        reason_for_attending=parts[13].strip() if parts[13].strip() else "",
                        sms_notification_phone=parts[14].strip() if len(parts) > 14 and parts[14].strip() else None,
                        recommendation_email=parts[15].strip() if len(parts) > 15 and parts[15].strip() else None,
                        status=parts[16].strip() if len(parts) > 16 and parts[16].strip() else "confirmed",
                        created_at=parts[17].strip() if len(parts) > 17 and parts[17].strip() else "",
                    ))

    return registrations


def get_registrations_by_event(event_id: int, env) -> List[Registration]:
    """Get registrations for a specific event.

    Args:
        event_id: The event ID.
        env: AsyncEnv or controller.

    Returns:
        List of Registration objects for the event.
    """
    controller = getattr(env, 'controller', env)
    _ensure_meventbrite_db(controller)
    query = f"SELECT * FROM registrations WHERE event_id = {event_id} ORDER BY created_at DESC;"
    response = adb_utils.execute_sql_command(MEVENTBRITE_DB_PATH, query, controller)

    registrations = []
    if response.output:
        for line in response.output.strip().split('\n'):
            if '|' in line:
                parts = line.split('|')
                if len(parts) >= 14:
                    registrations.append(Registration(
                        id=int(parts[0].strip()) if parts[0].strip() else 0,
                        event_id=int(parts[1].strip()) if parts[1].strip() else 0,
                        attendee_name=parts[2].strip() if parts[2].strip() else "",
                        attendee_phone=parts[3].strip() if parts[3].strip() else None,
                        attendee_dob=parts[4].strip() if parts[4].strip() else None,
                        attendee_gender=parts[5].strip() if parts[5].strip() else None,
                        blood_type=parts[6].strip() if parts[6].strip() else None,
                        attendee_email=parts[7].strip() if parts[7].strip() else None,
                        home_address=parts[8].strip() if parts[8].strip() else None,
                        num_tickets=int(parts[9].strip()) if parts[9].strip() else 1,
                        occupation=parts[10].strip() if parts[10].strip() else None,
                        emergency_contact_name=parts[11].strip() if parts[11].strip() else None,
                        emergency_contact_phone=parts[12].strip() if parts[12].strip() else None,
                        reason_for_attending=parts[13].strip() if parts[13].strip() else "",
                        sms_notification_phone=parts[14].strip() if len(parts) > 14 and parts[14].strip() else None,
                        recommendation_email=parts[15].strip() if len(parts) > 15 and parts[15].strip() else None,
                        status=parts[16].strip() if len(parts) > 16 and parts[16].strip() else "confirmed",
                        created_at=parts[17].strip() if len(parts) > 17 and parts[17].strip() else "",
                    ))

    return registrations


def clear_registrations(env) -> None:
    """Clear all registrations from the database (for testing).

    Args:
        env: AsyncEnv or controller.
    """
    controller = getattr(env, 'controller', env)
    _ensure_meventbrite_db(controller)
    query = "DELETE FROM registrations;"
    adb_utils.execute_sql_command(MEVENTBRITE_DB_PATH, query, controller)


def clear_form_drafts(env) -> None:
    """Clear all form drafts from the database.

    Must be called during environment reset to prevent stale drafts from
    previous runs from contaminating form_minimization scoring.

    Args:
        env: AsyncEnv or controller.
    """
    controller = getattr(env, 'controller', env)
    query = "DELETE FROM form_drafts;"
    try:
        adb_utils.execute_sql_command(MEVENTBRITE_DB_PATH, query, controller)
    except Exception:
        pass  # Table may not exist yet on first run


def load_meventbrite_data(data: Dict[str, Any], env) -> bool:
    """Load mEventbrite seed data into the device database.

    Clears all existing registrations and form drafts, then inserts any
    pre-loaded registrations from data["registrations"] (if present).

    Args:
        data: mEventbrite data dict from JSON.  May contain a "registrations"
              list of dicts with keys matching the registrations table columns.
        env: AsyncEnv or controller.

    Returns:
        True if successful
    """
    try:
        controller = getattr(env, 'controller', env)
        _ensure_meventbrite_db(controller)

        # Clear existing registrations and form drafts
        clear_registrations(controller)
        clear_form_drafts(controller)

        # Insert pre-loaded registrations (for cancel / multi-register scenarios)
        for reg in data.get('registrations', []):
            event_id = reg.get('event_id', 0)
            attendee_name = reg.get('attendee_name', '').replace("'", "''")
            attendee_phone = reg.get('attendee_phone', '').replace("'", "''")
            attendee_dob = reg.get('attendee_dob', '').replace("'", "''")
            attendee_gender = reg.get('attendee_gender', '').replace("'", "''")
            blood_type = reg.get('blood_type', '').replace("'", "''")
            attendee_email = reg.get('attendee_email', '').replace("'", "''")
            home_address = reg.get('home_address', '').replace("'", "''")
            num_tickets = reg.get('num_tickets', 1)
            occupation = reg.get('occupation', '').replace("'", "''")
            emergency_contact_name = reg.get('emergency_contact_name', '').replace("'", "''")
            emergency_contact_phone = reg.get('emergency_contact_phone', '').replace("'", "''")
            reason_for_attending = reg.get('reason_for_attending', '').replace("'", "''")
            status = reg.get('status', 'confirmed').replace("'", "''")

            query = (
                "INSERT INTO registrations "
                "(event_id, attendee_name, attendee_phone, attendee_dob, "
                "attendee_gender, blood_type, attendee_email, home_address, "
                "num_tickets, occupation, emergency_contact_name, "
                "emergency_contact_phone, reason_for_attending, status) VALUES "
                f"({event_id}, '{attendee_name}', '{attendee_phone}', "
                f"'{attendee_dob}', '{attendee_gender}', '{blood_type}', "
                f"'{attendee_email}', '{home_address}', {num_tickets}, "
                f"'{occupation}', '{emergency_contact_name}', "
                f"'{emergency_contact_phone}', '{reason_for_attending}', "
                f"'{status}');"
            )
            adb_utils.execute_sql_command(MEVENTBRITE_DB_PATH, query, controller)

        return True
    except Exception as e:
        print(f"Error loading mEventbrite data: {e}")
        return False
