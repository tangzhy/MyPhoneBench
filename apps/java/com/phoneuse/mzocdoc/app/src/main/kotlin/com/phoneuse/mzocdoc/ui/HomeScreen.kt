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

package com.phoneuse.mzocdoc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phoneuse.mzocdoc.data.DoctorDemo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    doctors: List<DoctorDemo>,
    onSearch: (String, String?) -> Unit,
    onDoctorClick: (DoctorDemo) -> Unit,
    onMyAppointmentsClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val specialties = remember(doctors) { doctors.map { it.specialty }.distinct().sorted() }
    val popularDoctors = doctors.take(3)
    
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
                text = "Find a Doctor",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onMyAppointmentsClick,
                modifier = Modifier.semantics { contentDescription = "My Appointments button" }
            ) {
                Text("My Appointments")
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
                label = { Text("Search by name, specialty, or city") },
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
        
        // Specialty shortcuts
        Text(
            text = "Specialties",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            specialties.forEach { specialty ->
                FilterChip(
                    selected = false,
                    onClick = { onSearch("", specialty) },
                    label = { Text(specialty) },
                    modifier = Modifier
                        .semantics { contentDescription = "Specialty filter: $specialty" }
                        .testTag("specialty_$specialty")
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Popular doctors
        Text(
            text = "Popular Doctors",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(popularDoctors) { doctor ->
                DoctorCard(
                    doctor = doctor,
                    onClick = { onDoctorClick(doctor) }
                )
            }
        }
    }
}

@Composable
fun DoctorCard(
    doctor: DoctorDemo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Doctor card: ${doctor.name}, ${doctor.specialty}" }
            .testTag("doctor_card_${doctor.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = doctor.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = doctor.specialty,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = doctor.hospital,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "★ ${doctor.rating}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

