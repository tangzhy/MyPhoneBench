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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.phoneuse.meventbrite.data.EventDemo

@Composable
fun EventListScreen(
    events: List<EventDemo>,
    query: String,
    category: String?,
    onEventClick: (EventDemo) -> Unit,
    onBack: () -> Unit
) {
    val filteredEvents = remember(events, query, category) {
        events.filter { event ->
            val matchesQuery = query.isEmpty() ||
                event.title.contains(query, ignoreCase = true) ||
                event.category.contains(query, ignoreCase = true) ||
                event.city.contains(query, ignoreCase = true) ||
                event.venue.contains(query, ignoreCase = true) ||
                event.organizer.contains(query, ignoreCase = true)
            val matchesCategory = category == null || event.category == category
            matchesQuery && matchesCategory
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
                text = if (category != null) category else "Search Results",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No events found")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredEvents) { event ->
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event) }
                    )
                }
            }
        }
    }
}
