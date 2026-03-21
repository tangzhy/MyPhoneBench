package com.phoneuse.mzillow.ui

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
import com.phoneuse.mzillow.data.Property
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    properties: List<Property>,
    onPropertyClick: (Property) -> Unit,
    onMyViewingsClick: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedNeighborhood by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<String?>(null) }

    val neighborhoods = remember(properties) {
        properties.map { it.neighborhood }.distinct().sorted()
    }
    val propertyTypes = remember(properties) {
        properties.map { it.propertyType }.distinct().sorted()
    }

    val filtered = remember(properties, searchQuery, selectedNeighborhood, selectedType) {
        properties.filter { p ->
            val q = searchQuery.lowercase()
            val matchesQuery = q.isEmpty() ||
                p.address.lowercase().contains(q) ||
                p.neighborhood.lowercase().contains(q) ||
                p.propertyType.lowercase().contains(q) ||
                p.listingAgent.lowercase().contains(q) ||
                p.description.lowercase().contains(q)
            val matchesNeighborhood =
                selectedNeighborhood == null || p.neighborhood == selectedNeighborhood
            val matchesType =
                selectedType == null || p.propertyType == selectedType
            matchesQuery && matchesNeighborhood && matchesType
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "mZillow",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onMyViewingsClick,
                modifier = Modifier
                    .semantics { contentDescription = "My Viewings button" }
                    .testTag("my_viewings_button"),
            ) {
                Text("My Viewings")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search properties...") },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Search properties input" }
                    .testTag("search_input"),
                singleLine = true,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { },
                modifier = Modifier
                    .semantics { contentDescription = "Search button" }
                    .testTag("search_button"),
            ) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Neighborhood", style = MaterialTheme.typography.labelMedium)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(
                selected = selectedNeighborhood == null,
                onClick = { selectedNeighborhood = null },
                label = { Text("All") },
                modifier = Modifier
                    .padding(end = 4.dp)
                    .semantics { contentDescription = "Neighborhood filter: All" }
                    .testTag("neighborhood_all"),
            )
            neighborhoods.forEach { n ->
                FilterChip(
                    selected = selectedNeighborhood == n,
                    onClick = {
                        selectedNeighborhood = if (selectedNeighborhood == n) null else n
                    },
                    label = { Text(n) },
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .semantics { contentDescription = "Neighborhood filter: $n" }
                        .testTag("neighborhood_$n"),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text("Property Type", style = MaterialTheme.typography.labelMedium)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(
                selected = selectedType == null,
                onClick = { selectedType = null },
                label = { Text("All") },
                modifier = Modifier
                    .padding(end = 4.dp)
                    .semantics { contentDescription = "Type filter: All" }
                    .testTag("type_all"),
            )
            propertyTypes.forEach { t ->
                FilterChip(
                    selected = selectedType == t,
                    onClick = { selectedType = if (selectedType == t) null else t },
                    label = { Text(t) },
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .semantics { contentDescription = "Type filter: $t" }
                        .testTag("type_$t"),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "${filtered.size} properties found",
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered, key = { it.id }) { property ->
                PropertyCard(property = property, onClick = { onPropertyClick(property) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyCard(property: Property, onClick: () -> Unit) {
    val fmt = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics { contentDescription = "Property: ${property.address}" }
            .testTag("property_card_${property.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = fmt.format(property.price),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = property.address,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${property.neighborhood} · ${property.propertyType}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "${property.bedrooms} bd · ${property.bathrooms} ba · " +
                    "${NumberFormat.getNumberInstance().format(property.sqft)} sqft",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Agent: ${property.listingAgent}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
