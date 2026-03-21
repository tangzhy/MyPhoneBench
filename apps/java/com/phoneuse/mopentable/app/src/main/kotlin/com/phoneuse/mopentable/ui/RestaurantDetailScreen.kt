package com.phoneuse.mopentable.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phoneuse.mopentable.data.Restaurant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
    restaurant: Restaurant,
    onSelectSlot: (String, Int) -> Unit,
    onBack: () -> Unit
) {
    var selectedSlot by remember { mutableStateOf<String?>(null) }
    var partySize by remember { mutableStateOf(2) }
    val priceStr = "$".repeat(restaurant.priceLevel)

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
                text = restaurant.name,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = restaurant.cuisine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "★ ${restaurant.rating}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = priceStr,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Hours: ${restaurant.hours}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "${restaurant.neighborhood}, ${restaurant.city}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Available Time Slots",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                restaurant.availableSlots.forEach { slot ->
                    val isSelected = selectedSlot == slot
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSlot = slot },
                        label = { Text(slot) },
                        modifier = Modifier
                            .semantics { contentDescription = "Time slot: $slot" }
                            .testTag("time_slot_$slot")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Party Size",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = partySize.toString(),
                onValueChange = { v ->
                    v.toIntOrNull()?.takeIf { it in 1..10 }?.let { partySize = it }
                },
                label = { Text("Number of guests (1-10)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Party size input" }
                    .testTag("party_size_input"),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val slot = selectedSlot
                if (slot != null) onSelectSlot(slot, partySize)
            },
            enabled = selectedSlot != null,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Make Reservation button" }
                .testTag("make_reservation_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Make Reservation", style = MaterialTheme.typography.titleMedium)
        }
    }
}
