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

// Room DAO temporarily disabled - using direct SQLite operations instead
// @Dao
interface MZocdocDao {
    // Doctor operations - placeholder methods (not used without Room)
    fun getAllDoctors(): List<Doctor> = emptyList()
    fun getDoctorById(id: Int): Doctor? = null
    fun searchDoctors(specialty: String, query: String): List<Doctor> = emptyList()
    fun insertDoctor(doctor: Doctor): Long = 0
    fun insertDoctors(doctors: List<Doctor>) {}
    fun deleteAllDoctors() {}
    
    // Appointment operations - placeholder methods (not used without Room)
    fun getAllAppointments(): List<Appointment> = emptyList()
    fun getAppointmentsByDoctor(doctorId: Int): List<Appointment> = emptyList()
    fun getAppointmentById(id: Int): Appointment? = null
    fun insertAppointment(appointment: Appointment): Long = 0
    fun deleteAppointment(id: Int) {}
    fun deleteAllAppointments() {}
}

