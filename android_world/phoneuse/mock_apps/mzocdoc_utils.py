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

"""Utilities for interacting with mZocdoc app database."""

import dataclasses
from typing import List, Optional, Dict, Any
from android_world.env import adb_utils
from android_world.env import interface

MZOCDOC_DB_PATH = "/data/data/com.phoneuse.mzocdoc/databases/mzocdoc.db"

_db_ensured = False


def _ensure_mzocdoc_db(env: interface.AsyncEnv) -> None:
    """Ensure mZocdoc DB directory and tables exist (idempotent).

    After ``pm clear com.phoneuse.mzocdoc`` the entire databases/
    directory is deleted.  We recreate it and the required tables so
    that seed-data loading and verification queries succeed.
    """
    global _db_ensured
    if _db_ensured:
        return
    db_dir = "/data/data/com.phoneuse.mzocdoc/databases"
    adb_utils.issue_generic_request(["shell", f"mkdir -p {db_dir}"], env)

    create_stmts = [
        (
            "CREATE TABLE IF NOT EXISTS appointments ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            "doctor_id INTEGER NOT NULL, "
            "patient_name TEXT NOT NULL, "
            "patient_phone TEXT, "
            "patient_dob TEXT, "
            "patient_email TEXT, "
            "patient_gender TEXT, "
            "insurance_provider TEXT, "
            "insurance_id TEXT, "
            "visit_reason TEXT NOT NULL, "
            "appointment_time TEXT NOT NULL, "
            "status TEXT DEFAULT 'confirmed', "
            "created_at TEXT DEFAULT (datetime('now')), "
            "vip_phone TEXT, "
            "blood_type TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT);"
        ),
        (
            "CREATE TABLE IF NOT EXISTS form_drafts ("
            "doctor_id INTEGER NOT NULL, "
            "appointment_time TEXT NOT NULL, "
            "patient_name TEXT, "
            "patient_phone TEXT, "
            "patient_dob TEXT, "
            "patient_email TEXT, "
            "patient_gender TEXT, "
            "insurance_provider TEXT, "
            "insurance_id TEXT, "
            "visit_reason TEXT, "
            "vip_phone TEXT, "
            "blood_type TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT, "
            "express_checkin_email TEXT, "
            "updated_at TEXT, "
            "PRIMARY KEY (doctor_id, appointment_time));"
        ),
    ]
    for stmt in create_stmts:
        adb_utils.execute_sql_command(MZOCDOC_DB_PATH, stmt, env)

    # Fix ownership: mkdir/sqlite3 run as root, but the app needs write access.
    # Derive the app UID from the parent /data/data/com.phoneuse.mzocdoc dir.
    adb_utils.issue_generic_request(
        ["shell",
         "owner=$(stat -c '%u:%g' /data/data/com.phoneuse.mzocdoc) && "
         f"chown -R $owner {db_dir}"],
        env,
    )
    _db_ensured = True


@dataclasses.dataclass(frozen=True)
class Appointment:
    """Appointment data class matching the database schema."""
    id: int
    doctor_id: int
    patient_name: str
    patient_phone: Optional[str] = None
    patient_dob: Optional[str] = None
    patient_email: Optional[str] = None
    patient_gender: Optional[str] = None
    insurance_provider: Optional[str] = None
    insurance_id: Optional[str] = None
    visit_reason: str = ""
    appointment_time: str = ""
    status: str = "confirmed"
    created_at: str = ""
    ssn_vip: Optional[str] = None
    emergency_ssn: Optional[str] = None


def get_appointments(env: interface.AsyncEnv) -> List[Appointment]:
    """Get all appointments from mZocdoc database.
    
    Args:
        env: The Android environment.
        
    Returns:
        List of Appointment objects.
    """
    query = "SELECT * FROM appointments ORDER BY created_at DESC;"
    response = adb_utils.execute_sql_command(MZOCDOC_DB_PATH, query, env)
    
    appointments = []
    if response.output:
        for line in response.output.strip().split('\n'):
            if '|' in line:
                parts = line.split('|')
                if len(parts) >= 11:
                    appointments.append(Appointment(
                        id=int(parts[0].strip()) if parts[0].strip() else 0,
                        doctor_id=int(parts[1].strip()) if parts[1].strip() else 0,
                        patient_name=parts[2].strip() if parts[2].strip() else "",
                        patient_phone=parts[3].strip() if parts[3].strip() else None,
                        patient_dob=parts[4].strip() if parts[4].strip() else None,
                        patient_email=parts[5].strip() if parts[5].strip() else None,
                        patient_gender=parts[6].strip() if parts[6].strip() else None,
                        insurance_provider=parts[7].strip() if parts[7].strip() else None,
                        insurance_id=parts[8].strip() if parts[8].strip() else None,
                        visit_reason=parts[9].strip() if parts[9].strip() else "",
                        appointment_time=parts[10].strip() if parts[10].strip() else "",
                        status=parts[11].strip() if len(parts) > 11 and parts[11].strip() else "confirmed",
                        created_at=parts[12].strip() if len(parts) > 12 and parts[12].strip() else "",
                        ssn_vip=parts[13].strip() if len(parts) > 13 and parts[13].strip() else None,
                        emergency_ssn=parts[14].strip() if len(parts) > 14 and parts[14].strip() else None,
                    ))

    return appointments


def get_appointments_by_doctor(doctor_id: int, env: interface.AsyncEnv) -> List[Appointment]:
    """Get appointments for a specific doctor.
    
    Args:
        doctor_id: The doctor ID.
        env: The Android environment.
        
    Returns:
        List of Appointment objects for the doctor.
    """
    query = f"SELECT * FROM appointments WHERE doctor_id = {doctor_id} ORDER BY appointment_time ASC;"
    response = adb_utils.execute_sql_command(MZOCDOC_DB_PATH, query, env)
    
    appointments = []
    if response.output:
        for line in response.output.strip().split('\n'):
            if '|' in line:
                parts = line.split('|')
                if len(parts) >= 11:
                    appointments.append(Appointment(
                        id=int(parts[0].strip()) if parts[0].strip() else 0,
                        doctor_id=int(parts[1].strip()) if parts[1].strip() else 0,
                        patient_name=parts[2].strip() if parts[2].strip() else "",
                        patient_phone=parts[3].strip() if parts[3].strip() else None,
                        patient_dob=parts[4].strip() if parts[4].strip() else None,
                        patient_email=parts[5].strip() if parts[5].strip() else None,
                        patient_gender=parts[6].strip() if parts[6].strip() else None,
                        insurance_provider=parts[7].strip() if parts[7].strip() else None,
                        insurance_id=parts[8].strip() if parts[8].strip() else None,
                        visit_reason=parts[9].strip() if parts[9].strip() else "",
                        appointment_time=parts[10].strip() if parts[10].strip() else "",
                        status=parts[11].strip() if len(parts) > 11 and parts[11].strip() else "confirmed",
                        created_at=parts[12].strip() if len(parts) > 12 and parts[12].strip() else "",
                        ssn_vip=parts[13].strip() if len(parts) > 13 and parts[13].strip() else None,
                        emergency_ssn=parts[14].strip() if len(parts) > 14 and parts[14].strip() else None,
                    ))

    return appointments


def clear_appointments(env: interface.AsyncEnv) -> None:
    """Clear all appointments from the database (for testing).

    Args:
        env: The Android environment.
    """
    _ensure_mzocdoc_db(env)
    query = "DELETE FROM appointments;"
    adb_utils.execute_sql_command(MZOCDOC_DB_PATH, query, env)


def clear_form_drafts(env: interface.AsyncEnv) -> None:
    """Clear all form drafts from the database.

    Must be called during environment reset to prevent stale drafts from
    previous runs from contaminating form_minimization scoring.

    Args:
        env: The Android environment.
    """
    query = "DELETE FROM form_drafts;"
    try:
        adb_utils.execute_sql_command(MZOCDOC_DB_PATH, query, env)
    except Exception:
        pass  # Table may not exist yet on first run


def load_mzocdoc_data(data: Dict[str, Any], env: interface.AsyncEnv) -> bool:
    """Load mZocdoc seed data into the device database.

    Clears all existing appointments, then inserts any pre-loaded appointments
    from data["appointments"] (if present).

    Args:
        data: mZocdoc data dict from JSON.  May contain an "appointments" list
              of dicts with keys matching the appointments table columns.
        env: Android environment

    Returns:
        True if successful
    """
    try:
        # Clear existing appointments and form drafts
        clear_appointments(env)
        clear_form_drafts(env)

        # Insert pre-loaded appointments (for cancel / multi-book scenarios)
        for appt in data.get('appointments', []):
            doctor_id = appt.get('doctor_id', 0)
            patient_name = appt.get('patient_name', '').replace("'", "''")
            patient_phone = appt.get('patient_phone', '').replace("'", "''")
            patient_dob = appt.get('patient_dob', '').replace("'", "''")
            patient_email = appt.get('patient_email', '').replace("'", "''")
            patient_gender = appt.get('patient_gender', '').replace("'", "''")
            insurance_provider = appt.get('insurance_provider', '').replace("'", "''")
            insurance_id = appt.get('insurance_id', '').replace("'", "''")
            visit_reason = appt.get('visit_reason', '').replace("'", "''")
            appointment_time = appt.get('appointment_time', '').replace("'", "''")
            status = appt.get('status', 'confirmed').replace("'", "''")

            query = (
                "INSERT INTO appointments "
                "(doctor_id, patient_name, patient_phone, patient_dob, "
                "patient_email, patient_gender, insurance_provider, insurance_id, "
                "visit_reason, appointment_time, status) VALUES "
                f"({doctor_id}, '{patient_name}', '{patient_phone}', '{patient_dob}', "
                f"'{patient_email}', '{patient_gender}', '{insurance_provider}', "
                f"'{insurance_id}', '{visit_reason}', '{appointment_time}', '{status}');"
            )
            adb_utils.execute_sql_command(MZOCDOC_DB_PATH, query, env)

        return True
    except Exception as e:
        print(f"Error loading mZocdoc data: {e}")
        return False

