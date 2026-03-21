package com.phoneuse.mcvspharmacy.ui

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
import com.phoneuse.mcvspharmacy.data.CareService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    careServices: List<CareService>,
    onSearch: (String, String?) -> Unit,
    onServiceClick: (CareService) -> Unit,
    onMyBookingsClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val serviceTypes = remember(careServices) {
        careServices.map { it.type }.distinct().sorted()
    }
    val popularServices = careServices.filter { it.availableOnlineBooking }.take(3)

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
                text = "MinuteClinic Services",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onMyBookingsClick,
                modifier = Modifier.semantics {
                    contentDescription = "My Bookings button"
                }
            ) {
                Text("My Bookings")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search services by name or type") },
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

        Text(
            text = "Service Types",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            serviceTypes.forEach { sType ->
                FilterChip(
                    selected = false,
                    onClick = { onSearch("", sType) },
                    label = { Text(sType.replace("_", " ").replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier
                        .semantics { contentDescription = "Service type filter: $sType" }
                        .testTag("service_type_$sType")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Popular Services",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(popularServices) { service ->
                ServiceCard(service = service, onClick = { onServiceClick(service) })
            }
        }
    }
}

@Composable
fun ServiceCard(service: CareService, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Service card: ${service.name}, ${service.type}"
            }
            .testTag("service_card_${service.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = service.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = service.type.replace("_", " ").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${service.durationMin} min",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (service.basePrice == 0.0) "Free" else "$${service.basePrice}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
