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
import com.phoneuse.meventbrite.data.DataLoader
import com.phoneuse.meventbrite.data.EventDemo
import android.database.sqlite.SQLiteDatabase
import java.io.File

data class RegistrationItem(
    val id: Int,
    val eventId: Int,
    val eventTitle: String,
    val category: String,
    val venue: String,
    val eventDate: String,
    val eventTime: String,
    val reasonForAttending: String,
    val attendeeName: String
)

@Composable
fun MyRegistrationsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var registrations by remember { mutableStateOf<List<RegistrationItem>>(emptyList()) }
    var events by remember { mutableStateOf<List<EventDemo>>(emptyList()) }

    // Load events for mapping event_id to event info
    LaunchedEffect(Unit) {
        try {
            events = DataLoader.loadAllFromJson(context)
        } catch (e: Exception) {
            events = DataLoader.getDefaultEvents()
        }
    }

    // Load registrations from database
    LaunchedEffect(events) {
        try {
            val dbDir = context.getDatabasePath("meventbrite.db").parent
            val dbPath = if (dbDir != null) {
                File(dbDir, "meventbrite.db")
            } else {
                File("/data/data/com.phoneuse.meventbrite/databases/meventbrite.db")
            }
            if (dbPath.exists()) {
                val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                val cursor = db.rawQuery(
                    "SELECT id, event_id, attendee_name, reason_for_attending FROM registrations ORDER BY created_at DESC",
                    null
                )

                val registrationList = mutableListOf<RegistrationItem>()
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0)
                    val eventId = cursor.getInt(1)
                    val attendeeName = cursor.getString(2) ?: ""
                    val reasonForAttending = cursor.getString(3) ?: ""

                    // Find event info
                    val event = events.find { it.id == eventId }
                    if (event != null) {
                        registrationList.add(
                            RegistrationItem(
                                id = id,
                                eventId = eventId,
                                eventTitle = event.title,
                                category = event.category,
                                venue = event.venue,
                                eventDate = event.date,
                                eventTime = event.time,
                                reasonForAttending = reasonForAttending,
                                attendeeName = attendeeName
                            )
                        )
                    }
                }
                cursor.close()
                db.close()
                registrations = registrationList
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
                Text("\u2190")
            }
            Text(
                text = "My Registrations",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (registrations.isEmpty()) {
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
                        text = "No registrations yet",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Register for an event to see it here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(registrations) { registration ->
                    RegistrationCard(
                        registration = registration,
                        onCancel = {
                            // Delete registration from database
                            try {
                                val dbDir = context.getDatabasePath("meventbrite.db").parent
                                val dbPath = if (dbDir != null) {
                                    File(dbDir, "meventbrite.db")
                                } else {
                                    File("/data/data/com.phoneuse.meventbrite/databases/meventbrite.db")
                                }
                                val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
                                db.execSQL("DELETE FROM registrations WHERE id = ?", arrayOf(registration.id.toString()))
                                db.close()

                                // Reload registrations
                                registrations = registrations.filter { it.id != registration.id }
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
fun RegistrationCard(
    registration: RegistrationItem,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Registration card: ${registration.eventTitle}" }
            .testTag("registration_card_${registration.id}"),
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
                        text = registration.eventTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = registration.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = registration.venue,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .semantics { contentDescription = "Cancel registration button" }
                        .testTag("cancel_registration_${registration.id}")
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
                        text = "Date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${registration.eventDate} ${registration.eventTime}",
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
                        text = registration.reasonForAttending,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
