package com.phoneuse.mopentable.ui

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
import com.phoneuse.mopentable.data.Restaurant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    restaurants: List<Restaurant>,
    onSearch: (String, String?, Int) -> Unit,
    onRestaurantClick: (Restaurant) -> Unit,
    onMyReservationsClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPartySize by remember { mutableStateOf(2) }
    val cuisines = remember(restaurants) {
        restaurants.map { it.cuisine }.distinct().sorted()
    }
    val popularRestaurants = restaurants.take(3)
    val commonPartySizes = listOf(1, 2, 4, 6, 8, 10)

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
                text = "mOpenTable",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onMyReservationsClick,
                modifier = Modifier.semantics {
                    contentDescription = "My Reservations button"
                }
                .testTag("my_reservations_button")
            ) {
                Text("My Reservations")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search restaurants") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Search input field" }
                .testTag("search_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Cuisine",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cuisines.forEach { cuisine ->
                FilterChip(
                    selected = false,
                    onClick = { onSearch(searchQuery, cuisine, selectedPartySize) },
                    label = { Text(cuisine) },
                    modifier = Modifier
                        .semantics { contentDescription = "Cuisine filter: $cuisine" }
                        .testTag("cuisine_$cuisine")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Party Size",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commonPartySizes.forEach { size ->
                val isSelected = selectedPartySize == size
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedPartySize = size },
                    label = { Text("$size") },
                    modifier = Modifier
                        .semantics { contentDescription = "Party size: $size" }
                        .testTag("party_size_$size")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = selectedPartySize.toString(),
            onValueChange = { v ->
                v.toIntOrNull()?.takeIf { it in 1..10 }?.let { selectedPartySize = it }
            },
            label = { Text("Or enter 1-10") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Party size input" }
                .testTag("party_size_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onSearch(searchQuery, null, selectedPartySize) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Browse all button" }
                    .testTag("browse_all_button")
            ) {
                Text("Browse All")
            }
            Button(
                onClick = { onSearch(searchQuery, null, selectedPartySize) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Search button" }
                    .testTag("search_button")
            ) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Popular Restaurants",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(popularRestaurants) { restaurant ->
                RestaurantCard(
                    restaurant = restaurant,
                    onClick = { onRestaurantClick(restaurant) }
                )
            }
        }
    }
}

@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    onClick: () -> Unit
) {
    val priceStr = "$".repeat(restaurant.priceLevel)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Restaurant: ${restaurant.name}, ${restaurant.cuisine}"
            }
            .testTag("restaurant_card_${restaurant.id}"),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                Text(
                    text = restaurant.neighborhood,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
