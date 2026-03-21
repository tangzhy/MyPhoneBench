package com.phoneuse.mcvspharmacy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phoneuse.mcvspharmacy.data.CareService
import com.phoneuse.mcvspharmacy.data.Store
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSelectionScreen(
    service: CareService,
    stores: List<Store>,
    onSelectSlot: (Store, String) -> Unit,
    onBack: () -> Unit
) {
    val eligibleStores = remember(stores, service) {
        stores.filter { store ->
            store.services.contains("minuteclinic") || store.services.contains("vaccination")
        }.sortedBy { it.distanceMiles }
    }

    var selectedStore by remember { mutableStateOf<Store?>(null) }
    var selectedSlot by remember { mutableStateOf<String?>(null) }

    val slotsForStore = remember(selectedStore) {
        selectedStore?.let { generateSlots(it.id) } ?: emptyList()
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
                text = service.name,
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
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${service.durationMin} min | ${
                        if (service.basePrice == 0.0) "Free" else "$${service.basePrice}"
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select a Store",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(eligibleStores) { store ->
                val isSelected = selectedStore?.id == store.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedStore = store
                            selectedSlot = null
                        }
                        .semantics {
                            contentDescription = "Store: ${store.name}, ${store.city}"
                        }
                        .testTag("store_card_${store.id}"),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) 6.dp else 2.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = store.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = store.address,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = store.openHours,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${store.distanceMiles} mi",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (isSelected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Available Time Slots",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(180.dp)
                    ) {
                        items(slotsForStore) { slot ->
                            val slotSelected = selectedSlot == slot
                            FilterChip(
                                selected = slotSelected,
                                onClick = { selectedSlot = slot },
                                label = { Text(slot) },
                                modifier = Modifier
                                    .semantics {
                                        contentDescription = "Time slot: $slot"
                                    }
                                    .testTag("time_slot_$slot")
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val st = selectedStore
                val sl = selectedSlot
                if (st != null && sl != null) onSelectSlot(st, sl)
            },
            enabled = selectedStore != null && selectedSlot != null,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Continue to Booking button" }
                .testTag("continue_booking_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Continue to Booking", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun generateSlots(storeId: Int): List<String> {
    val rng = Random(storeId.toLong() * 31)
    val cal = Calendar.getInstance().apply {
        set(2026, Calendar.MARCH, 10, 0, 0, 0)
    }
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val slots = mutableListOf<String>()
    for (day in 0..2) {
        cal.set(Calendar.DAY_OF_MONTH, 10 + day)
        val hours = (8..17).shuffled(rng).take(3 + rng.nextInt(3)).sorted()
        for (h in hours) {
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, if (rng.nextBoolean()) 0 else 30)
            slots.add(sdf.format(cal.time))
        }
    }
    return slots
}
