// Copyright 2025 PhoneUse Privacy Protocol Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.phoneuse.mzocdoc.data

/**
 * Doctor data class (Room annotations removed for now).
 */
data class Doctor(
    val id: Int,
    val name: String,
    val specialty: String,
    val hospital: String,
    val city: String,
    val rating: Double = 4.5,
    val availableSlots: String  // JSON array string
)

/**
 * Appointment data class (Room annotations removed for now).
 */
data class Appointment(
    val id: Int = 0,
    val doctorId: Int,
    val patientName: String,
    val patientPhone: String? = null,
    val patientDob: String? = null,
    val patientEmail: String? = null,
    val patientGender: String? = null,
    val insuranceProvider: String? = null,
    val insuranceId: String? = null,
    val visitReason: String,
    val appointmentTime: String,
    val status: String = "confirmed",
    val createdAt: String = ""
)

/**
 * Demo data class for JSON loading (before Room integration).
 */
data class DoctorDemo(
    val id: Int,
    val name: String,
    val specialty: String,
    val hospital: String,
    val city: String,
    val rating: Double = 4.5,
    val availableSlots: List<String>
)

