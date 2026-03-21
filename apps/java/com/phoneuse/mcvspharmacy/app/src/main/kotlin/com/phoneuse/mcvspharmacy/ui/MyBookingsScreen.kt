package com.phoneuse.mcvspharmacy.ui

import android.database.sqlite.SQLiteDatabase
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

data class BookingSummary(
    val id: Int,
    val serviceName: String,
    val storeName: String,
    val bookingTime: String,
    val status: String
)

@Composable
fun MyBookingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val bookings = remember {
        try {
            val dbPath = context.getDatabasePath("mcvspharmacy.db")
            if (!dbPath.exists()) return@remember emptyList<BookingSummary>()
            val db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)
            val list = mutableListOf<BookingSummary>()
            val cursor = db.rawQuery(
                "SELECT id, service_id, store_id, booking_time, status FROM bookings ORDER BY created_at DESC",
                null
            )
            while (cursor.moveToNext()) {
                list.add(
                    BookingSummary(
                        id = cursor.getInt(0),
                        serviceName = "Service #${cursor.getInt(1)}",
                        storeName = "Store #${cursor.getInt(2)}",
                        bookingTime = cursor.getString(3) ?: "",
                        status = cursor.getString(4) ?: "confirmed"
                    )
                )
            }
            cursor.close()
            db.close()
            list
        } catch (e: Exception) {
            emptyList<BookingSummary>()
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
                text = "My Bookings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (bookings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No bookings yet")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bookings.size) { idx ->
                    val b = bookings[idx]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Booking: ${b.serviceName} at ${b.storeName}"
                            }
                            .testTag("booking_card_${b.id}"),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = b.serviceName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = b.storeName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = b.bookingTime,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = b.status.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
