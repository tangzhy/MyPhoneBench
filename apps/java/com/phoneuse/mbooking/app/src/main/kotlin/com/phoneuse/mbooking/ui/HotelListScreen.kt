package com.phoneuse.mbooking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.phoneuse.mbooking.data.Hotel

@Composable
fun HotelListScreen(
    hotels: List<Hotel>,
    query: String,
    neighborhood: String?,
    onHotelClick: (Hotel) -> Unit,
    onBack: () -> Unit
) {
    val filteredHotels = remember(hotels, query, neighborhood) {
        hotels.filter { hotel ->
            val matchesQuery = query.isEmpty() ||
                hotel.name.contains(query, ignoreCase = true) ||
                hotel.neighborhood.contains(query, ignoreCase = true) ||
                hotel.city.contains(query, ignoreCase = true) ||
                hotel.amenities.any { it.contains(query, ignoreCase = true) }
            val matchesNeighborhood = neighborhood == null || hotel.neighborhood == neighborhood
            matchesQuery && matchesNeighborhood
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
                text = if (neighborhood != null) neighborhood else "Search Results",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredHotels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hotels found")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredHotels) { hotel ->
                    HotelCard(
                        hotel = hotel,
                        onClick = { onHotelClick(hotel) }
                    )
                }
            }
        }
    }
}
