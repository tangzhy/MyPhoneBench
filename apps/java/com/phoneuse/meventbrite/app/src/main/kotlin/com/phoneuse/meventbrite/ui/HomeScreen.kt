// Copyright 2025 PhoneUse Privacy Protocol Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.phoneuse.meventbrite.ui

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
import com.phoneuse.meventbrite.data.EventDemo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    events: List<EventDemo>,
    onSearch: (String, String?) -> Unit,
    onEventClick: (EventDemo) -> Unit,
    onMyRegistrationsClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val categories = remember(events) { events.map { it.category }.distinct().sorted() }
    val popularEvents = events.take(3)

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
                text = "Find Events",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onMyRegistrationsClick,
                modifier = Modifier.semantics { contentDescription = "My Registrations button" }
            ) {
                Text("My Registrations")
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
                label = { Text("Search events by name, category, or city") },
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

        // Category filter chips
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = false,
                    onClick = { onSearch("", category) },
                    label = { Text(category) },
                    modifier = Modifier
                        .semantics { contentDescription = "Category filter: $category" }
                        .testTag("category_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Popular events
        Text(
            text = "Popular Events",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(popularEvents) { event ->
                EventCard(
                    event = event,
                    onClick = { onEventClick(event) }
                )
            }
        }
    }
}

@Composable
fun EventCard(
    event: EventDemo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Event card: ${event.title}, ${event.category}" }
            .testTag("event_card_${event.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = event.category,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${event.venue} \u2022 ${event.date}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (event.price == 0.0) "Free" else "$${String.format("%.2f", event.price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
