package com.phoneuse.mthumbtack.ui

import androidx.compose.foundation.layout.*
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
import com.phoneuse.mthumbtack.data.ProDemo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProDetailScreen(
    pro: ProDemo,
    onRequestService: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedSlot by remember { mutableStateOf<String?>(null) }

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
                text = "Pro Details",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Pro info card: ${pro.name}" },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = pro.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = pro.serviceType,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Company: ${pro.company}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "City: ${pro.city}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Rating: ★ ${pro.rating}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Available Time Slots",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(pro.availableSlots) { slot ->
                FilterChip(
                    selected = selectedSlot == slot,
                    onClick = { selectedSlot = slot },
                    label = { Text(slot, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .semantics { contentDescription = "Time slot: $slot" }
                        .testTag("time_slot_$slot")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { selectedSlot?.let { onRequestService(it) } },
            enabled = selectedSlot != null,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Request Service button" }
                .testTag("request_service_button")
        ) {
            Text("Request Service", style = MaterialTheme.typography.titleMedium)
        }
    }
}
