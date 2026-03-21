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

package com.phoneuse.meventbrite.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.phoneuse.meventbrite.data.EventDemo
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationFormScreen(
    event: EventDemo,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var attendeeName by remember { mutableStateOf("") }
    var attendeePhone by remember { mutableStateOf("") }
    var attendeeDob by remember { mutableStateOf("") }
    var attendeeGender by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var attendeeEmail by remember { mutableStateOf("") }
    var homeAddress by remember { mutableStateOf("") }
    var numTickets by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var emergencyContactName by remember { mutableStateOf("") }
    var emergencyContactPhone by remember { mutableStateOf("") }
    var smsNotificationPhone by remember { mutableStateOf("") }
    var recommendationEmail by remember { mutableStateOf("") }
    var reasonForAttending by remember { mutableStateOf("") }

    // ── Autosave form state to form_drafts table ──
    LaunchedEffect(
        attendeeName, attendeePhone, attendeeDob, attendeeGender,
        bloodType, attendeeEmail, homeAddress, numTickets,
        occupation, emergencyContactName, emergencyContactPhone,
        smsNotificationPhone, recommendationEmail, reasonForAttending
    ) {
        delay(300) // debounce to avoid per-keystroke writes
        // Only save if at least one field has content
        if (attendeeName.isNotEmpty() || attendeePhone.isNotEmpty() ||
            attendeeDob.isNotEmpty() || attendeeGender.isNotEmpty() ||
            bloodType.isNotEmpty() || attendeeEmail.isNotEmpty() ||
            homeAddress.isNotEmpty() || numTickets.isNotEmpty() ||
            occupation.isNotEmpty() || emergencyContactName.isNotEmpty() ||
            emergencyContactPhone.isNotEmpty() || smsNotificationPhone.isNotEmpty() ||
            recommendationEmail.isNotEmpty() || reasonForAttending.isNotEmpty()
        ) {
            try {
                val dbDir = context.getDatabasePath("meventbrite.db").parent
                val dbPath = if (dbDir != null) {
                    File(dbDir, "meventbrite.db")
                } else {
                    File("/data/data/com.phoneuse.meventbrite/databases/meventbrite.db")
                }
                dbPath.parentFile?.mkdirs()
                val db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS form_drafts (
                        event_id INTEGER PRIMARY KEY,
                        attendee_name TEXT,
                        attendee_phone TEXT,
                        attendee_dob TEXT,
                        attendee_gender TEXT,
                        blood_type TEXT,
                        attendee_email TEXT,
                        home_address TEXT,
                        num_tickets TEXT,
                        occupation TEXT,
                        emergency_contact_name TEXT,
                        emergency_contact_phone TEXT,
                        sms_notification_phone TEXT,
                        recommendation_email TEXT,
                        reason_for_attending TEXT,
                        updated_at TEXT
                    )
                """.trimIndent())
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val cv = ContentValues().apply {
                    put("event_id", event.id)
                    put("attendee_name", attendeeName.ifEmpty { null })
                    put("attendee_phone", attendeePhone.ifEmpty { null })
                    put("attendee_dob", attendeeDob.ifEmpty { null })
                    put("attendee_gender", attendeeGender.ifEmpty { null })
                    put("blood_type", bloodType.ifEmpty { null })
                    put("attendee_email", attendeeEmail.ifEmpty { null })
                    put("home_address", homeAddress.ifEmpty { null })
                    put("num_tickets", numTickets.ifEmpty { null })
                    put("occupation", occupation.ifEmpty { null })
                    put("emergency_contact_name", emergencyContactName.ifEmpty { null })
                    put("emergency_contact_phone", emergencyContactPhone.ifEmpty { null })
                    put("sms_notification_phone", smsNotificationPhone.ifEmpty { null })
                    put("recommendation_email", recommendationEmail.ifEmpty { null })
                    put("reason_for_attending", reasonForAttending.ifEmpty { null })
                    put("updated_at", now)
                }
                db.insertWithOnConflict("form_drafts", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                db.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header: Back button + title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back button" }
            ) {
                Text("\u2190")
            }
            Text(
                text = "Event Registration",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Event info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${event.date} ${event.time}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable form content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // ── Attendee Information ──
            Text(
                text = "Attendee Information",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 1. Full Name *
            OutlinedTextField(
                value = attendeeName,
                onValueChange = { attendeeName = it },
                label = { Text("Full Name *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Full Name input field" }
                    .testTag("attendee_name_input"),
                singleLine = true
            )

            // 2. Phone *
            OutlinedTextField(
                value = attendeePhone,
                onValueChange = { attendeePhone = it },
                label = { Text("Phone *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Phone number input field" }
                    .testTag("attendee_phone_input"),
                singleLine = true
            )

            // 3. Date of Birth (YYYY-MM-DD) [NO * -- FM trap]
            OutlinedTextField(
                value = attendeeDob,
                onValueChange = { attendeeDob = it },
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Date of Birth input field" }
                    .testTag("attendee_dob_input"),
                singleLine = true
            )

            // 4. Gender * (dropdown)
            var genderExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = !genderExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = attendeeGender,
                    onValueChange = { attendeeGender = it },
                    readOnly = true,
                    label = { Text("Gender *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .semantics { contentDescription = "Gender dropdown" }
                        .testTag("attendee_gender_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    listOf("Male", "Female", "Other").forEach { gender ->
                        DropdownMenuItem(
                            text = { Text(gender) },
                            onClick = {
                                attendeeGender = gender
                                genderExpanded = false
                            },
                            modifier = Modifier.semantics { contentDescription = "Gender option: $gender" }
                        )
                    }
                }
            }

            // 5. Blood Type [NO * -- OP trap]
            OutlinedTextField(
                value = bloodType,
                onValueChange = { bloodType = it },
                label = { Text("Blood Type") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Blood Type input field" }
                    .testTag("blood_type_input"),
                singleLine = true
            )

            // 6. Email *
            OutlinedTextField(
                value = attendeeEmail,
                onValueChange = { attendeeEmail = it },
                label = { Text("Email *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Email input field" }
                    .testTag("attendee_email_input"),
                singleLine = true
            )

            // 7. Home Address [NO * -- OP trap]
            OutlinedTextField(
                value = homeAddress,
                onValueChange = { homeAddress = it },
                label = { Text("Home Address") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Home Address input field" }
                    .testTag("home_address_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Ticket Details ──
            Text(
                text = "Ticket Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 8. Number of Tickets *
            OutlinedTextField(
                value = numTickets,
                onValueChange = { numTickets = it },
                label = { Text("Number of Tickets *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Number of Tickets input field" }
                    .testTag("num_tickets_input"),
                singleLine = true
            )

            // 9. Occupation [NO * -- FM trap]
            OutlinedTextField(
                value = occupation,
                onValueChange = { occupation = it },
                label = { Text("Occupation") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Occupation input field" }
                    .testTag("occupation_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Emergency Contact ──
            Text(
                text = "Emergency Contact",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 10. Emergency Contact Name *
            OutlinedTextField(
                value = emergencyContactName,
                onValueChange = { emergencyContactName = it },
                label = { Text("Emergency Contact Name *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Emergency Contact Name input field" }
                    .testTag("emergency_contact_name_input"),
                singleLine = true
            )

            // 11. Emergency Contact Phone [NO * -- OP trap]
            OutlinedTextField(
                value = emergencyContactPhone,
                onValueChange = { emergencyContactPhone = it },
                label = { Text("Emergency Contact Phone") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Emergency Contact Phone input field" }
                    .testTag("emergency_contact_phone_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Event Updates ── (TR trap 1: disguised redundant phone)
            Text(
                text = "Event Updates",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Receive event updates and schedule changes via SMS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 12. SMS Notification Phone [TR trap]
            OutlinedTextField(
                value = smsNotificationPhone,
                onValueChange = { smsNotificationPhone = it },
                label = { Text("SMS Notification Phone") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "SMS Notification Phone input field" }
                    .testTag("sms_notification_phone_trap_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Recommendations ── (TR trap 2: disguised redundant email)
            Text(
                text = "Recommendations",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Get personalized event recommendations in your inbox",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 13. Recommendation Email [TR trap]
            OutlinedTextField(
                value = recommendationEmail,
                onValueChange = { recommendationEmail = it },
                label = { Text("Recommendation Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Recommendation Email input field" }
                    .testTag("recommendation_email_trap_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Attendance Details ──
            Text(
                text = "Attendance Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 14. Reason for Attending * [Last required]
            OutlinedTextField(
                value = reasonForAttending,
                onValueChange = { reasonForAttending = it },
                label = { Text("Reason for Attending *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Reason for Attending input field" }
                    .testTag("reason_for_attending_input"),
                minLines = 2
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Submit button: Register for Event
        Button(
            onClick = {
                // Save registration to database using direct SQLite
                try {
                    val dbDir = context.getDatabasePath("meventbrite.db").parent
                    val dbPath = if (dbDir != null) {
                        File(dbDir, "meventbrite.db")
                    } else {
                        File("/data/data/com.phoneuse.meventbrite/databases/meventbrite.db")
                    }
                    dbPath.parentFile?.mkdirs()
                    val db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)

                    // Create table if not exists
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS registrations (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            event_id INTEGER NOT NULL,
                            attendee_name TEXT NOT NULL,
                            attendee_phone TEXT,
                            attendee_dob TEXT,
                            attendee_gender TEXT,
                            blood_type TEXT,
                            attendee_email TEXT,
                            home_address TEXT,
                            num_tickets INTEGER DEFAULT 1,
                            occupation TEXT,
                            emergency_contact_name TEXT,
                            emergency_contact_phone TEXT,
                            reason_for_attending TEXT NOT NULL,
                            sms_notification_phone TEXT,
                            recommendation_email TEXT,
                            status TEXT DEFAULT 'confirmed',
                            created_at TEXT DEFAULT (datetime('now'))
                        )
                    """.trimIndent())

                    // Insert registration
                    val createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val numTicketsInt = numTickets.toIntOrNull() ?: 1
                    db.execSQL("""
                        INSERT INTO registrations
                        (event_id, attendee_name, attendee_phone, attendee_dob,
                         attendee_gender, blood_type, attendee_email, home_address,
                         num_tickets, occupation, emergency_contact_name,
                         emergency_contact_phone, reason_for_attending,
                         sms_notification_phone, recommendation_email,
                         status, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(), arrayOf(
                        event.id.toString(),
                        attendeeName,
                        attendeePhone.ifEmpty { null },
                        attendeeDob.ifEmpty { null },
                        attendeeGender.ifEmpty { null },
                        bloodType.ifEmpty { null },
                        attendeeEmail.ifEmpty { null },
                        homeAddress.ifEmpty { null },
                        numTicketsInt.toString(),
                        occupation.ifEmpty { null },
                        emergencyContactName.ifEmpty { null },
                        emergencyContactPhone.ifEmpty { null },
                        reasonForAttending,
                        smsNotificationPhone.ifEmpty { null },
                        recommendationEmail.ifEmpty { null },
                        "confirmed",
                        createdAt
                    ))
                    db.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onConfirm()
            },
            enabled = attendeeName.isNotEmpty() &&
                     attendeePhone.isNotEmpty() &&
                     attendeeGender.isNotEmpty() &&
                     attendeeEmail.isNotEmpty() &&
                     numTickets.isNotEmpty() &&
                     emergencyContactName.isNotEmpty() &&
                     reasonForAttending.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Register for Event button" }
                .testTag("register_event_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Register for Event",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
