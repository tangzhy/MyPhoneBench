package com.phoneuse.mdmv.ui

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
import com.phoneuse.mdmv.data.DmvOffice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    offices: List<DmvOffice>,
    onOfficeClick: (DmvOffice) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val cities = remember(offices) { offices.map { it.city }.distinct().sorted() }
    val allServices = remember(offices) {
        offices.flatMap { it.servicesOffered }.distinct().sorted()
    }
    var selectedCity by remember { mutableStateOf<String?>(null) }
    var selectedService by remember { mutableStateOf<String?>(null) }

    val filteredOffices = remember(offices, searchQuery, selectedCity, selectedService) {
        offices.filter { office ->
            val matchesSearch = searchQuery.isEmpty() ||
                office.name.contains(searchQuery, ignoreCase = true) ||
                office.city.contains(searchQuery, ignoreCase = true) ||
                office.address.contains(searchQuery, ignoreCase = true)
            val matchesCity = selectedCity == null || office.city == selectedCity
            val matchesService = selectedService == null ||
                office.servicesOffered.contains(selectedService)
            matchesSearch && matchesCity && matchesService
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "DMV Offices",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Search bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name, city, or address") },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Search input field" }
                    .testTag("search_input"),
                singleLine = true
            )
            Button(
                onClick = { /* search is live */ },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .semantics { contentDescription = "Search button" }
                    .testTag("search_button")
            ) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // City filter chips
        Text(
            text = "Filter by City",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCity == null,
                onClick = { selectedCity = null },
                label = { Text("All") },
                modifier = Modifier
                    .semantics { contentDescription = "City filter: All" }
                    .testTag("city_all")
            )
            cities.forEach { city ->
                FilterChip(
                    selected = selectedCity == city,
                    onClick = { selectedCity = if (selectedCity == city) null else city },
                    label = { Text(city) },
                    modifier = Modifier
                        .semantics { contentDescription = "City filter: $city" }
                        .testTag("city_$city")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Service filter chips
        Text(
            text = "Filter by Service",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedService == null,
                onClick = { selectedService = null },
                label = { Text("All") },
                modifier = Modifier
                    .semantics { contentDescription = "Service filter: All" }
                    .testTag("service_all")
            )
            allServices.forEach { service ->
                FilterChip(
                    selected = selectedService == service,
                    onClick = { selectedService = if (selectedService == service) null else service },
                    label = { Text(service) },
                    modifier = Modifier
                        .semantics { contentDescription = "Service filter: $service" }
                        .testTag("service_$service")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Available Offices (${filteredOffices.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredOffices) { office ->
                OfficeCard(
                    office = office,
                    onClick = { onOfficeClick(office) }
                )
            }
        }
    }
}

@Composable
fun OfficeCard(
    office: DmvOffice,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "DMV Office card: ${office.name}, ${office.city}" }
            .testTag("office_card_${office.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = office.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${office.address}, ${office.city}, ${office.state} ${office.zipCode}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = office.hours,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Wait: ~${office.waitTimeMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Services: ${office.servicesOffered.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
