package com.phoneuse.mthumbtack.ui

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
import com.phoneuse.mthumbtack.data.ProDemo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    pros: List<ProDemo>,
    onSearch: (String, String?) -> Unit,
    onProClick: (ProDemo) -> Unit,
    onMyRequestsClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val serviceTypes = remember(pros) { pros.map { it.serviceType }.distinct().sorted() }
    val topPros = pros.take(3)

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
                text = "Find a Pro",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onMyRequestsClick,
                modifier = Modifier.semantics { contentDescription = "My Requests button" }
            ) {
                Text("My Requests")
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
                label = { Text("Search by name, service, or city") },
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
            text = "Service Categories",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            serviceTypes.forEach { serviceType ->
                FilterChip(
                    selected = false,
                    onClick = { onSearch("", serviceType) },
                    label = { Text(serviceType) },
                    modifier = Modifier
                        .semantics { contentDescription = "Service filter: $serviceType" }
                        .testTag("service_type_$serviceType")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Top Rated Pros",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(topPros) { pro ->
                ProCard(pro = pro, onClick = { onProClick(pro) })
            }
        }
    }
}

@Composable
fun ProCard(
    pro: ProDemo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Pro card: ${pro.name}, ${pro.serviceType}" }
            .testTag("pro_card_${pro.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = pro.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = pro.serviceType,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${pro.company} - ${pro.city}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "★ ${pro.rating}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
