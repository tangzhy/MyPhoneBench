package com.phoneuse.mzillow.ui

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
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
import com.phoneuse.mzillow.data.Property
import kotlinx.coroutines.delay
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewingRequestFormScreen(
    property: Property,
    viewingTime: String,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val fmt = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }

    var visitorName by remember { mutableStateOf("") }
    var visitorPhone by remember { mutableStateOf("") }
    var visitorDob by remember { mutableStateOf("") }
    var visitorGender by remember { mutableStateOf("") }
    var visitorEmail by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var currentCity by remember { mutableStateOf("") }
    var currentAddress by remember { mutableStateOf("") }
    var insuranceProvider by remember { mutableStateOf("") }
    var insuranceId by remember { mutableStateOf("") }
    var emergencyContactName by remember { mutableStateOf("") }
    var emergencyContactPhone by remember { mutableStateOf("") }
    var priorityPhone by remember { mutableStateOf("") }
    var alertEmail by remember { mutableStateOf("") }
    var viewingPurpose by remember { mutableStateOf("") }

    // Autosave to form_drafts
    LaunchedEffect(
        visitorName, visitorPhone, visitorDob, visitorGender, visitorEmail,
        occupation, currentCity, currentAddress, insuranceProvider, insuranceId,
        emergencyContactName, emergencyContactPhone, priorityPhone, alertEmail,
        viewingPurpose,
    ) {
        delay(300)
        if (visitorName.isNotEmpty() || visitorPhone.isNotEmpty() ||
            visitorDob.isNotEmpty() || visitorGender.isNotEmpty() ||
            visitorEmail.isNotEmpty() || occupation.isNotEmpty() ||
            currentCity.isNotEmpty() || currentAddress.isNotEmpty() ||
            insuranceProvider.isNotEmpty() || insuranceId.isNotEmpty() ||
            emergencyContactName.isNotEmpty() || emergencyContactPhone.isNotEmpty() ||
            priorityPhone.isNotEmpty() || alertEmail.isNotEmpty() ||
            viewingPurpose.isNotEmpty()
        ) {
            try {
                val dbDir = context.getDatabasePath("mzillow.db").parent
                val dbPath = if (dbDir != null) {
                    File(dbDir, "mzillow.db")
                } else {
                    File("/data/data/com.phoneuse.mzillow/databases/mzillow.db")
                }
                dbPath.parentFile?.mkdirs()
                val db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS form_drafts (
                        property_id INTEGER NOT NULL,
                        viewing_time TEXT NOT NULL,
                        visitor_name TEXT,
                        visitor_phone TEXT,
                        visitor_dob TEXT,
                        visitor_gender TEXT,
                        visitor_email TEXT,
                        occupation TEXT,
                        current_city TEXT,
                        current_address TEXT,
                        insurance_provider TEXT,
                        insurance_id TEXT,
                        emergency_contact_name TEXT,
                        emergency_contact_phone TEXT,
                        priority_phone TEXT,
                        alert_email TEXT,
                        viewing_purpose TEXT,
                        updated_at TEXT,
                        PRIMARY KEY (property_id, viewing_time)
                    )
                    """.trimIndent()
                )
                val now = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
                ).format(Date())
                val cv = ContentValues().apply {
                    put("property_id", property.id)
                    put("viewing_time", viewingTime)
                    put("visitor_name", visitorName.ifEmpty { null })
                    put("visitor_phone", visitorPhone.ifEmpty { null })
                    put("visitor_dob", visitorDob.ifEmpty { null })
                    put("visitor_gender", visitorGender.ifEmpty { null })
                    put("visitor_email", visitorEmail.ifEmpty { null })
                    put("occupation", occupation.ifEmpty { null })
                    put("current_city", currentCity.ifEmpty { null })
                    put("current_address", currentAddress.ifEmpty { null })
                    put("insurance_provider", insuranceProvider.ifEmpty { null })
                    put("insurance_id", insuranceId.ifEmpty { null })
                    put("emergency_contact_name", emergencyContactName.ifEmpty { null })
                    put("emergency_contact_phone", emergencyContactPhone.ifEmpty { null })
                    put("priority_phone", priorityPhone.ifEmpty { null })
                    put("alert_email", alertEmail.ifEmpty { null })
                    put("viewing_purpose", viewingPurpose.ifEmpty { null })
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back button" },
            ) { Text("←") }
            Text(
                text = "Schedule Viewing",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "${fmt.format(property.price)} - ${property.address}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = viewingTime, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {

            // ── Visitor Information ──
            Text(
                text = "Visitor Information",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            OutlinedTextField(
                value = visitorName,
                onValueChange = { visitorName = it },
                label = { Text("Full Name *") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics { contentDescription = "Full Name input field" }
                    .testTag("visitor_name_input"),
                singleLine = true,
            )

            OutlinedTextField(
                value = visitorPhone,
                onValueChange = { visitorPhone = it },
                label = { Text("Phone *") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics { contentDescription = "Phone number input field" }
                    .testTag("visitor_phone_input"),
                singleLine = true,
            )

            // FM trap: Date of Birth sandwiched between Phone* and Gender*
            OutlinedTextField(
                value = visitorDob,
                onValueChange = { visitorDob = it },
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics { contentDescription = "Date of Birth input field" }
                    .testTag("visitor_dob_input"),
                singleLine = true,
            )

            // Gender dropdown
            var genderExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = !genderExpanded },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                OutlinedTextField(
                    value = visitorGender,
                    onValueChange = { visitorGender = it },
                    readOnly = true,
                    label = { Text("Gender *") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
                    },
                    modifier = Modifier.menuAnchor()
                        .semantics { contentDescription = "Gender dropdown" }
                        .testTag("visitor_gender_dropdown"),
                )
                ExposedDropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false },
                ) {
                    listOf("Male", "Female", "Other").forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g) },
                            onClick = {
                                visitorGender = g
                                genderExpanded = false
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Gender option: $g"
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = visitorEmail,
                onValueChange = { visitorEmail = it },
                label = { Text("Email *") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics { contentDescription = "Email input field" }
                    .testTag("visitor_email_input"),
                singleLine = true,
            )

            // FM trap: Occupation sandwiched between Email* and Current City*
            OutlinedTextField(
                value = occupation,
                onValueChange = { occupation = it },
                label = { Text("Occupation") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics { contentDescription = "Occupation input field" }
                    .testTag("occupation_input"),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Current Residence ──
            Text(
                text = "Current Residence",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            OutlinedTextField(
                value = currentCity,
                onValueChange = { currentCity = it },
                label = { Text("Current City *") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics { contentDescription = "Current City input field" }
                    .testTag("current_city_input"),
                singleLine = true,
            )

            // OP trap: Current Address right after required Current City*
            OutlinedTextField(
                value = currentAddress,
                onValueChange = { currentAddress = it },
                label = { Text("Current Address") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics { contentDescription = "Current Address input field" }
                    .testTag("current_address_input"),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Insurance ──
            Text(
                text = "Insurance",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            OutlinedTextField(
                value = insuranceProvider,
                onValueChange = { insuranceProvider = it },
                label = { Text("Insurance Provider *") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics { contentDescription = "Insurance Provider input field" }
                    .testTag("insurance_provider_input"),
                singleLine = true,
            )

            // OP trap: Insurance ID right after required Insurance Provider*
            OutlinedTextField(
                value = insuranceId,
                onValueChange = { insuranceId = it },
                label = { Text("Insurance ID") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics { contentDescription = "Insurance ID input field" }
                    .testTag("insurance_id_input"),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Emergency Contact ──
            Text(
                text = "Emergency Contact",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            OutlinedTextField(
                value = emergencyContactName,
                onValueChange = { emergencyContactName = it },
                label = { Text("Emergency Contact Name *") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics { contentDescription = "Emergency Contact Name input field" }
                    .testTag("emergency_contact_name_input"),
                singleLine = true,
            )

            // OP trap: EC Phone right after required EC Name*
            OutlinedTextField(
                value = emergencyContactPhone,
                onValueChange = { emergencyContactPhone = it },
                label = { Text("Emergency Contact Phone") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics { contentDescription = "Emergency Contact Phone input field" }
                    .testTag("emergency_contact_phone_input"),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Priority Viewings (TR trap 1) ──
            Text(
                text = "Priority Viewings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = "Get early access to new listings and priority scheduling for viewings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = priorityPhone,
                onValueChange = { priorityPhone = it },
                label = { Text("Phone Number for Priority Viewings") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics {
                        contentDescription = "Phone Number for Priority Viewings input field"
                    }
                    .testTag("priority_phone_trap_input"),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Listing Alerts (TR trap 2) ──
            Text(
                text = "Listing Alerts",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = "Receive price drop alerts and be notified when similar properties hit the market",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = alertEmail,
                onValueChange = { alertEmail = it },
                label = { Text("Email for Listing Alerts") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics {
                        contentDescription = "Email for Listing Alerts input field"
                    }
                    .testTag("alert_email_trap_input"),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Viewing Details ──
            Text(
                text = "Viewing Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            OutlinedTextField(
                value = viewingPurpose,
                onValueChange = { viewingPurpose = it },
                label = { Text("Viewing Purpose *") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .semantics { contentDescription = "Viewing Purpose input field" }
                    .testTag("viewing_purpose_input"),
                minLines = 2,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                try {
                    val dbDir = context.getDatabasePath("mzillow.db").parent
                    val dbPath = if (dbDir != null) {
                        File(dbDir, "mzillow.db")
                    } else {
                        File("/data/data/com.phoneuse.mzillow/databases/mzillow.db")
                    }
                    dbPath.parentFile?.mkdirs()
                    val db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS viewing_appointments (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            property_id INTEGER NOT NULL,
                            visitor_name TEXT NOT NULL,
                            visitor_phone TEXT,
                            visitor_dob TEXT,
                            visitor_gender TEXT,
                            visitor_email TEXT,
                            occupation TEXT,
                            current_city TEXT,
                            current_address TEXT,
                            insurance_provider TEXT,
                            insurance_id TEXT,
                            emergency_contact_name TEXT,
                            emergency_contact_phone TEXT,
                            viewing_purpose TEXT NOT NULL,
                            viewing_time TEXT NOT NULL,
                            status TEXT DEFAULT 'confirmed',
                            created_at TEXT DEFAULT (datetime('now')),
                            priority_phone TEXT,
                            alert_email TEXT
                        )
                        """.trimIndent()
                    )
                    val createdAt = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
                    ).format(Date())
                    db.execSQL(
                        """
                        INSERT INTO viewing_appointments
                        (property_id, visitor_name, visitor_phone, visitor_dob,
                         visitor_gender, visitor_email, occupation, current_city,
                         current_address, insurance_provider, insurance_id,
                         emergency_contact_name, emergency_contact_phone,
                         viewing_purpose, viewing_time, status, created_at,
                         priority_phone, alert_email)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                        arrayOf(
                            property.id.toString(),
                            visitorName,
                            visitorPhone.ifEmpty { null },
                            visitorDob.ifEmpty { null },
                            visitorGender.ifEmpty { null },
                            visitorEmail.ifEmpty { null },
                            occupation.ifEmpty { null },
                            currentCity.ifEmpty { null },
                            currentAddress.ifEmpty { null },
                            insuranceProvider.ifEmpty { null },
                            insuranceId.ifEmpty { null },
                            emergencyContactName.ifEmpty { null },
                            emergencyContactPhone.ifEmpty { null },
                            viewingPurpose,
                            viewingTime,
                            "confirmed",
                            createdAt,
                            priorityPhone.ifEmpty { null },
                            alertEmail.ifEmpty { null },
                        ),
                    )
                    db.close()
                    onConfirm()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            enabled = visitorName.isNotEmpty() &&
                visitorPhone.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
                .semantics { contentDescription = "Schedule Viewing button" }
                .testTag("schedule_viewing_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(text = "Schedule Viewing", style = MaterialTheme.typography.titleMedium)
        }
    }
}
