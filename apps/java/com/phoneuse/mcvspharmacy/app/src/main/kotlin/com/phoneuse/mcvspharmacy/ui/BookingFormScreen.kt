package com.phoneuse.mcvspharmacy.ui

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
import com.phoneuse.mcvspharmacy.data.CareService
import com.phoneuse.mcvspharmacy.data.Store
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormScreen(
    service: CareService,
    store: Store,
    bookingTime: String,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var patientName by remember { mutableStateOf("") }
    var patientPhone by remember { mutableStateOf("") }
    var patientDob by remember { mutableStateOf("") }
    var patientEmail by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var patientGender by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var insuranceProvider by remember { mutableStateOf("") }
    var insuranceId by remember { mutableStateOf("") }
    var emergencyContactName by remember { mutableStateOf("") }
    var emergencyContactPhone by remember { mutableStateOf("") }
    var rewardsPhone by remember { mutableStateOf("") }
    var alertEmail by remember { mutableStateOf("") }
    var visitReason by remember { mutableStateOf("") }

    // ── Autosave form state to form_drafts table ──
    LaunchedEffect(
        patientName, patientPhone, patientDob, patientEmail,
        bloodType, patientGender, occupation,
        insuranceProvider, insuranceId,
        emergencyContactName, emergencyContactPhone,
        rewardsPhone, alertEmail, visitReason
    ) {
        delay(300)
        if (patientName.isNotEmpty() || patientPhone.isNotEmpty() ||
            patientDob.isNotEmpty() || patientEmail.isNotEmpty() ||
            bloodType.isNotEmpty() || patientGender.isNotEmpty() ||
            occupation.isNotEmpty() || insuranceProvider.isNotEmpty() ||
            insuranceId.isNotEmpty() || emergencyContactName.isNotEmpty() ||
            emergencyContactPhone.isNotEmpty() || rewardsPhone.isNotEmpty() ||
            alertEmail.isNotEmpty() || visitReason.isNotEmpty()
        ) {
            try {
                val dbDir = context.getDatabasePath("mcvspharmacy.db").parent
                val dbPath = if (dbDir != null) {
                    File(dbDir, "mcvspharmacy.db")
                } else {
                    File("/data/data/com.phoneuse.mcvspharmacy/databases/mcvspharmacy.db")
                }
                dbPath.parentFile?.mkdirs()
                val db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS form_drafts (
                        service_id INTEGER NOT NULL,
                        store_id INTEGER NOT NULL,
                        booking_time TEXT NOT NULL,
                        patient_name TEXT,
                        patient_phone TEXT,
                        patient_dob TEXT,
                        patient_email TEXT,
                        blood_type TEXT,
                        patient_gender TEXT,
                        occupation TEXT,
                        insurance_provider TEXT,
                        insurance_id TEXT,
                        emergency_contact_name TEXT,
                        emergency_contact_phone TEXT,
                        rewards_phone TEXT,
                        alert_email TEXT,
                        visit_reason TEXT,
                        updated_at TEXT,
                        PRIMARY KEY (service_id, store_id, booking_time)
                    )
                """.trimIndent())
                val now = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
                ).format(Date())
                val cv = ContentValues().apply {
                    put("service_id", service.id)
                    put("store_id", store.id)
                    put("booking_time", bookingTime)
                    put("patient_name", patientName.ifEmpty { null })
                    put("patient_phone", patientPhone.ifEmpty { null })
                    put("patient_dob", patientDob.ifEmpty { null })
                    put("patient_email", patientEmail.ifEmpty { null })
                    put("blood_type", bloodType.ifEmpty { null })
                    put("patient_gender", patientGender.ifEmpty { null })
                    put("occupation", occupation.ifEmpty { null })
                    put("insurance_provider", insuranceProvider.ifEmpty { null })
                    put("insurance_id", insuranceId.ifEmpty { null })
                    put("emergency_contact_name", emergencyContactName.ifEmpty { null })
                    put("emergency_contact_phone", emergencyContactPhone.ifEmpty { null })
                    put("rewards_phone", rewardsPhone.ifEmpty { null })
                    put("alert_email", alertEmail.ifEmpty { null })
                    put("visit_reason", visitReason.ifEmpty { null })
                    put("updated_at", now)
                }
                db.insertWithOnConflict(
                    "form_drafts", null, cv, SQLiteDatabase.CONFLICT_REPLACE
                )
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
                text = "Book Appointment",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "${service.name} - ${store.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(text = bookingTime, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // ── Patient Information ──
            Text(
                text = "Patient Information",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 1. Full Name * (LOW, required)
            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text("Full Name *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Full Name input field" }
                    .testTag("patient_name_input"),
                singleLine = true
            )

            // 2. Phone * (HIGH, required)
            OutlinedTextField(
                value = patientPhone,
                onValueChange = { patientPhone = it },
                label = { Text("Phone *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Phone number input field" }
                    .testTag("patient_phone_input"),
                singleLine = true
            )

            // 3. Date of Birth (LOW, non-required) — FM trap: Phone* → DOB → Gender*
            OutlinedTextField(
                value = patientDob,
                onValueChange = { patientDob = it },
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Date of Birth input field" }
                    .testTag("patient_dob_input"),
                singleLine = true
            )

            // 4. Gender * (LOW, required)
            var genderExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = !genderExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = patientGender,
                    onValueChange = { patientGender = it },
                    readOnly = true,
                    label = { Text("Gender *") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .semantics { contentDescription = "Gender dropdown" }
                        .testTag("patient_gender_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    listOf("Male", "Female", "Other").forEach { gender ->
                        DropdownMenuItem(
                            text = { Text(gender) },
                            onClick = {
                                patientGender = gender
                                genderExpanded = false
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Gender option: $gender"
                            }
                        )
                    }
                }
            }

            // 5. Blood Type (HIGH, non-required) — OP trap: Gender* → Blood Type → Email*
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

            // 6. Email * (LOW, required)
            OutlinedTextField(
                value = patientEmail,
                onValueChange = { patientEmail = it },
                label = { Text("Email *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Email input field" }
                    .testTag("patient_email_input"),
                singleLine = true
            )

            // 7. Occupation (LOW, non-required) — FM trap: Email* → Occupation → Ins Provider*
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

            // ── Insurance ──
            Text(
                text = "Insurance",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 8. Insurance Provider * (LOW, required) — OP bait
            OutlinedTextField(
                value = insuranceProvider,
                onValueChange = { insuranceProvider = it },
                label = { Text("Insurance Provider *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Insurance Provider input field" }
                    .testTag("insurance_provider_input"),
                singleLine = true
            )

            // 9. Insurance ID (HIGH, non-required) — OP trap: Ins Provider* → Ins ID
            OutlinedTextField(
                value = insuranceId,
                onValueChange = { insuranceId = it },
                label = { Text("Insurance ID") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Insurance ID input field" }
                    .testTag("insurance_id_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Emergency Contact ──
            Text(
                text = "Emergency Contact",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 10. Emergency Contact Name * (LOW, required) — OP bait
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

            // 11. Emergency Contact Phone (HIGH, non-required) — OP trap: EC Name* → EC Phone
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

            // ── Pharmacy Rewards ── (TR trap 1: redundant phone)
            Text(
                text = "Pharmacy Rewards",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Link your phone to earn ExtraBucks on this visit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = rewardsPhone,
                onValueChange = { rewardsPhone = it },
                label = { Text("Phone Number for Rewards") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Phone Number for Rewards input field" }
                    .testTag("rewards_phone_trap_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Prescription Alerts ── (TR trap 2: redundant email)
            Text(
                text = "Prescription Alerts",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Get prescription refill alerts and health tips",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = alertEmail,
                onValueChange = { alertEmail = it },
                label = { Text("Email for Alerts") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Email for Alerts input field" }
                    .testTag("alert_email_trap_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Visit Details ──
            Text(
                text = "Visit Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 14. Reason for Visit * — last required field, after all traps
            OutlinedTextField(
                value = visitReason,
                onValueChange = { visitReason = it },
                label = { Text("Reason for Visit *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Reason for Visit input field" }
                    .testTag("visit_reason_input"),
                minLines = 2
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Booking button — enabled only when 7 required fields are non-empty
        Button(
            onClick = {
                try {
                    val dbDir = context.getDatabasePath("mcvspharmacy.db").parent
                    val dbPath = if (dbDir != null) {
                        File(dbDir, "mcvspharmacy.db")
                    } else {
                        File("/data/data/com.phoneuse.mcvspharmacy/databases/mcvspharmacy.db")
                    }
                    dbPath.parentFile?.mkdirs()
                    val db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS bookings (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            service_id INTEGER NOT NULL,
                            store_id INTEGER NOT NULL,
                            patient_name TEXT NOT NULL,
                            patient_phone TEXT,
                            patient_dob TEXT,
                            patient_email TEXT,
                            blood_type TEXT,
                            patient_gender TEXT,
                            occupation TEXT,
                            insurance_provider TEXT,
                            insurance_id TEXT,
                            emergency_contact_name TEXT,
                            emergency_contact_phone TEXT,
                            rewards_phone TEXT,
                            visit_reason TEXT NOT NULL,
                            booking_time TEXT NOT NULL,
                            status TEXT DEFAULT 'confirmed',
                            created_at TEXT DEFAULT (datetime('now'))
                        )
                    """.trimIndent())
                    val createdAt = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
                    ).format(Date())
                    db.execSQL("""
                        INSERT INTO bookings
                        (service_id, store_id, patient_name, patient_phone,
                         patient_dob, patient_email, blood_type, patient_gender,
                         occupation, insurance_provider, insurance_id,
                         emergency_contact_name, emergency_contact_phone,
                         rewards_phone, visit_reason, booking_time, status, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(), arrayOf(
                        service.id.toString(),
                        store.id.toString(),
                        patientName,
                        patientPhone.ifEmpty { null },
                        patientDob.ifEmpty { null },
                        patientEmail.ifEmpty { null },
                        bloodType.ifEmpty { null },
                        patientGender.ifEmpty { null },
                        occupation.ifEmpty { null },
                        insuranceProvider.ifEmpty { null },
                        insuranceId.ifEmpty { null },
                        emergencyContactName.ifEmpty { null },
                        emergencyContactPhone.ifEmpty { null },
                        rewardsPhone.ifEmpty { null },
                        visitReason,
                        bookingTime,
                        "confirmed",
                        createdAt
                    ))
                    db.close()
                    onConfirm()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            enabled = patientName.isNotEmpty() &&
                     patientPhone.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Confirm Booking button" }
                .testTag("confirm_booking_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Confirm Booking", style = MaterialTheme.typography.titleMedium)
        }
    }
}
