"""Utilities for interacting with mCVS Pharmacy app database."""

import dataclasses
from typing import List, Optional, Dict, Any
from android_world.env import adb_utils
from android_world.env import interface

MCVSPHARMACY_DB_PATH = (
    "/data/data/com.phoneuse.mcvspharmacy/databases/mcvspharmacy.db"
)

_db_ensured = False


def _ensure_mcvspharmacy_db(env: interface.AsyncEnv) -> None:
    """Ensure mCVS Pharmacy DB directory and tables exist (idempotent)."""
    global _db_ensured
    if _db_ensured:
        return
    db_dir = "/data/data/com.phoneuse.mcvspharmacy/databases"
    adb_utils.issue_generic_request(["shell", f"mkdir -p {db_dir}"], env)

    create_stmts = [
        (
            "CREATE TABLE IF NOT EXISTS bookings ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            "service_id INTEGER NOT NULL, "
            "store_id INTEGER NOT NULL, "
            "patient_name TEXT NOT NULL, "
            "patient_phone TEXT, "
            "patient_dob TEXT, "
            "patient_email TEXT, "
            "blood_type TEXT, "
            "patient_gender TEXT, "
            "occupation TEXT, "
            "insurance_provider TEXT, "
            "insurance_id TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT, "
            "rewards_phone TEXT, "
            "visit_reason TEXT NOT NULL, "
            "booking_time TEXT NOT NULL, "
            "status TEXT DEFAULT 'confirmed', "
            "created_at TEXT DEFAULT (datetime('now')));"
        ),
        (
            "CREATE TABLE IF NOT EXISTS form_drafts ("
            "service_id INTEGER NOT NULL, "
            "store_id INTEGER NOT NULL, "
            "booking_time TEXT NOT NULL, "
            "patient_name TEXT, "
            "patient_phone TEXT, "
            "patient_dob TEXT, "
            "patient_email TEXT, "
            "blood_type TEXT, "
            "patient_gender TEXT, "
            "occupation TEXT, "
            "insurance_provider TEXT, "
            "insurance_id TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT, "
            "rewards_phone TEXT, "
            "alert_email TEXT, "
            "visit_reason TEXT, "
            "updated_at TEXT, "
            "PRIMARY KEY (service_id, store_id, booking_time));"
        ),
    ]
    for stmt in create_stmts:
        adb_utils.execute_sql_command(MCVSPHARMACY_DB_PATH, stmt, env)

    adb_utils.issue_generic_request(
        [
            "shell",
            "owner=$(stat -c '%u:%g' /data/data/com.phoneuse.mcvspharmacy) && "
            f"chown -R $owner {db_dir}",
        ],
        env,
    )
    _db_ensured = True


@dataclasses.dataclass(frozen=True)
class Booking:
    """Booking data class matching the database schema."""

    id: int
    service_id: int
    store_id: int
    patient_name: str
    patient_phone: Optional[str] = None
    patient_dob: Optional[str] = None
    patient_email: Optional[str] = None
    blood_type: Optional[str] = None
    patient_gender: Optional[str] = None
    occupation: Optional[str] = None
    insurance_provider: Optional[str] = None
    insurance_id: Optional[str] = None
    emergency_contact_name: Optional[str] = None
    emergency_contact_phone: Optional[str] = None
    rewards_phone: Optional[str] = None
    visit_reason: str = ""
    booking_time: str = ""
    status: str = "confirmed"
    created_at: str = ""


def get_bookings(env: interface.AsyncEnv) -> List[Booking]:
    """Get all bookings from mCVS Pharmacy database."""
    query = "SELECT * FROM bookings ORDER BY created_at DESC;"
    response = adb_utils.execute_sql_command(MCVSPHARMACY_DB_PATH, query, env)

    bookings: List[Booking] = []
    if response.output:
        for line in response.output.strip().split("\n"):
            if "|" in line:
                p = line.split("|")
                if len(p) >= 17:
                    bookings.append(
                        Booking(
                            id=int(p[0].strip()) if p[0].strip() else 0,
                            service_id=int(p[1].strip()) if p[1].strip() else 0,
                            store_id=int(p[2].strip()) if p[2].strip() else 0,
                            patient_name=p[3].strip() or "",
                            patient_phone=p[4].strip() or None,
                            patient_dob=p[5].strip() or None,
                            patient_email=p[6].strip() or None,
                            blood_type=p[7].strip() or None,
                            patient_gender=p[8].strip() or None,
                            occupation=p[9].strip() or None,
                            insurance_provider=p[10].strip() or None,
                            insurance_id=p[11].strip() or None,
                            emergency_contact_name=p[12].strip() or None,
                            emergency_contact_phone=p[13].strip() or None,
                            rewards_phone=p[14].strip() or None,
                            visit_reason=p[15].strip() or "",
                            booking_time=p[16].strip() or "",
                            status=(
                                p[17].strip()
                                if len(p) > 17 and p[17].strip()
                                else "confirmed"
                            ),
                            created_at=(
                                p[18].strip() if len(p) > 18 and p[18].strip() else ""
                            ),
                        )
                    )
    return bookings


def clear_bookings(env: interface.AsyncEnv) -> None:
    """Clear all bookings from the database."""
    _ensure_mcvspharmacy_db(env)
    adb_utils.execute_sql_command(
        MCVSPHARMACY_DB_PATH, "DELETE FROM bookings;", env
    )


def clear_form_drafts(env: interface.AsyncEnv) -> None:
    """Clear all form drafts (must be called during reset)."""
    try:
        adb_utils.execute_sql_command(
            MCVSPHARMACY_DB_PATH, "DELETE FROM form_drafts;", env
        )
    except Exception:
        pass


def load_mcvspharmacy_data(
    data: Dict[str, Any], env: interface.AsyncEnv
) -> bool:
    """Load mCVS Pharmacy seed data into the device database.

    Clears existing bookings and form_drafts, then inserts any pre-loaded
    bookings from data["bookings"] (if present).
    """
    try:
        clear_bookings(env)
        clear_form_drafts(env)

        for bk in data.get("bookings", []):
            service_id = bk.get("service_id", 0)
            store_id = bk.get("store_id", 0)
            pn = bk.get("patient_name", "").replace("'", "''")
            pp = bk.get("patient_phone", "").replace("'", "''")
            pd = bk.get("patient_dob", "").replace("'", "''")
            pe = bk.get("patient_email", "").replace("'", "''")
            bt = bk.get("blood_type", "").replace("'", "''")
            pg = bk.get("patient_gender", "").replace("'", "''")
            oc = bk.get("occupation", "").replace("'", "''")
            ip = bk.get("insurance_provider", "").replace("'", "''")
            ii = bk.get("insurance_id", "").replace("'", "''")
            en = bk.get("emergency_contact_name", "").replace("'", "''")
            ep = bk.get("emergency_contact_phone", "").replace("'", "''")
            rp = bk.get("rewards_phone", "").replace("'", "''")
            vr = bk.get("visit_reason", "").replace("'", "''")
            btime = bk.get("booking_time", "").replace("'", "''")
            status = bk.get("status", "confirmed").replace("'", "''")

            query = (
                "INSERT INTO bookings "
                "(service_id, store_id, patient_name, patient_phone, "
                "patient_dob, patient_email, blood_type, patient_gender, "
                "occupation, insurance_provider, insurance_id, "
                "emergency_contact_name, emergency_contact_phone, "
                "rewards_phone, visit_reason, booking_time, status) VALUES "
                f"({service_id}, {store_id}, '{pn}', '{pp}', '{pd}', '{pe}', "
                f"'{bt}', '{pg}', '{oc}', '{ip}', '{ii}', '{en}', '{ep}', "
                f"'{rp}', '{vr}', '{btime}', '{status}');"
            )
            adb_utils.execute_sql_command(MCVSPHARMACY_DB_PATH, query, env)

        return True
    except Exception as e:
        print(f"Error loading mCVS Pharmacy data: {e}")
        return False
