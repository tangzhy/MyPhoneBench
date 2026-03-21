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

package com.phoneuse.mzocdoc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phoneuse.mzocdoc.data.DoctorDemo
import com.phoneuse.mzocdoc.data.DataLoader
import android.database.sqlite.SQLiteDatabase
import java.io.File

data class AppointmentItem(
    val id: Int,
    val doctorId: Int,
    val doctorName: String,
    val specialty: String,
    val hospital: String,
    val appointmentTime: String,
    val visitReason: String,
    val patientName: String
)

@Composable
fun MyAppointmentsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var appointments by remember { mutableStateOf<List<AppointmentItem>>(emptyList()) }
    var doctors by remember { mutableStateOf<List<DoctorDemo>>(emptyList()) }
    
    // Load doctors for mapping doctor_id to doctor info
    LaunchedEffect(Unit) {
        try {
            doctors = DataLoader.loadAllFromJson(context)
        } catch (e: Exception) {
            doctors = DataLoader.getDefaultDoctors()
        }
    }
    
    // Load appointments from database
    LaunchedEffect(doctors) {
        try {
            val dbDir = context.getDatabasePath("mzocdoc.db").parent
            val dbPath = if (dbDir != null) {
                File(dbDir, "mzocdoc.db")
            } else {
                File("/data/data/com.phoneuse.mzocdoc/databases/mzocdoc.db")
            }
            if (dbPath.exists()) {
                val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                val cursor = db.rawQuery(
                    "SELECT id, doctor_id, patient_name, visit_reason, appointment_time FROM appointments ORDER BY appointment_time ASC",
                    null
                )
                
                val appointmentList = mutableListOf<AppointmentItem>()
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0)
                    val doctorId = cursor.getInt(1)
                    val patientName = cursor.getString(2)
                    val visitReason = cursor.getString(3)
                    val appointmentTime = cursor.getString(4)
                    
                    // Find doctor info
                    val doctor = doctors.find { it.id == doctorId }
                    if (doctor != null) {
                        appointmentList.add(
                            AppointmentItem(
                                id = id,
                                doctorId = doctorId,
                                doctorName = doctor.name,
                                specialty = doctor.specialty,
                                hospital = doctor.hospital,
                                appointmentTime = appointmentTime,
                                visitReason = visitReason,
                                patientName = patientName
                            )
                        )
                    }
                }
                cursor.close()
                db.close()
                appointments = appointmentList
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back button" }
            ) {
                Text("←")
            }
            Text(
                text = "My Appointments",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (appointments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No appointments yet",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Book an appointment to see it here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(appointments) { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        onCancel = {
                            // Delete appointment from database
                            try {
                                val dbDir = context.getDatabasePath("mzocdoc.db").parent
                                val dbPath = if (dbDir != null) {
                                    File(dbDir, "mzocdoc.db")
                                } else {
                                    File("/data/data/com.phoneuse.mzocdoc/databases/mzocdoc.db")
                                }
                                val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
                                db.execSQL("DELETE FROM appointments WHERE id = ?", arrayOf(appointment.id.toString()))
                                db.close()
                                
                                // Reload appointments
                                appointments = appointments.filter { it.id != appointment.id }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppointmentCard(
    appointment: AppointmentItem,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Appointment card: ${appointment.doctorName}, ${appointment.appointmentTime}" }
            .testTag("appointment_card_${appointment.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = appointment.doctorName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = appointment.specialty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = appointment.hospital,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .semantics { contentDescription = "Cancel appointment button" }
                        .testTag("cancel_appointment_${appointment.id}")
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = appointment.appointmentTime,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "Reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = appointment.visitReason,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

