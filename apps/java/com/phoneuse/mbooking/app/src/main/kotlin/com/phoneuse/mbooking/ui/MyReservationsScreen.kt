package com.phoneuse.mbooking.ui

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
import com.phoneuse.mbooking.data.Hotel
import com.phoneuse.mbooking.data.DataLoader
import android.database.sqlite.SQLiteDatabase
import java.io.File

data class ReservationItem(
    val id: Int,
    val hotelId: Int,
    val hotelName: String,
    val neighborhood: String,
    val checkInDate: String,
    val checkOutDate: String,
    val roomType: String,
    val guestName: String
)

@Composable
fun MyReservationsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var reservations by remember { mutableStateOf<List<ReservationItem>>(emptyList()) }
    var hotels by remember { mutableStateOf<List<Hotel>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            hotels = DataLoader.loadAllFromJson(context)
        } catch (e: Exception) {
            hotels = DataLoader.getDefaultHotels()
        }
    }

    LaunchedEffect(hotels) {
        try {
            val dbDir = context.getDatabasePath("mbooking.db").parent
            val dbPath = if (dbDir != null) {
                File(dbDir, "mbooking.db")
            } else {
                File("/data/data/com.phoneuse.mbooking/databases/mbooking.db")
            }
            if (dbPath.exists()) {
                val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                val cursor = db.rawQuery(
                    "SELECT id, hotel_id, guest_name, check_in_date, check_out_date, room_type FROM reservations ORDER BY check_in_date ASC",
                    null
                )

                val reservationList = mutableListOf<ReservationItem>()
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0)
                    val hotelId = cursor.getInt(1)
                    val guestName = cursor.getString(2)
                    val checkInDate = cursor.getString(3)
                    val checkOutDate = cursor.getString(4) ?: ""
                    val roomType = cursor.getString(5) ?: ""

                    val hotel = hotels.find { it.id == hotelId }
                    if (hotel != null) {
                        reservationList.add(
                            ReservationItem(
                                id = id,
                                hotelId = hotelId,
                                hotelName = hotel.name,
                                neighborhood = hotel.neighborhood,
                                checkInDate = checkInDate,
                                checkOutDate = checkOutDate,
                                roomType = roomType,
                                guestName = guestName
                            )
                        )
                    }
                }
                cursor.close()
                db.close()
                reservations = reservationList
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
                text = "My Reservations",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (reservations.isEmpty()) {
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
                        text = "No reservations yet",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Book a hotel to see it here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reservations) { reservation ->
                    ReservationCard(
                        reservation = reservation,
                        onCancel = {
                            try {
                                val dbDir = context.getDatabasePath("mbooking.db").parent
                                val dbPath = if (dbDir != null) {
                                    File(dbDir, "mbooking.db")
                                } else {
                                    File("/data/data/com.phoneuse.mbooking/databases/mbooking.db")
                                }
                                val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
                                db.execSQL("DELETE FROM reservations WHERE id = ?", arrayOf(reservation.id.toString()))
                                db.close()
                                reservations = reservations.filter { it.id != reservation.id }
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
fun ReservationCard(
    reservation: ReservationItem,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Reservation card: ${reservation.hotelName}, ${reservation.checkInDate}" }
            .testTag("reservation_card_${reservation.id}"),
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
                        text = reservation.hotelName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reservation.neighborhood,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (reservation.roomType.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reservation.roomType,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .semantics { contentDescription = "Cancel reservation button" }
                        .testTag("cancel_reservation_${reservation.id}")
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
                        text = "Check-in",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = reservation.checkInDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "Check-out",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = reservation.checkOutDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
