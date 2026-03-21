"""Utilities for interacting with mDMV app database."""

import dataclasses
from typing import List, Optional, Dict, Any
from android_world.env import adb_utils
from android_world.env import interface

MDMV_DB_PATH = "/data/data/com.phoneuse.mdmv/databases/mdmv.db"

_db_ensured = False


def _ensure_mdmv_db(env: interface.AsyncEnv) -> None:
    """Ensure mDMV DB directory and tables exist (idempotent)."""
    global _db_ensured
    if _db_ensured:
        return
    db_dir = "/data/data/com.phoneuse.mdmv/databases"
    adb_utils.issue_generic_request(["shell", f"mkdir -p {db_dir}"], env)

    create_stmts = [
        (
            "CREATE TABLE IF NOT EXISTS appointments ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            "applicant_name TEXT NOT NULL, "
            "applicant_phone TEXT, "
            "applicant_email TEXT, "
            "office_id INTEGER NOT NULL, "
            "office_name TEXT, "
            "service_type TEXT, "
            "appointment_date TEXT, "
            "appointment_time TEXT, "
            "created_at TEXT DEFAULT (datetime('now')), "
            "applicant_dob TEXT, "
            "residency_state TEXT, "
            "home_address TEXT, "
            "license_state TEXT, "
            "license_number TEXT, "
            "vehicle_make TEXT, "
            "vehicle_vin TEXT, "
            "express_phone TEXT, "
            "digital_email TEXT);"
        ),
        (
            "CREATE TABLE IF NOT EXISTS form_drafts ("
            "office_id INTEGER NOT NULL, "
            "applicant_name TEXT, "
            "applicant_phone TEXT, "
            "applicant_dob TEXT, "
            "applicant_email TEXT, "
            "residency_state TEXT, "
            "home_address TEXT, "
            "license_state TEXT, "
            "license_number TEXT, "
            "vehicle_make TEXT, "
            "vehicle_vin TEXT, "
            "express_phone TEXT, "
            "digital_email TEXT, "
            "service_type TEXT, "
            "appointment_date TEXT, "
            "appointment_time TEXT, "
            "updated_at TEXT, "
            "PRIMARY KEY (office_id));"
        ),
    ]
    for stmt in create_stmts:
        adb_utils.execute_sql_command(MDMV_DB_PATH, stmt, env)

    adb_utils.issue_generic_request(
        ["shell",
         "owner=$(stat -c '%u:%g' /data/data/com.phoneuse.mdmv) && "
         f"chown -R $owner {db_dir}"],
        env,
    )
    _db_ensured = True


@dataclasses.dataclass(frozen=True)
class MdmvAppointment:
    """Appointment data class matching the database schema."""
    id: int
    applicant_name: str
    applicant_phone: Optional[str] = None
    applicant_email: Optional[str] = None
    office_id: int = 0
    office_name: Optional[str] = None
    service_type: Optional[str] = None
    appointment_date: Optional[str] = None
    appointment_time: Optional[str] = None
    created_at: str = ""


def get_appointments(env: interface.AsyncEnv) -> List[MdmvAppointment]:
    """Get all appointments from mDMV database."""
    _ensure_mdmv_db(env)
    query = "SELECT * FROM appointments ORDER BY created_at DESC;"
    response = adb_utils.execute_sql_command(MDMV_DB_PATH, query, env)

    appointments = []
    if response.output:
        for line in response.output.strip().split('\n'):
            if '|' in line:
                parts = line.split('|')
                if len(parts) >= 10:
                    appointments.append(MdmvAppointment(
                        id=int(parts[0].strip()) if parts[0].strip() else 0,
                        applicant_name=parts[1].strip() if parts[1].strip() else "",
                        applicant_phone=parts[2].strip() if parts[2].strip() else None,
                        applicant_email=parts[3].strip() if parts[3].strip() else None,
                        office_id=int(parts[4].strip()) if parts[4].strip() else 0,
                        office_name=parts[5].strip() if parts[5].strip() else None,
                        service_type=parts[6].strip() if parts[6].strip() else None,
                        appointment_date=parts[7].strip() if parts[7].strip() else None,
                        appointment_time=parts[8].strip() if parts[8].strip() else None,
                        created_at=parts[9].strip() if parts[9].strip() else "",
                    ))

    return appointments


def clear_appointments(env: interface.AsyncEnv) -> None:
    """Clear all appointments from the database."""
    _ensure_mdmv_db(env)
    query = "DELETE FROM appointments;"
    adb_utils.execute_sql_command(MDMV_DB_PATH, query, env)


def clear_form_drafts(env: interface.AsyncEnv) -> None:
    """Clear all form drafts from the database."""
    query = "DELETE FROM form_drafts;"
    try:
        adb_utils.execute_sql_command(MDMV_DB_PATH, query, env)
    except Exception:
        pass


def load_mdmv_data(data: Dict[str, Any], env: interface.AsyncEnv) -> bool:
    """Load mDMV seed data into the device database.

    Clears existing appointments and form_drafts, then inserts
    any pre-loaded appointments from data.
    """
    try:
        clear_appointments(env)
        clear_form_drafts(env)

        for appt in data.get('appointments', []):
            name = appt.get('applicant_name', '').replace("'", "''")
            phone = appt.get('applicant_phone', '').replace("'", "''")
            email = appt.get('applicant_email', '').replace("'", "''")
            office_id = appt.get('office_id', 0)
            office_name = appt.get('office_name', '').replace("'", "''")
            service_type = appt.get('service_type', '').replace("'", "''")
            appt_date = appt.get('appointment_date', '').replace("'", "''")
            appt_time = appt.get('appointment_time', '').replace("'", "''")

            query = (
                "INSERT INTO appointments "
                "(applicant_name, applicant_phone, applicant_email, "
                "office_id, office_name, service_type, "
                "appointment_date, appointment_time) VALUES "
                f"('{name}', '{phone}', '{email}', "
                f"{office_id}, '{office_name}', '{service_type}', "
                f"'{appt_date}', '{appt_time}');"
            )
            adb_utils.execute_sql_command(MDMV_DB_PATH, query, env)

        return True
    except Exception as e:
        print(f"Error loading mDMV data: {e}")
        return False
