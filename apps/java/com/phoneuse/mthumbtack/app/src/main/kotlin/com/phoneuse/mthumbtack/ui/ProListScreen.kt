package com.phoneuse.mthumbtack.ui

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
import com.phoneuse.mthumbtack.data.ProDemo

@Composable
fun ProListScreen(
    pros: List<ProDemo>,
    query: String,
    serviceType: String?,
    onProClick: (ProDemo) -> Unit,
    onBack: () -> Unit
) {
    val filteredPros = remember(pros, query, serviceType) {
        pros.filter { pro ->
            val matchesQuery = query.isEmpty() ||
                pro.name.contains(query, ignoreCase = true) ||
                pro.serviceType.contains(query, ignoreCase = true) ||
                pro.city.contains(query, ignoreCase = true) ||
                pro.company.contains(query, ignoreCase = true)
            val matchesType = serviceType == null ||
                pro.serviceType.equals(serviceType, ignoreCase = true)
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
                Text("←")
            }
            Text(
                text = if (serviceType != null) "$serviceType Pros" else "Search Results",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredPros.isEmpty()) {
            Text(
                text = "No pros found. Try a different search.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPros) { pro ->
                    ProCard(pro = pro, onClick = { onProClick(pro) })
                }
            }
        }
    }
}
