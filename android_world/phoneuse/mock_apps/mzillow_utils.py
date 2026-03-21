"""Utilities for interacting with mZillow app database."""

import dataclasses
from typing import List, Optional, Dict, Any
from android_world.env import adb_utils
from android_world.env import interface

MZILLOW_DB_PATH = "/data/data/com.phoneuse.mzillow/databases/mzillow.db"

_db_ensured = False


def _ensure_mzillow_db(env: interface.AsyncEnv) -> None:
    """Ensure mZillow DB directory and tables exist (idempotent)."""
    global _db_ensured
    if _db_ensured:
        return
    db_dir = "/data/data/com.phoneuse.mzillow/databases"
    adb_utils.issue_generic_request(["shell", f"mkdir -p {db_dir}"], env)

    create_stmts = [
        (
            "CREATE TABLE IF NOT EXISTS viewing_appointments ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            "property_id INTEGER NOT NULL, "
            "visitor_name TEXT NOT NULL, "
            "visitor_phone TEXT, "
            "visitor_dob TEXT, "
            "visitor_gender TEXT, "
            "visitor_email TEXT, "
            "occupation TEXT, "
            "current_city TEXT, "
            "current_address TEXT, "
            "insurance_provider TEXT, "
            "insurance_id TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT, "
            "viewing_purpose TEXT NOT NULL, "
            "viewing_time TEXT NOT NULL, "
            "status TEXT DEFAULT 'confirmed', "
            "created_at TEXT DEFAULT (datetime('now')), "
            "priority_phone TEXT, "
            "alert_email TEXT);"
        ),
        (
            "CREATE TABLE IF NOT EXISTS form_drafts ("
            "property_id INTEGER NOT NULL, "
            "viewing_time TEXT NOT NULL, "
            "visitor_name TEXT, "
            "visitor_phone TEXT, "
            "visitor_dob TEXT, "
            "visitor_gender TEXT, "
            "visitor_email TEXT, "
            "occupation TEXT, "
            "current_city TEXT, "
            "current_address TEXT, "
            "insurance_provider TEXT, "
            "insurance_id TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT, "
            "priority_phone TEXT, "
            "alert_email TEXT, "
            "viewing_purpose TEXT, "
            "updated_at TEXT, "
            "PRIMARY KEY (property_id, viewing_time));"
        ),
    ]
    for stmt in create_stmts:
        adb_utils.execute_sql_command(MZILLOW_DB_PATH, stmt, env)

    adb_utils.issue_generic_request(
        ["shell",
         "owner=$(stat -c '%u:%g' /data/data/com.phoneuse.mzillow) && "
         f"chown -R $owner {db_dir}"],
        env,
    )
    _db_ensured = True


@dataclasses.dataclass(frozen=True)
class ViewingAppointment:
    id: int
    property_id: int
    visitor_name: str
    visitor_phone: Optional[str] = None
    visitor_dob: Optional[str] = None
    visitor_gender: Optional[str] = None
    visitor_email: Optional[str] = None
    occupation: Optional[str] = None
    current_city: Optional[str] = None
    current_address: Optional[str] = None
    insurance_provider: Optional[str] = None
    insurance_id: Optional[str] = None
    emergency_contact_name: Optional[str] = None
    emergency_contact_phone: Optional[str] = None
    viewing_purpose: str = ""
    viewing_time: str = ""
    status: str = "confirmed"
    created_at: str = ""


def get_viewing_appointments(env: interface.AsyncEnv) -> List[ViewingAppointment]:
    query = "SELECT * FROM viewing_appointments ORDER BY created_at DESC;"
    response = adb_utils.execute_sql_command(MZILLOW_DB_PATH, query, env)
    appointments = []
    if response.output:
        for line in response.output.strip().split('\n'):
            if '|' in line:
                parts = line.split('|')
                if len(parts) >= 17:
                    appointments.append(ViewingAppointment(
                        id=int(parts[0].strip()) if parts[0].strip() else 0,
                        property_id=int(parts[1].strip()) if parts[1].strip() else 0,
                        visitor_name=parts[2].strip() if parts[2].strip() else "",
                        visitor_phone=parts[3].strip() or None,
                        visitor_dob=parts[4].strip() or None,
                        visitor_gender=parts[5].strip() or None,
                        visitor_email=parts[6].strip() or None,
                        occupation=parts[7].strip() or None,
                        current_city=parts[8].strip() or None,
                        current_address=parts[9].strip() or None,
                        insurance_provider=parts[10].strip() or None,
                        insurance_id=parts[11].strip() or None,
                        emergency_contact_name=parts[12].strip() or None,
                        emergency_contact_phone=parts[13].strip() or None,
                        viewing_purpose=parts[14].strip() if parts[14].strip() else "",
                        viewing_time=parts[15].strip() if parts[15].strip() else "",
                        status=parts[16].strip() if len(parts) > 16 and parts[16].strip() else "confirmed",
                        created_at=parts[17].strip() if len(parts) > 17 and parts[17].strip() else "",
                    ))
    return appointments


def clear_viewing_appointments(env: interface.AsyncEnv) -> None:
    _ensure_mzillow_db(env)
    adb_utils.execute_sql_command(
        MZILLOW_DB_PATH, "DELETE FROM viewing_appointments;", env
    )


def clear_form_drafts(env: interface.AsyncEnv) -> None:
    try:
        adb_utils.execute_sql_command(
            MZILLOW_DB_PATH, "DELETE FROM form_drafts;", env
        )
    except Exception:
        pass


def load_mzillow_data(data: Dict[str, Any], env: interface.AsyncEnv) -> bool:
    """Load mZillow seed data (pre-existing viewing appointments) into device DB."""
    try:
        clear_viewing_appointments(env)
        clear_form_drafts(env)

        for appt in data.get('viewing_appointments', []):
            property_id = appt.get('property_id', 0)
            visitor_name = appt.get('visitor_name', '').replace("'", "''")
            visitor_phone = appt.get('visitor_phone', '').replace("'", "''")
            visitor_dob = appt.get('visitor_dob', '').replace("'", "''")
            visitor_email = appt.get('visitor_email', '').replace("'", "''")
            visitor_gender = appt.get('visitor_gender', '').replace("'", "''")
            occupation = appt.get('occupation', '').replace("'", "''")
            current_city = appt.get('current_city', '').replace("'", "''")
            current_address = appt.get('current_address', '').replace("'", "''")
            insurance_provider = appt.get('insurance_provider', '').replace("'", "''")
            insurance_id = appt.get('insurance_id', '').replace("'", "''")
            ec_name = appt.get('emergency_contact_name', '').replace("'", "''")
            ec_phone = appt.get('emergency_contact_phone', '').replace("'", "''")
            viewing_purpose = appt.get('viewing_purpose', '').replace("'", "''")
            viewing_time = appt.get('viewing_time', '').replace("'", "''")
            status = appt.get('status', 'confirmed').replace("'", "''")

            query = (
                "INSERT INTO viewing_appointments "
                "(property_id, visitor_name, visitor_phone, visitor_dob, "
                "visitor_email, visitor_gender, occupation, current_city, "
                "current_address, insurance_provider, insurance_id, "
                "emergency_contact_name, emergency_contact_phone, "
                "viewing_purpose, viewing_time, status) VALUES "
                f"({property_id}, '{visitor_name}', '{visitor_phone}', "
                f"'{visitor_dob}', '{visitor_email}', '{visitor_gender}', "
                f"'{occupation}', '{current_city}', '{current_address}', "
                f"'{insurance_provider}', '{insurance_id}', "
                f"'{ec_name}', '{ec_phone}', "
                f"'{viewing_purpose}', '{viewing_time}', '{status}');"
            )
            adb_utils.execute_sql_command(MZILLOW_DB_PATH, query, env)

        return True
    except Exception as e:
        print(f"Error loading mZillow data: {e}")
        return False
