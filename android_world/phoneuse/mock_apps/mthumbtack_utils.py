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

"""Utilities for interacting with mThumbtack app database."""

import dataclasses
from typing import List, Optional, Dict, Any
from android_world.env import adb_utils
from android_world.env import interface

MTHUMBTACK_DB_PATH = "/data/data/com.phoneuse.mthumbtack/databases/mthumbtack.db"

_db_ensured = False


def _ensure_mthumbtack_db(env: interface.AsyncEnv) -> None:
    """Ensure mThumbtack DB directory and tables exist (idempotent).

    After ``pm clear com.phoneuse.mthumbtack`` the entire databases/
    directory is deleted.  We recreate it and the required tables so
    that seed-data loading and verification queries succeed.
    """
    global _db_ensured
    if _db_ensured:
        return
    db_dir = "/data/data/com.phoneuse.mthumbtack/databases"
    adb_utils.issue_generic_request(["shell", f"mkdir -p {db_dir}"], env)

    create_stmts = [
        (
            "CREATE TABLE IF NOT EXISTS service_requests ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            "pro_id INTEGER NOT NULL, "
            "customer_name TEXT NOT NULL, "
            "customer_phone TEXT, "
            "customer_dob TEXT, "
            "customer_email TEXT, "
            "customer_address TEXT, "
            "customer_occupation TEXT, "
            "insurance_provider TEXT, "
            "insurance_id TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT, "
            "project_description TEXT NOT NULL, "
            "appointment_time TEXT NOT NULL, "
            "status TEXT DEFAULT 'confirmed', "
            "created_at TEXT DEFAULT (datetime('now')));"
        ),
        (
            "CREATE TABLE IF NOT EXISTS form_drafts ("
            "pro_id INTEGER NOT NULL, "
            "appointment_time TEXT NOT NULL, "
            "customer_name TEXT, "
            "customer_phone TEXT, "
            "customer_dob TEXT, "
            "customer_email TEXT, "
            "customer_address TEXT, "
            "customer_occupation TEXT, "
            "insurance_provider TEXT, "
            "insurance_id TEXT, "
            "emergency_contact_name TEXT, "
            "emergency_contact_phone TEXT, "
            "project_description TEXT, "
            "priority_phone TEXT, "
            "notification_email TEXT, "
            "updated_at TEXT, "
            "PRIMARY KEY (pro_id, appointment_time));"
        ),
    ]
    for stmt in create_stmts:
        adb_utils.execute_sql_command(MTHUMBTACK_DB_PATH, stmt, env)

    # Fix ownership: mkdir/sqlite3 run as root, but the app needs write access.
    # Derive the app UID from the parent /data/data/com.phoneuse.mthumbtack dir.
    adb_utils.issue_generic_request(
        ["shell",
         "owner=$(stat -c '%u:%g' /data/data/com.phoneuse.mthumbtack) && "
         f"chown -R $owner {db_dir}"],
        env,
    )
    _db_ensured = True


@dataclasses.dataclass(frozen=True)
class ServiceRequest:
    """Service request data class matching the database schema."""
    id: int
    pro_id: int
    customer_name: str
    customer_phone: Optional[str] = None
    customer_dob: Optional[str] = None
    customer_email: Optional[str] = None
    customer_address: Optional[str] = None
    customer_occupation: Optional[str] = None
    insurance_provider: Optional[str] = None
    insurance_id: Optional[str] = None
    emergency_contact_name: Optional[str] = None
    emergency_contact_phone: Optional[str] = None
    project_description: str = ""
    appointment_time: str = ""
    status: str = "confirmed"
    created_at: str = ""


def get_service_requests(env: interface.AsyncEnv) -> List[ServiceRequest]:
    """Get all service requests from mThumbtack database.

    Args:
        env: The Android environment.

    Returns:
        List of ServiceRequest objects.
    """
    _ensure_mthumbtack_db(env)
    query = "SELECT * FROM service_requests ORDER BY created_at DESC;"
    response = adb_utils.execute_sql_command(MTHUMBTACK_DB_PATH, query, env)

    service_requests = []
    if response.output:
        for line in response.output.strip().split('\n'):
            if '|' in line:
                parts = line.split('|')
                if len(parts) >= 14:
                    service_requests.append(ServiceRequest(
                        id=int(parts[0].strip()) if parts[0].strip() else 0,
                        pro_id=int(parts[1].strip()) if parts[1].strip() else 0,
                        customer_name=parts[2].strip() if parts[2].strip() else "",
                        customer_phone=parts[3].strip() if parts[3].strip() else None,
                        customer_dob=parts[4].strip() if parts[4].strip() else None,
                        customer_email=parts[5].strip() if parts[5].strip() else None,
                        customer_address=parts[6].strip() if parts[6].strip() else None,
                        customer_occupation=parts[7].strip() if parts[7].strip() else None,
                        insurance_provider=parts[8].strip() if parts[8].strip() else None,
                        insurance_id=parts[9].strip() if parts[9].strip() else None,
                        emergency_contact_name=parts[10].strip() if parts[10].strip() else None,
                        emergency_contact_phone=parts[11].strip() if parts[11].strip() else None,
                        project_description=parts[12].strip() if parts[12].strip() else "",
                        appointment_time=parts[13].strip() if parts[13].strip() else "",
                        status=parts[14].strip() if len(parts) > 14 and parts[14].strip() else "confirmed",
                        created_at=parts[15].strip() if len(parts) > 15 and parts[15].strip() else "",
                    ))

    return service_requests


def get_service_requests_by_pro(pro_id: int, env: interface.AsyncEnv) -> List[ServiceRequest]:
    """Get service requests for a specific pro.

    Args:
        pro_id: The pro ID.
        env: The Android environment.

    Returns:
        List of ServiceRequest objects for the pro.
    """
    _ensure_mthumbtack_db(env)
    query = f"SELECT * FROM service_requests WHERE pro_id = {pro_id} ORDER BY appointment_time ASC;"
    response = adb_utils.execute_sql_command(MTHUMBTACK_DB_PATH, query, env)

    service_requests = []
    if response.output:
        for line in response.output.strip().split('\n'):
            if '|' in line:
                parts = line.split('|')
                if len(parts) >= 14:
                    service_requests.append(ServiceRequest(
                        id=int(parts[0].strip()) if parts[0].strip() else 0,
                        pro_id=int(parts[1].strip()) if parts[1].strip() else 0,
                        customer_name=parts[2].strip() if parts[2].strip() else "",
                        customer_phone=parts[3].strip() if parts[3].strip() else None,
                        customer_dob=parts[4].strip() if parts[4].strip() else None,
                        customer_email=parts[5].strip() if parts[5].strip() else None,
                        customer_address=parts[6].strip() if parts[6].strip() else None,
                        customer_occupation=parts[7].strip() if parts[7].strip() else None,
                        insurance_provider=parts[8].strip() if parts[8].strip() else None,
                        insurance_id=parts[9].strip() if parts[9].strip() else None,
                        emergency_contact_name=parts[10].strip() if parts[10].strip() else None,
                        emergency_contact_phone=parts[11].strip() if parts[11].strip() else None,
                        project_description=parts[12].strip() if parts[12].strip() else "",
                        appointment_time=parts[13].strip() if parts[13].strip() else "",
                        status=parts[14].strip() if len(parts) > 14 and parts[14].strip() else "confirmed",
                        created_at=parts[15].strip() if len(parts) > 15 and parts[15].strip() else "",
                    ))

    return service_requests


def clear_service_requests(env: interface.AsyncEnv) -> None:
    """Clear all service requests from the database (for testing).

    Args:
        env: The Android environment.
    """
    _ensure_mthumbtack_db(env)
    query = "DELETE FROM service_requests;"
    adb_utils.execute_sql_command(MTHUMBTACK_DB_PATH, query, env)


def clear_form_drafts(env: interface.AsyncEnv) -> None:
    """Clear all form drafts from the database.

    Must be called during environment reset to prevent stale drafts from
    previous runs from contaminating form_minimization scoring.

    Args:
        env: The Android environment.
    """
    query = "DELETE FROM form_drafts;"
    try:
        adb_utils.execute_sql_command(MTHUMBTACK_DB_PATH, query, env)
    except Exception:
        pass  # Table may not exist yet on first run


def load_mthumbtack_data(data: Dict[str, Any], env: interface.AsyncEnv) -> bool:
    """Load mThumbtack seed data into the device database.

    Clears all existing service requests and form drafts, then inserts any
    pre-loaded service requests from data["service_requests"] (if present).

    Args:
        data: mThumbtack data dict from JSON.  May contain a "service_requests"
              list of dicts with keys matching the service_requests table columns.
        env: Android environment

    Returns:
        True if successful
    """
    try:
        # Clear existing service requests and form drafts
        clear_service_requests(env)
        clear_form_drafts(env)

        # Insert pre-loaded service requests (for cancel / multi-book scenarios)
        for req in data.get('service_requests', []):
            pro_id = req.get('pro_id', 0)
            customer_name = req.get('customer_name', '').replace("'", "''")
            customer_phone = req.get('customer_phone', '').replace("'", "''")
            customer_dob = req.get('customer_dob', '').replace("'", "''")
            customer_email = req.get('customer_email', '').replace("'", "''")
            customer_address = req.get('customer_address', '').replace("'", "''")
            customer_occupation = req.get('customer_occupation', '').replace("'", "''")
            insurance_provider = req.get('insurance_provider', '').replace("'", "''")
            insurance_id = req.get('insurance_id', '').replace("'", "''")
            emergency_contact_name = req.get('emergency_contact_name', '').replace("'", "''")
            emergency_contact_phone = req.get('emergency_contact_phone', '').replace("'", "''")
            project_description = req.get('project_description', '').replace("'", "''")
            appointment_time = req.get('appointment_time', '').replace("'", "''")
            status = req.get('status', 'confirmed').replace("'", "''")

            query = (
                "INSERT INTO service_requests "
                "(pro_id, customer_name, customer_phone, customer_dob, "
                "customer_email, customer_address, customer_occupation, "
                "insurance_provider, insurance_id, "
                "emergency_contact_name, emergency_contact_phone, "
                "project_description, appointment_time, status) VALUES "
                f"({pro_id}, '{customer_name}', '{customer_phone}', '{customer_dob}', "
                f"'{customer_email}', '{customer_address}', '{customer_occupation}', "
                f"'{insurance_provider}', '{insurance_id}', "
                f"'{emergency_contact_name}', '{emergency_contact_phone}', "
                f"'{project_description}', '{appointment_time}', '{status}');"
            )
            adb_utils.execute_sql_command(MTHUMBTACK_DB_PATH, query, env)

        return True
    except Exception as e:
        print(f"Error loading mThumbtack data: {e}")
        return False
