package com.phoneuse.mbooking.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phoneuse.mbooking.data.Hotel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    hotels: List<Hotel>,
    onSearch: (String, String?) -> Unit,
    onHotelClick: (Hotel) -> Unit,
    onMyReservationsClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val neighborhoods = remember(hotels) { hotels.map { it.neighborhood }.distinct().sorted() }
    val popularHotels = hotels.take(3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Find a Hotel",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onMyReservationsClick,
                modifier = Modifier.semantics { contentDescription = "My Reservations button" }
            ) {
                Text("My Reservations")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Search bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name, neighborhood, or amenity") },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Search input field" }
                    .testTag("search_input"),
                singleLine = true
            )
            Button(
                onClick = { onSearch(searchQuery, null) },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .semantics { contentDescription = "Search button" }
                    .testTag("search_button")
            ) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Neighborhood shortcuts
        Text(
            text = "Neighborhoods",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            neighborhoods.forEach { neighborhood ->
                FilterChip(
                    selected = false,
                    onClick = { onSearch("", neighborhood) },
                    label = { Text(neighborhood) },
                    modifier = Modifier
                        .semantics { contentDescription = "Neighborhood filter: $neighborhood" }
                        .testTag("neighborhood_$neighborhood")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Popular hotels
        Text(
            text = "Popular Hotels",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(popularHotels) { hotel ->
                HotelCard(
                    hotel = hotel,
                    onClick = { onHotelClick(hotel) }
                )
            }
        }
    }
}

@Composable
fun HotelCard(
    hotel: Hotel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Hotel card: ${hotel.name}, ${hotel.neighborhood}" }
            .testTag("hotel_card_${hotel.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = hotel.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = hotel.neighborhood,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${hotel.pricePerNight}/night",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "★".repeat(hotel.starRating),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
