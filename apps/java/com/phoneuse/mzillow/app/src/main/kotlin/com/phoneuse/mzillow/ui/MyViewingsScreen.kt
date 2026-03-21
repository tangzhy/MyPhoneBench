package com.phoneuse.mzillow.ui

import android.database.sqlite.SQLiteDatabase
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
import java.io.File

data class ViewingRow(
    val id: Int,
    val propertyId: Int,
    val visitorName: String,
    val viewingTime: String,
    val viewingPurpose: String,
    val status: String,
)

@Composable
fun MyViewingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var viewings by remember { mutableStateOf<List<ViewingRow>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        viewings = try {
            val dbPath = context.getDatabasePath("mzillow.db")
            if (!dbPath.exists()) {
                emptyList()
            } else {
                val db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)
                val rows = mutableListOf<ViewingRow>()
                try {
                    val cursor = db.rawQuery(
                        "SELECT id, property_id, visitor_name, viewing_time, " +
                            "viewing_purpose, status FROM viewing_appointments " +
                            "ORDER BY created_at DESC",
                        null,
                    )
                    while (cursor.moveToNext()) {
                        rows.add(
                            ViewingRow(
                                id = cursor.getInt(0),
                                propertyId = cursor.getInt(1),
                                visitorName = cursor.getString(2) ?: "",
                                viewingTime = cursor.getString(3) ?: "",
                                viewingPurpose = cursor.getString(4) ?: "",
                                status = cursor.getString(5) ?: "confirmed",
                            )
                        )
                    }
                    cursor.close()
                } catch (_: Exception) { }
                db.close()
                rows
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

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
                text = "My Viewings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (viewings.isEmpty()) {
            Text(
                text = "No viewings scheduled yet.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 32.dp),
            )
        } else {
            LazyColumn {
                items(viewings, key = { it.id }) { v ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .semantics {
                                contentDescription = "Viewing #${v.id}"
                            }
                            .testTag("viewing_card_${v.id}"),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Property #${v.propertyId}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = v.status.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (v.status == "cancelled")
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = v.viewingTime,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = v.viewingPurpose,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (v.status == "confirmed") {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val dbPath = context.getDatabasePath("mzillow.db")
                                            val db = SQLiteDatabase.openOrCreateDatabase(
                                                dbPath, null
                                            )
                                            db.execSQL(
                                                "UPDATE viewing_appointments " +
                                                    "SET status = 'cancelled' WHERE id = ?",
                                                arrayOf(v.id.toString()),
                                            )
                                            db.close()
                                            refreshKey++
                                        } catch (_: Exception) { }
                                    },
                                    modifier = Modifier
                                        .semantics {
                                            contentDescription = "Cancel viewing ${v.id}"
                                        }
                                        .testTag("cancel_viewing_${v.id}"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text("Cancel Viewing")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
