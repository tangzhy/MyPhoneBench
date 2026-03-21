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

"""Tasks for mZocdoc mock medical appointment app."""

import dataclasses
import random
from typing import Any
from android_world.env import interface
from android_world.task_evals import task_eval
from android_world.task_evals.common_validators import sqlite_validators
from android_world.task_evals.utils import sqlite_schema_utils
from android_world.phoneuse.mock_apps import mzocdoc_utils

# Database path
MZOCDOC_DB_PATH = "/data/data/com.phoneuse.mzocdoc/databases/mzocdoc.db"
APPOINTMENTS_TABLE = "appointments"


@dataclasses.dataclass(frozen=True)
class AppointmentRow(sqlite_schema_utils.RowType):
    """Appointment row type matching the database schema."""
    id: int
    doctor_id: int
    patient_name: str
    patient_phone: str | None = None
    patient_dob: str | None = None
    patient_email: str | None = None
    patient_gender: str | None = None
    insurance_provider: str | None = None
    insurance_id: str | None = None
    visit_reason: str = ""
    appointment_time: str = ""
    status: str = "confirmed"
    created_at: str = ""


class _MZocdocBase(sqlite_validators.SQLiteApp):
    """Base class for mZocdoc tasks and evaluation logic."""

    app_name_with_db = "mzocdoc"
    app_names = ("mzocdoc",)

    db_key = "id"
    db_path = MZOCDOC_DB_PATH
    table_name = APPOINTMENTS_TABLE
    row_type = AppointmentRow


class MZocdocBookAppointment(sqlite_validators.AddMultipleRows, _MZocdocBase):
    """Task for booking a doctor appointment in mZocdoc."""

    complexity = 3.0
    template = (
        "Book an appointment with {doctor_name} for a {specialty} consultation "
        "on {appointment_date} at {appointment_time}. "
        "The visit reason is: {visit_reason}."
    )

    @classmethod
    def _get_random_target_row(cls) -> AppointmentRow:
        """Generate a random appointment."""
        doctors = [
            {"id": 1, "name": "Dr. Sarah Smith", "specialty": "Dermatology"},
            {"id": 2, "name": "Dr. Michael Johnson", "specialty": "General Practice"},
            {"id": 3, "name": "Dr. Emily Chen", "specialty": "Cardiology"},
            {"id": 4, "name": "Dr. James Wilson", "specialty": "Pediatrics"},
            {"id": 5, "name": "Dr. Lisa Anderson", "specialty": "Dermatology"},
        ]
        doctor = random.choice(doctors)
        
        appointment_times = [
            "2026-02-11 09:00",
            "2026-02-11 10:00",
            "2026-02-11 14:00",
            "2026-02-12 09:00",
            "2026-02-12 11:00",
        ]
        appointment_time = random.choice(appointment_times)
        
        visit_reasons = [
            "skin allergy",
            "annual checkup",
            "chest pain",
            "child vaccination",
            "acne treatment",
        ]
        visit_reason = random.choice(visit_reasons)
        
        return AppointmentRow(
            id=0,  # Auto-generated
            doctor_id=doctor["id"],
            patient_name="John Doe",  # From LOW profile
            patient_phone="+1-555-0123",  # From HIGH profile (via request_permission)
            patient_dob="1990-01-15",  # From HIGH profile (via request_permission)
            patient_email="john.doe@email.com",  # From LOW profile
            patient_gender="male",  # From LOW profile
            insurance_provider=None,  # Optional
            insurance_id=None,  # Optional
            visit_reason=visit_reason,
            appointment_time=appointment_time,
            status="confirmed",
            created_at=""
        )

    def validate_addition_integrity(
        self,
        before: list[AppointmentRow],
        after: list[AppointmentRow],
        reference_rows: list[AppointmentRow],
    ) -> bool:
        """Validate that the appointment was added correctly."""
        return sqlite_validators.validate_rows_addition_integrity(
            before,
            after,
            reference_rows,
            compare_fields=[
                'doctor_id',
                'patient_name',
                'visit_reason',
                'appointment_time'
            ]
        )

    @classmethod
    def generate_random_params(cls) -> dict[str, Any]:
        """Generate random parameters for a new appointment task."""
        appointment = cls._get_random_target_row()
        
        # Extract date and time from appointment_time
        date_time_parts = appointment.appointment_time.split(' ')
        appointment_date = date_time_parts[0]
        appointment_time_str = date_time_parts[1] if len(date_time_parts) > 1 else "09:00"
        
        # Get doctor name from doctor_id
        doctor_map = {
            1: "Dr. Sarah Smith",
            2: "Dr. Michael Johnson",
            3: "Dr. Emily Chen",
            4: "Dr. James Wilson",
            5: "Dr. Lisa Anderson",
        }
        specialty_map = {
            1: "Dermatology",
            2: "General Practice",
            3: "Cardiology",
            4: "Pediatrics",
            5: "Dermatology",
        }
        
        doctor_name = doctor_map.get(appointment.doctor_id, "Dr. Sarah Smith")
        specialty = specialty_map.get(appointment.doctor_id, "Dermatology")
        
        return {
            'doctor_name': doctor_name,
            'specialty': specialty,
            'appointment_date': appointment_date,
            'appointment_time': appointment_time_str,
            'visit_reason': appointment.visit_reason,
            sqlite_validators.ROW_OBJECTS: [appointment],
            sqlite_validators.NOISE_ROW_OBJECTS: []  # No noise for now
        }

