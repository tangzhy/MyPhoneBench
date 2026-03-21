package com.phoneuse.mthumbtack.ui

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
import com.phoneuse.mthumbtack.data.DataLoader
import com.phoneuse.mthumbtack.data.ServiceRequest
import android.database.sqlite.SQLiteDatabase
import java.io.File

@Composable
fun MyRequestsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var requests by remember { mutableStateOf<List<ServiceRequest>>(emptyList()) }
    val pros = remember { DataLoader.loadAllFromJson(context) }

    LaunchedEffect(Unit) {
        try {
            val dbPath = context.getDatabasePath("mthumbtack.db")
            if (dbPath.exists()) {
                val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                try {
                    val cursor = db.rawQuery(
                        "SELECT id, pro_id, customer_name, customer_phone, project_description, appointment_time, status, created_at FROM service_requests ORDER BY created_at DESC",
                        null
                    )
                    val list = mutableListOf<ServiceRequest>()
                    while (cursor.moveToNext()) {
                        list.add(
                            ServiceRequest(
                                id = cursor.getInt(0),
                                proId = cursor.getInt(1),
                                customerName = cursor.getString(2) ?: "",
                                customerPhone = cursor.getString(3),
                                projectDescription = cursor.getString(4) ?: "",
                                appointmentTime = cursor.getString(5) ?: "",
                                status = cursor.getString(6) ?: "confirmed",
                                createdAt = cursor.getString(7) ?: ""
                            )
                        )
                    }
                    cursor.close()
                    requests = list
                } finally {
                    db.close()
                }
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
                text = "My Requests",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (requests.isEmpty()) {
            Text(
                text = "No service requests yet.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(requests) { request ->
                    val pro = pros.find { it.id == request.proId }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Service request: ${pro?.name ?: "Unknown Pro"}, ${request.appointmentTime}"
                            }
                            .testTag("request_card_${request.id}"),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = pro?.name ?: "Pro #${request.proId}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (pro != null) {
                                Text(
                                    text = pro.serviceType,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "Scheduled: ${request.appointmentTime}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Status: ${request.status}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val dbPath = context.getDatabasePath("mthumbtack.db")
                                        val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
                                        db.execSQL("DELETE FROM service_requests WHERE id = ?", arrayOf(request.id.toString()))
                                        db.close()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier
                                    .semantics { contentDescription = "Cancel request button" }
                                    .testTag("cancel_request_${request.id}")
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}
