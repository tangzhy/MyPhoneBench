package com.phoneuse.mdoordash.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.phoneuse.mdoordash.data.RestaurantDemo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
    restaurant: RestaurantDemo,
    onPlaceOrder: (String, List<String>) -> Unit,
    onBack: () -> Unit
) {
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }
    var selectedItems by remember { mutableStateOf<Set<String>>(emptySet()) }

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
                text = "Restaurant Details",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Restaurant info
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = restaurant.cuisine,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${restaurant.neighborhood}, ${restaurant.city}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "\u2605 ${restaurant.rating}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${restaurant.deliveryTimeMin} min delivery",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Menu items
        Text(
            text = "Menu",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            restaurant.menuItems.forEach { menuItem ->
                val isSelected = selectedItems.contains(menuItem.name)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            selectedItems = if (checked) {
                                selectedItems + menuItem.name
                            } else {
                                selectedItems - menuItem.name
                            }
                        },
                        modifier = Modifier
                            .semantics { contentDescription = "Select ${menuItem.name}" }
                            .testTag("menu_item_${menuItem.name}")
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = menuItem.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Text(
                        text = "$${String.format("%.2f", menuItem.price)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Delivery time slots
            Text(
                text = "Delivery Time",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(restaurant.availableSlots) { slot ->
                    val isSelected = selectedTimeSlot == slot
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTimeSlot = slot },
                        label = { Text(slot) },
                        modifier = Modifier
                            .semantics { contentDescription = "Delivery time slot: $slot" }
                            .testTag("time_slot_$slot")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Place Order button
        Button(
            onClick = {
                selectedTimeSlot?.let { onPlaceOrder(it, selectedItems.toList()) }
            },
            enabled = selectedTimeSlot != null && selectedItems.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Place Order button" }
                .testTag("place_order_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Place Order",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
