"""Utilities for interacting with mBooking app database."""

import dataclasses
from typing import List, Optional, Dict, Any
from android_world.env import adb_utils
from android_world.env import interface

MBOOKING_DB_PATH = "/data/data/com.phoneuse.mbooking/databases/mbooking.db"

_db_ensured = False


def _ensure_mbooking_db(env: interface.AsyncEnv) -> None:
    """Ensure mBooking DB directory and tables exist (idempotent)."""
    global _db_ensured
    if _db_ensured:
        return
    db_dir = "/data/data/com.phoneuse.mbooking/databases"
    adb_utils.issue_generic_request(["shell", f"mkdir -p {db_dir}"], env)

    create_stmts = [
        (
            "CREATE TABLE IF NOT EXISTS reservations ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            "hotel_id INTEGER NOT NULL, "
            "guest_name TEXT NOT NULL, "
            "guest_phone TEXT, "
            "guest_dob TEXT, "
            "guest_email TEXT, "
            "guest_gender TEXT, "
            "loyalty_program TEXT, "
            "loyalty_id TEXT, "
            "passport_number TEXT, "
            "check_in_date TEXT NOT NULL, "
            "check_out_date TEXT NOT NULL, "
            "room_type TEXT, "
            "special_requests TEXT, "
            "status TEXT DEFAULT 'confirmed', "
            "created_at TEXT DEFAULT (datetime('now')), "
            "rewards_phone TEXT, "
            "express_checkin_email TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT);"
        ),
        (
            "CREATE TABLE IF NOT EXISTS form_drafts ("
            "hotel_id INTEGER NOT NULL, "
            "check_in_date TEXT NOT NULL, "
            "guest_name TEXT, "
            "guest_phone TEXT, "
            "guest_dob TEXT, "
            "guest_email TEXT, "
            "guest_gender TEXT, "
            "loyalty_program TEXT, "
            "loyalty_id TEXT, "
            "passport_number TEXT, "
            "check_out_date TEXT, "
            "room_type TEXT, "
            "special_requests TEXT, "
            "rewards_phone TEXT, "
            "express_checkin_email TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT, "
            "updated_at TEXT, "
            "PRIMARY KEY (hotel_id, check_in_date));"
        ),
    ]
    for stmt in create_stmts:
        adb_utils.execute_sql_command(MBOOKING_DB_PATH, stmt, env)

    adb_utils.issue_generic_request(
        ["shell",
         "owner=$(stat -c '%u:%g' /data/data/com.phoneuse.mbooking) && "
         f"chown -R $owner {db_dir}"],
        env,
    )
    _db_ensured = True


@dataclasses.dataclass(frozen=True)
class Reservation:
    """Reservation data class matching the database schema."""
    id: int
    hotel_id: int
    guest_name: str
    guest_phone: Optional[str] = None
    guest_dob: Optional[str] = None
    guest_email: Optional[str] = None
    guest_gender: Optional[str] = None
    loyalty_program: Optional[str] = None
    loyalty_id: Optional[str] = None
    passport_number: Optional[str] = None
    check_in_date: str = ""
    check_out_date: str = ""
    room_type: Optional[str] = None
    special_requests: Optional[str] = None
    status: str = "confirmed"
    created_at: str = ""


def get_reservations(env: interface.AsyncEnv) -> List[Reservation]:
    """Get all reservations from mBooking database."""
    query = "SELECT * FROM reservations ORDER BY created_at DESC;"
    response = adb_utils.execute_sql_command(MBOOKING_DB_PATH, query, env)

    reservations = []
    if response.output:
        for line in response.output.strip().split('\n'):
            if '|' in line:
                parts = line.split('|')
                if len(parts) >= 12:
                    reservations.append(Reservation(
                        id=int(parts[0].strip()) if parts[0].strip() else 0,
                        hotel_id=int(parts[1].strip()) if parts[1].strip() else 0,
                        guest_name=parts[2].strip() if parts[2].strip() else "",
                        guest_phone=parts[3].strip() if parts[3].strip() else None,
                        guest_dob=parts[4].strip() if parts[4].strip() else None,
                        guest_email=parts[5].strip() if parts[5].strip() else None,
                        guest_gender=parts[6].strip() if parts[6].strip() else None,
                        loyalty_program=parts[7].strip() if parts[7].strip() else None,
                        loyalty_id=parts[8].strip() if parts[8].strip() else None,
                        passport_number=parts[9].strip() if parts[9].strip() else None,
                        check_in_date=parts[10].strip() if parts[10].strip() else "",
                        check_out_date=parts[11].strip() if parts[11].strip() else "",
                        room_type=parts[12].strip() if len(parts) > 12 and parts[12].strip() else None,
                        special_requests=parts[13].strip() if len(parts) > 13 and parts[13].strip() else None,
                        status=parts[14].strip() if len(parts) > 14 and parts[14].strip() else "confirmed",
                        created_at=parts[15].strip() if len(parts) > 15 and parts[15].strip() else "",
                    ))

    return reservations


def clear_reservations(env: interface.AsyncEnv) -> None:
    """Clear all reservations from the database."""
    _ensure_mbooking_db(env)
    query = "DELETE FROM reservations;"
    adb_utils.execute_sql_command(MBOOKING_DB_PATH, query, env)


def clear_form_drafts(env: interface.AsyncEnv) -> None:
    """Clear all form drafts from the database."""
    query = "DELETE FROM form_drafts;"
    try:
        adb_utils.execute_sql_command(MBOOKING_DB_PATH, query, env)
    except Exception:
        pass


def load_mbooking_data(data: Dict[str, Any], env: interface.AsyncEnv) -> bool:
    """Load mBooking seed data into the device database.

    Clears all existing reservations and form_drafts, then inserts any
    pre-loaded reservations from data["reservations"] (if present).
    """
    try:
        clear_reservations(env)
        clear_form_drafts(env)

        for res in data.get('reservations', []):
            hotel_id = res.get('hotel_id', 0)
            guest_name = res.get('guest_name', '').replace("'", "''")
            guest_phone = res.get('guest_phone', '').replace("'", "''")
            guest_dob = res.get('guest_dob', '').replace("'", "''")
            guest_email = res.get('guest_email', '').replace("'", "''")
            guest_gender = res.get('guest_gender', '').replace("'", "''")
            loyalty_program = res.get('loyalty_program', '').replace("'", "''")
            loyalty_id = res.get('loyalty_id', '').replace("'", "''")
            passport_number = res.get('passport_number', '').replace("'", "''")
            check_in_date = res.get('check_in_date', '').replace("'", "''")
            check_out_date = res.get('check_out_date', '').replace("'", "''")
            room_type = res.get('room_type', '').replace("'", "''")
            special_requests = res.get('special_requests', '').replace("'", "''")
            status = res.get('status', 'confirmed').replace("'", "''")

            query = (
                "INSERT INTO reservations "
                "(hotel_id, guest_name, guest_phone, guest_dob, "
                "guest_email, guest_gender, loyalty_program, loyalty_id, "
                "passport_number, check_in_date, check_out_date, room_type, "
                "special_requests, status) VALUES "
                f"({hotel_id}, '{guest_name}', '{guest_phone}', '{guest_dob}', "
                f"'{guest_email}', '{guest_gender}', '{loyalty_program}', "
                f"'{loyalty_id}', '{passport_number}', '{check_in_date}', "
                f"'{check_out_date}', '{room_type}', '{special_requests}', '{status}');"
            )
            adb_utils.execute_sql_command(MBOOKING_DB_PATH, query, env)

        return True
    except Exception as e:
        print(f"Error loading mBooking data: {e}")
        return False
