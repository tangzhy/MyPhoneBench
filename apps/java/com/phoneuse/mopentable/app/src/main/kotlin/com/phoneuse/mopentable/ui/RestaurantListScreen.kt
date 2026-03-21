package com.phoneuse.mopentable.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phoneuse.mopentable.data.Restaurant

@Composable
fun RestaurantListScreen(
    restaurants: List<Restaurant>,
    query: String,
    cuisine: String?,
    partySize: Int,
    onRestaurantClick: (Restaurant) -> Unit,
    onBack: () -> Unit
) {
    val filtered = remember(restaurants, query, cuisine, partySize) {
        restaurants.filter { r ->
            val matchesQuery = query.isEmpty() ||
                r.name.contains(query, ignoreCase = true) ||
                r.cuisine.contains(query, ignoreCase = true) ||
                r.neighborhood.contains(query, ignoreCase = true)
            val matchesCuisine = cuisine == null || r.cuisine == cuisine
            matchesQuery && matchesCuisine
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
                text = cuisine?.let { "Restaurants - $it" } ?: "Search Results",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No restaurants found")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        onClick = { onRestaurantClick(restaurant) }
                    )
                }
            }
        }
    }
}
