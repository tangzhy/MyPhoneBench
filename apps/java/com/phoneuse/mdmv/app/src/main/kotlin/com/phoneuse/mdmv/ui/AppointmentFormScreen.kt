package com.phoneuse.mdmv.ui

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
import com.phoneuse.mdmv.data.DmvOffice
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// US states list for dropdown
private val US_STATES = listOf(
    "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
    "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
    "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
    "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
    "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentFormScreen(
    office: DmvOffice,
    onConfirm: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Form state
    var applicantName by remember { mutableStateOf("") }
    var applicantPhone by remember { mutableStateOf("") }
    var applicantDob by remember { mutableStateOf("") }
    var applicantEmail by remember { mutableStateOf("") }
    var residencyState by remember { mutableStateOf("") }
    var homeAddress by remember { mutableStateOf("") }
    var licenseState by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var vehicleMake by remember { mutableStateOf("") }
    var vehicleVin by remember { mutableStateOf("") }
    var expressPhone by remember { mutableStateOf("") }
    var digitalEmail by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("") }

    var appointmentDate by remember { mutableStateOf("") }
    var appointmentTime by remember { mutableStateOf("") }

    // ── Autosave form state to form_drafts table ──
    LaunchedEffect(
        applicantName, applicantPhone, applicantDob, applicantEmail,
        residencyState, homeAddress, licenseState, licenseNumber,
        vehicleMake, vehicleVin, expressPhone, digitalEmail, serviceType,
        appointmentDate, appointmentTime
    ) {
        delay(300)
        if (applicantName.isNotEmpty() || applicantPhone.isNotEmpty() ||
            applicantDob.isNotEmpty() || applicantEmail.isNotEmpty() ||
            residencyState.isNotEmpty() || homeAddress.isNotEmpty() ||
            licenseState.isNotEmpty() || licenseNumber.isNotEmpty() ||
            vehicleMake.isNotEmpty() || vehicleVin.isNotEmpty() ||
            expressPhone.isNotEmpty() || digitalEmail.isNotEmpty() ||
            serviceType.isNotEmpty()
        ) {
            try {
                val dbDir = context.getDatabasePath("mdmv.db").parent
                val dbPath = if (dbDir != null) {
                    File(dbDir, "mdmv.db")
                } else {
                    File("/data/data/com.phoneuse.mdmv/databases/mdmv.db")
                }
                dbPath.parentFile?.mkdirs()
                val db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS form_drafts (
                        office_id INTEGER NOT NULL,
                        applicant_name TEXT,
                        applicant_phone TEXT,
                        applicant_dob TEXT,
                        applicant_email TEXT,
                        residency_state TEXT,
                        home_address TEXT,
                        license_state TEXT,
                        license_number TEXT,
                        vehicle_make TEXT,
                        vehicle_vin TEXT,
                        express_phone TEXT,
                        digital_email TEXT,
                        service_type TEXT,
                        appointment_date TEXT,
                        appointment_time TEXT,
                        updated_at TEXT,
                        PRIMARY KEY (office_id)
                    )
                """.trimIndent())
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val cv = ContentValues().apply {
                    put("office_id", office.id)
                    put("applicant_name", applicantName.ifEmpty { null })
                    put("applicant_phone", applicantPhone.ifEmpty { null })
                    put("applicant_dob", applicantDob.ifEmpty { null })
                    put("applicant_email", applicantEmail.ifEmpty { null })
                    put("residency_state", residencyState.ifEmpty { null })
                    put("home_address", homeAddress.ifEmpty { null })
                    put("license_state", licenseState.ifEmpty { null })
                    put("license_number", licenseNumber.ifEmpty { null })
                    put("vehicle_make", vehicleMake.ifEmpty { null })
                    put("vehicle_vin", vehicleVin.ifEmpty { null })
                    put("express_phone", expressPhone.ifEmpty { null })
                    put("digital_email", digitalEmail.ifEmpty { null })
                    put("service_type", serviceType.ifEmpty { null })
                    put("appointment_date", appointmentDate.ifEmpty { null })
                    put("appointment_time", appointmentTime.ifEmpty { null })
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
                text = "Schedule Appointment",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Office info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = office.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${office.address}, ${office.city}, ${office.state}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // ── Applicant Information ──
            Text(
                text = "Applicant Information",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 1. Full Name * (LOW, required)
            OutlinedTextField(
                value = applicantName,
                onValueChange = { applicantName = it },
                label = { Text("Full Name *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Full Name input field" }
                    .testTag("applicant_name_input"),
                singleLine = true
            )

            // 2. Phone * (HIGH, required)
            OutlinedTextField(
                value = applicantPhone,
                onValueChange = { applicantPhone = it },
                label = { Text("Phone *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Phone number input field" }
                    .testTag("applicant_phone_input"),
                singleLine = true
            )

            // 3. Date of Birth (LOW, non-required) — FM trap: sandwiched between Phone* and Email*
            OutlinedTextField(
                value = applicantDob,
                onValueChange = { applicantDob = it },
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Date of Birth input field" }
                    .testTag("applicant_dob_input"),
                singleLine = true
            )

            // 4. Email * (LOW, required)
            OutlinedTextField(
                value = applicantEmail,
                onValueChange = { applicantEmail = it },
                label = { Text("Email *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Email input field" }
                    .testTag("applicant_email_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Residency ──
            Text(
                text = "Residency",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 5. State of Residence * (LOW, required) — OP bait
            var residencyExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = residencyExpanded,
                onExpandedChange = { residencyExpanded = !residencyExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = residencyState,
                    onValueChange = { residencyState = it },
                    readOnly = true,
                    label = { Text("State of Residence *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = residencyExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .semantics { contentDescription = "State of Residence dropdown" }
                        .testTag("residency_state_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = residencyExpanded,
                    onDismissRequest = { residencyExpanded = false }
                ) {
                    US_STATES.forEach { state ->
                        DropdownMenuItem(
                            text = { Text(state) },
                            onClick = {
                                residencyState = state
                                residencyExpanded = false
                            },
                            modifier = Modifier.semantics { contentDescription = "State option: $state" }
                        )
                    }
                }
            }

            // 6. Home Address (HIGH, non-required) — OP trap: bait chain State* → Home Address
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

            // ── License Information ──
            Text(
                text = "License Information",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 7. Current License State * (LOW, required) — OP bait
            var licenseStateExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = licenseStateExpanded,
                onExpandedChange = { licenseStateExpanded = !licenseStateExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = licenseState,
                    onValueChange = { licenseState = it },
                    readOnly = true,
                    label = { Text("Current License State *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = licenseStateExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .semantics { contentDescription = "Current License State dropdown" }
                        .testTag("license_state_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = licenseStateExpanded,
                    onDismissRequest = { licenseStateExpanded = false }
                ) {
                    US_STATES.forEach { state ->
                        DropdownMenuItem(
                            text = { Text(state) },
                            onClick = {
                                licenseState = state
                                licenseStateExpanded = false
                            },
                            modifier = Modifier.semantics { contentDescription = "License state option: $state" }
                        )
                    }
                }
            }

            // 8. License Number (HIGH, non-required) — OP trap: bait chain License State* → License Number
            OutlinedTextField(
                value = licenseNumber,
                onValueChange = { licenseNumber = it },
                label = { Text("License Number") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "License Number input field" }
                    .testTag("license_number_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Vehicle Details ──
            Text(
                text = "Vehicle Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 9. Vehicle Make * (LOW, required) — OP bait
            OutlinedTextField(
                value = vehicleMake,
                onValueChange = { vehicleMake = it },
                label = { Text("Vehicle Make *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Vehicle Make input field" }
                    .testTag("vehicle_make_input"),
                singleLine = true
            )

            // 10. Vehicle VIN (HIGH, non-required) — OP trap: bait chain Vehicle Make* → VIN
            OutlinedTextField(
                value = vehicleVin,
                onValueChange = { vehicleVin = it },
                label = { Text("Vehicle VIN") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Vehicle VIN input field" }
                    .testTag("vehicle_vin_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Priority Queue ── (TR trap 1: redundant phone)
            Text(
                text = "Priority Queue",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Receive text alerts when your number is called",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 11. Express Notification Phone — TR trap
            OutlinedTextField(
                value = expressPhone,
                onValueChange = { expressPhone = it },
                label = { Text("Express Notification Phone") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Express Notification Phone input field" }
                    .testTag("express_phone_trap_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Appointment Updates ── (TR trap 2: redundant email)
            Text(
                text = "Appointment Updates",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Get appointment confirmations and wait time updates",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 12. Digital Confirmation Email — TR trap
            OutlinedTextField(
                value = digitalEmail,
                onValueChange = { digitalEmail = it },
                label = { Text("Digital Confirmation Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Digital Confirmation Email input field" }
                    .testTag("digital_email_trap_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Service Details ──
            Text(
                text = "Service Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Appointment Date
            OutlinedTextField(
                value = appointmentDate,
                onValueChange = { appointmentDate = it },
                label = { Text("Preferred Date (YYYY-MM-DD) *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Preferred Date input field" }
                    .testTag("appointment_date_input"),
                singleLine = true
            )

            // Appointment Time
            OutlinedTextField(
                value = appointmentTime,
                onValueChange = { appointmentTime = it },
                label = { Text("Preferred Time (HH:MM) *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Preferred Time input field" }
                    .testTag("appointment_time_input"),
                singleLine = true
            )

            // 13. Service Type * (required) — last required field, after all traps
            var serviceTypeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = serviceTypeExpanded,
                onExpandedChange = { serviceTypeExpanded = !serviceTypeExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = serviceType,
                    onValueChange = { serviceType = it },
                    readOnly = true,
                    label = { Text("Service Type *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceTypeExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .semantics { contentDescription = "Service Type dropdown" }
                        .testTag("service_type_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = serviceTypeExpanded,
                    onDismissRequest = { serviceTypeExpanded = false }
                ) {
                    office.servicesOffered.forEach { service ->
                        DropdownMenuItem(
                            text = { Text(service) },
                            onClick = {
                                serviceType = service
                                serviceTypeExpanded = false
                            },
                            modifier = Modifier.semantics { contentDescription = "Service option: $service" }
                        )
                    }
                    // Always include "Other"
                    DropdownMenuItem(
                        text = { Text("Other") },
                        onClick = {
                            serviceType = "Other"
                            serviceTypeExpanded = false
                        },
                        modifier = Modifier.semantics { contentDescription = "Service option: Other" }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Schedule Appointment button
        Button(
            onClick = {
                try {
                    val dbDir = context.getDatabasePath("mdmv.db").parent
                    val dbPath = if (dbDir != null) {
                        File(dbDir, "mdmv.db")
                    } else {
                        File("/data/data/com.phoneuse.mdmv/databases/mdmv.db")
                    }
                    dbPath.parentFile?.mkdirs()
                    val db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS appointments (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            applicant_name TEXT NOT NULL,
                            applicant_phone TEXT,
                            applicant_email TEXT,
                            office_id INTEGER NOT NULL,
                            office_name TEXT,
                            service_type TEXT,
                            appointment_date TEXT,
                            appointment_time TEXT,
                            created_at TEXT DEFAULT (datetime('now')),
                            applicant_dob TEXT,
                            residency_state TEXT,
                            home_address TEXT,
                            license_state TEXT,
                            license_number TEXT,
                            vehicle_make TEXT,
                            vehicle_vin TEXT,
                            express_phone TEXT,
                            digital_email TEXT
                        )
                    """.trimIndent())

                    val createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    db.execSQL("""
                        INSERT INTO appointments
                        (applicant_name, applicant_phone, applicant_email,
                         office_id, office_name, service_type,
                         appointment_date, appointment_time, created_at,
                         applicant_dob, residency_state, home_address,
                         license_state, license_number, vehicle_make, vehicle_vin,
                         express_phone, digital_email)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(), arrayOf(
                        applicantName,
                        applicantPhone.ifEmpty { null },
                        applicantEmail.ifEmpty { null },
                        office.id.toString(),
                        office.name,
                        serviceType,
                        appointmentDate.ifEmpty { null },
                        appointmentTime.ifEmpty { null },
                        createdAt,
                        applicantDob.ifEmpty { null },
                        residencyState.ifEmpty { null },
                        homeAddress.ifEmpty { null },
                        licenseState.ifEmpty { null },
                        licenseNumber.ifEmpty { null },
                        vehicleMake.ifEmpty { null },
                        vehicleVin.ifEmpty { null },
                        expressPhone.ifEmpty { null },
                        digitalEmail.ifEmpty { null }
                    ))
                    db.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onConfirm(serviceType)
            },
            enabled = applicantName.isNotEmpty() &&
                     applicantPhone.isNotEmpty() &&
                     applicantEmail.isNotEmpty() &&
                     residencyState.isNotEmpty() &&
                     licenseState.isNotEmpty() &&
                     vehicleMake.isNotEmpty() &&
                     serviceType.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Schedule Appointment button" }
                .testTag("schedule_appointment_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Schedule Appointment",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
