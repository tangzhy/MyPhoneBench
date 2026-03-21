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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phoneuse.mzocdoc.data.DoctorDemo

@Composable
fun DoctorListScreen(
    doctors: List<DoctorDemo>,
    query: String,
    specialty: String?,
    onDoctorClick: (DoctorDemo) -> Unit,
    onBack: () -> Unit
) {
    val filteredDoctors = remember(doctors, query, specialty) {
        doctors.filter { doctor ->
            val matchesQuery = query.isEmpty() || 
                doctor.name.contains(query, ignoreCase = true) ||
                doctor.specialty.contains(query, ignoreCase = true) ||
                doctor.city.contains(query, ignoreCase = true) ||
                doctor.hospital.contains(query, ignoreCase = true)
            val matchesSpecialty = specialty == null || doctor.specialty == specialty
            matchesQuery && matchesSpecialty
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
                text = if (specialty != null) specialty else "Search Results",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (filteredDoctors.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No doctors found")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredDoctors) { doctor ->
                    DoctorCard(
                        doctor = doctor,
                        onClick = { onDoctorClick(doctor) }
                    )
                }
            }
        }
    }
}

