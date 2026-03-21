package com.phoneuse.mopentable.ui

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
import com.phoneuse.mopentable.data.Restaurant

data class ReservationSummary(
    val id: Int,
    val restaurantId: Int,
    val restaurantName: String,
    val reservationTime: String,
    val partySize: String,
    val status: String
)

@Composable
fun MyReservationsScreen(
    restaurants: List<Restaurant>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var refreshCount by remember { mutableStateOf(0) }
    val restaurantMap = remember(restaurants) {
        restaurants.associateBy { it.id }
    }
    val reservations = remember(refreshCount) {
        try {
            val dbPath = context.getDatabasePath("mopentable.db")
            if (!dbPath.exists()) return@remember emptyList<ReservationSummary>()
            val db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)
            val list = mutableListOf<ReservationSummary>()
            val cursor = db.rawQuery(
                "SELECT id, restaurant_id, reservation_time, party_size, status FROM reservations WHERE status != 'cancelled' ORDER BY created_at DESC",
                null
            )
            while (cursor.moveToNext()) {
                val id = cursor.getInt(0)
                val restId = cursor.getInt(1)
                val time = cursor.getString(2) ?: ""
                val party = cursor.getString(3) ?: ""
                val status = cursor.getString(4) ?: "confirmed"
                val restName = restaurantMap[restId]?.name ?: "Restaurant #$restId"
                list.add(
                    ReservationSummary(
                        id = id,
                        restaurantId = restId,
                        restaurantName = restName,
                        reservationTime = time,
                        partySize = party,
                        status = status
                    )
                )
            }
            cursor.close()
            db.close()
            list
        } catch (e: Exception) {
            emptyList<ReservationSummary>()
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
                    .testTag("back_button")
            ) {
                Text("\u2190")
            }
            Text(
                text = "My Reservations",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (reservations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No reservations yet")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reservations) { r ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Reservation: ${r.restaurantName} at ${r.reservationTime}"
                            }
                            .testTag("reservation_card_${r.id}"),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = r.restaurantName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = r.reservationTime,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Party of ${r.partySize}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = r.status.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val dbPath = context.getDatabasePath("mopentable.db")
                                            val db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)
                                            db.execSQL(
                                                "UPDATE reservations SET status = 'cancelled' WHERE id = ?",
                                                arrayOf(r.id.toString())
                                            )
                                            db.close()
                                            refreshCount++
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    modifier = Modifier
                                        .semantics { contentDescription = "Cancel reservation button" }
                                        .testTag("cancel_button_${r.id}")
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
}
