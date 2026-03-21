"""Utility functions for interacting with the mGeico app's SQLite database."""

import dataclasses
import logging
from typing import Optional

from android_world.env import adb_utils

logger = logging.getLogger(__name__)

MGEICO_DB_PATH = "/data/data/com.phoneuse.mgeico/databases/mgeico.db"
MGEICO_PACKAGE = "com.phoneuse.mgeico"

_db_ensured = False


def _ensure_mgeico_db(env) -> None:
    """Ensure the mGeico database and tables exist with correct permissions."""
    global _db_ensured
    if _db_ensured:
        return

    db_dir = "/data/data/com.phoneuse.mgeico/databases"
    adb_utils.issue_generic_request(["shell", f"mkdir -p {db_dir}"], env)

    # Create quotes table
    create_quotes = (
        "CREATE TABLE IF NOT EXISTS quotes ("
        "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        "plan_id INTEGER NOT NULL, "
        "applicant_name TEXT NOT NULL, "
        "applicant_phone TEXT, "
        "applicant_dob TEXT, "
        "applicant_email TEXT, "
        "applicant_gender TEXT, "
        "applicant_address TEXT, "
        "applicant_occupation TEXT, "
        "vehicle_year TEXT NOT NULL, "
        "vehicle_make_model TEXT NOT NULL, "
        "current_insurance TEXT, "
        "current_policy_number TEXT, "
        "emergency_contact_name TEXT, "
        "emergency_contact_phone TEXT, "
        "roadside_phone TEXT, "
        "claims_email TEXT, "
        "coverage_notes TEXT NOT NULL, "
        "status TEXT DEFAULT 'submitted', "
        "created_at TEXT DEFAULT (datetime('now'))"
        ");"
    )
    adb_utils.execute_sql_command(MGEICO_DB_PATH, create_quotes, env)

    # Create form_drafts table
    create_drafts = (
        "CREATE TABLE IF NOT EXISTS form_drafts ("
        "plan_id INTEGER NOT NULL PRIMARY KEY, "
        "applicant_name TEXT, "
        "applicant_phone TEXT, "
        "applicant_dob TEXT, "
        "applicant_email TEXT, "
        "applicant_gender TEXT, "
        "applicant_address TEXT, "
        "applicant_occupation TEXT, "
        "vehicle_year TEXT, "
        "vehicle_make_model TEXT, "
        "current_insurance TEXT, "
        "current_policy_number TEXT, "
        "emergency_contact_name TEXT, "
        "emergency_contact_phone TEXT, "
        "roadside_phone TEXT, "
        "claims_email TEXT, "
        "coverage_notes TEXT, "
        "updated_at TEXT"
        ");"
    )
    adb_utils.execute_sql_command(MGEICO_DB_PATH, create_drafts, env)

    # Fix permissions
    adb_utils.issue_generic_request(
        ["shell",
         f"owner=$(stat -c '%u:%g' /data/data/{MGEICO_PACKAGE}) && "
         f"chown -R $owner {db_dir}"],
        env,
    )

    _db_ensured = True


@dataclasses.dataclass
class QuoteRecord:
    id: int
    plan_id: int
    applicant_name: str
    applicant_phone: Optional[str] = None
    applicant_dob: Optional[str] = None
    applicant_email: Optional[str] = None
    applicant_gender: Optional[str] = None
    applicant_address: Optional[str] = None
    applicant_occupation: Optional[str] = None
    vehicle_year: str = ""
    vehicle_make_model: str = ""
    current_insurance: Optional[str] = None
    current_policy_number: Optional[str] = None
    emergency_contact_name: Optional[str] = None
    emergency_contact_phone: Optional[str] = None
    roadside_phone: Optional[str] = None
    claims_email: Optional[str] = None
    coverage_notes: str = ""
    status: str = "submitted"
    created_at: str = ""


def get_quotes(env) -> list[QuoteRecord]:
    """Get all quotes from the database."""
    _ensure_mgeico_db(env)
    query = (
        "SELECT id, plan_id, applicant_name, applicant_phone, applicant_dob, "
        "applicant_email, applicant_gender, applicant_address, applicant_occupation, "
        "vehicle_year, vehicle_make_model, current_insurance, current_policy_number, "
        "emergency_contact_name, emergency_contact_phone, "
        "roadside_phone, claims_email, coverage_notes, status, created_at "
        "FROM quotes ORDER BY id ASC;"
    )
    response = adb_utils.execute_sql_command(MGEICO_DB_PATH, query, env)
    result = response.generic.output.decode('utf-8', errors='replace').strip()
    quotes = []
    if result:
        for line in result.strip().split("\n"):
            parts = line.split("|")
            if len(parts) >= 20:
                quotes.append(QuoteRecord(
                    id=int(parts[0]),
                    plan_id=int(parts[1]),
                    applicant_name=parts[2],
                    applicant_phone=parts[3] or None,
                    applicant_dob=parts[4] or None,
                    applicant_email=parts[5] or None,
                    applicant_gender=parts[6] or None,
                    applicant_address=parts[7] or None,
                    applicant_occupation=parts[8] or None,
                    vehicle_year=parts[9],
                    vehicle_make_model=parts[10],
                    current_insurance=parts[11] or None,
                    current_policy_number=parts[12] or None,
                    emergency_contact_name=parts[13] or None,
                    emergency_contact_phone=parts[14] or None,
                    roadside_phone=parts[15] or None,
                    claims_email=parts[16] or None,
                    coverage_notes=parts[17],
                    status=parts[18],
                    created_at=parts[19],
                ))
    return quotes


def clear_form_drafts(env) -> None:
    """Clear the form_drafts table."""
    _ensure_mgeico_db(env)
    adb_utils.execute_sql_command(MGEICO_DB_PATH, "DELETE FROM form_drafts;", env)


def load_mgeico_data(data: dict, env) -> None:
    """Clear quotes + form_drafts and optionally insert pre-set quotes."""
    _ensure_mgeico_db(env)

    # Clear both tables
    adb_utils.execute_sql_command(MGEICO_DB_PATH, "DELETE FROM quotes;", env)
    clear_form_drafts(env)

    # Insert pre-set quotes if any
    quotes = data.get("quotes", [])
    for q in quotes:
        plan_id = q.get("plan_id", 0)
        name = q.get("applicant_name", "").replace("'", "''")
        phone = q.get("applicant_phone", "").replace("'", "''")
        vehicle_year = q.get("vehicle_year", "").replace("'", "''")
        vehicle_mm = q.get("vehicle_make_model", "").replace("'", "''")
        coverage_notes = q.get("coverage_notes", "").replace("'", "''")
        current_insurance = q.get("current_insurance", "").replace("'", "''")
        sql = (
            f"INSERT INTO quotes (plan_id, applicant_name, applicant_phone, "
            f"vehicle_year, vehicle_make_model, current_insurance, coverage_notes, status) "
            f"VALUES ({plan_id}, '{name}', '{phone}', "
            f"'{vehicle_year}', '{vehicle_mm}', '{current_insurance}', '{coverage_notes}', 'submitted');"
        )
        adb_utils.execute_sql_command(MGEICO_DB_PATH, sql, env)

    logger.info("mGeico data loaded: %d pre-set quotes", len(quotes))
