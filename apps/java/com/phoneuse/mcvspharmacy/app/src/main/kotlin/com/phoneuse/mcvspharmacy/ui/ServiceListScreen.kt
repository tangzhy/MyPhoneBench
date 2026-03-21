package com.phoneuse.mcvspharmacy.ui

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
import com.phoneuse.mcvspharmacy.data.CareService

@Composable
fun ServiceListScreen(
    careServices: List<CareService>,
    query: String,
    serviceType: String?,
    onServiceClick: (CareService) -> Unit,
    onBack: () -> Unit
) {
    val filtered = remember(careServices, query, serviceType) {
        careServices.filter { svc ->
            val matchesQuery = query.isEmpty() ||
                svc.name.contains(query, ignoreCase = true) ||
                svc.type.contains(query, ignoreCase = true)
            val matchesType = serviceType == null || svc.type == serviceType
            matchesQuery && matchesType
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
                text = serviceType?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                    ?: "Search Results",
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
                Text("No services found")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { service ->
                    ServiceCard(service = service, onClick = { onServiceClick(service) })
                }
            }
        }
    }
}
