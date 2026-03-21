package com.phoneuse.mgeico.ui

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
import com.phoneuse.mgeico.data.InsurancePlan

@Composable
fun PlanListScreen(
    plans: List<InsurancePlan>,
    query: String,
    coverageType: String?,
    onPlanClick: (InsurancePlan) -> Unit,
    onBack: () -> Unit
) {
    val filteredPlans = remember(plans, query, coverageType) {
        plans.filter { plan ->
            val matchesQuery = query.isEmpty() ||
                plan.planName.contains(query, ignoreCase = true) ||
                plan.coverageType.contains(query, ignoreCase = true) ||
                plan.provider.contains(query, ignoreCase = true)
            val matchesType = coverageType == null || plan.coverageType == coverageType
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
                text = coverageType ?: "Search Results",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredPlans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No plans found")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredPlans) { plan ->
                    PlanCard(plan = plan, onClick = { onPlanClick(plan) })
                }
            }
        }
    }
}
