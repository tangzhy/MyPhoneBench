package com.phoneuse.mzillow.ui

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
import com.phoneuse.mzillow.data.Property
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    property: Property,
    onSelectSlot: (String) -> Unit,
    onBack: () -> Unit,
) {
    val fmt = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }
    var selectedSlot by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back button" },
            ) {
                Text("←")
            }
            Text(
                text = "Property Details",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = fmt.format(property.price),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = property.address,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${property.neighborhood}, ${property.city}, ${property.state}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Text(
                            text = "${property.bedrooms} Beds",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "${property.bathrooms} Baths",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "${NumberFormat.getNumberInstance().format(property.sqft)} sqft",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Type: ${property.propertyType}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Listed by: ${property.listingAgent}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (property.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = property.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Available Viewing Times",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            property.availableViewingSlots.forEach { slot ->
                FilterChip(
                    selected = selectedSlot == slot,
                    onClick = { selectedSlot = slot },
                    label = { Text(slot) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .semantics { contentDescription = "Time slot: $slot" }
                        .testTag("time_slot_$slot"),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { selectedSlot?.let { onSelectSlot(it) } },
            enabled = selectedSlot != null,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Schedule Viewing button" }
                .testTag("schedule_viewing_button"),
        ) {
            Text("Schedule Viewing")
        }
    }
}
