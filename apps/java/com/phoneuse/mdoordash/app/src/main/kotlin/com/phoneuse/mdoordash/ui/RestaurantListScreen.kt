package com.phoneuse.mdoordash.ui

import androidx.compose.foundation.clickable
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
import com.phoneuse.mdoordash.data.RestaurantDemo

@Composable
fun RestaurantListScreen(
    restaurants: List<RestaurantDemo>,
    query: String,
    cuisine: String?,
    onRestaurantClick: (RestaurantDemo) -> Unit,
    onBack: () -> Unit
) {
    val filteredRestaurants = remember(restaurants, query, cuisine) {
        restaurants.filter { restaurant ->
            val matchesQuery = query.isEmpty() ||
                restaurant.name.contains(query, ignoreCase = true) ||
                restaurant.cuisine.contains(query, ignoreCase = true) ||
                restaurant.neighborhood.contains(query, ignoreCase = true) ||
                restaurant.city.contains(query, ignoreCase = true)
            val matchesCuisine = cuisine == null || restaurant.cuisine == cuisine
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
            ) {
                Text("\u2190")
            }
            Text(
                text = if (cuisine != null) cuisine else "Search Results",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredRestaurants.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No restaurants found")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRestaurants) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        onClick = { onRestaurantClick(restaurant) }
                    )
                }
            }
        }
    }
}
