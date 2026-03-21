package com.phoneuse.mgeico.ui

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
import com.phoneuse.mgeico.data.InsurancePlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    plans: List<InsurancePlan>,
    onSearch: (String, String?) -> Unit,
    onPlanClick: (InsurancePlan) -> Unit,
    onMyQuotesClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val coverageTypes = remember(plans) { plans.map { it.coverageType }.distinct().sorted() }
    val popularPlans = plans.take(3)

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
                text = "Find Insurance",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onMyQuotesClick,
                modifier = Modifier.semantics { contentDescription = "My Quotes button" }
            ) {
                Text("My Quotes")
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
                label = { Text("Search by plan name, type, or provider") },
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
            text = "Coverage Types",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            coverageTypes.forEach { type ->
                FilterChip(
                    selected = false,
                    onClick = { onSearch("", type) },
                    label = { Text(type) },
                    modifier = Modifier
                        .semantics { contentDescription = "Coverage type filter: $type" }
                        .testTag("coverage_type_$type")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Popular Plans",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(popularPlans) { plan ->
                PlanCard(plan = plan, onClick = { onPlanClick(plan) })
            }
        }
    }
}

@Composable
fun PlanCard(
    plan: InsurancePlan,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Plan card: ${plan.planName}, ${plan.coverageType}" }
            .testTag("plan_card_${plan.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = plan.planName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = plan.coverageType,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%.0f", plan.monthlyPremium)}/mo | \$${plan.deductible} deductible",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "★ ${plan.rating}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
