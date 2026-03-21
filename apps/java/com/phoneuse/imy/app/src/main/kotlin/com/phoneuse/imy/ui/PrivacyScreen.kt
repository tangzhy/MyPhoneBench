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

package com.phoneuse.imy.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phoneuse.imy.data.AppPermissionDemo
import com.phoneuse.imy.data.DataLoader
import com.phoneuse.imy.data.ProfileItemDemo

@Composable
fun PrivacyScreen() {
    val context = LocalContext.current
    
    // Load data from JSON file (for testing) or use defaults
    // JSON file path: /sdcard/imy_data.json (can be pushed via adb)
    val jsonFilePath = "/sdcard/imy_data.json"
    
    // Load all data from JSON (privacy mode, profile items, app permissions)
    val (loadedPrivacyMode, loadedProfileItems, loadedAppPermissions) = remember {
        try {
            DataLoader.loadAllFromJson(context, jsonFilePath)
        } catch (e: Exception) {
            Triple(
                DataLoader.loadPrivacyModeFromJson(context, jsonFilePath),
                DataLoader.loadProfileItemsFromJson(context, jsonFilePath),
                DataLoader.loadAppPermissionsFromJson(context, jsonFilePath)
            )
        }
    }
    
    var privacyMode by remember { mutableStateOf(loadedPrivacyMode) }
    
    // Mutable state for profile items - load from JSON if available
    var profileItems by remember {
        mutableStateOf(loadedProfileItems)
    }
    
    // Mutable state for app permissions - load from JSON if available
    var appPermissions by remember {
        mutableStateOf(loadedAppPermissions)
    }
    
    var expandedItemId by remember { mutableStateOf<Int?>(null) }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var showAddAppDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Text(
                text = "Privacy Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Privacy Mode Slider
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Privacy Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Full Control")
                        Switch(
                            checked = privacyMode == "semi_control",
                            onCheckedChange = { checked ->
                                privacyMode = if (checked) "semi_control" else "full_control"
                            }
                        )
                        Text("Semi Control")
                    }
                }
            }
        }
        
        // Profile Items Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Personal Data",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showAddProfileDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Profile Item")
                }
            }
        }
        
        // Profile Items
        items(profileItems) { item ->
            ProfileItemCard(
                item = item,
                isExpanded = expandedItemId == item.id,
                onExpandClick = { 
                    expandedItemId = if (expandedItemId == item.id) null else item.id
                },
                onLevelToggle = { 
                    profileItems = profileItems.map { 
                        if (it.id == item.id) it.copy(level = if (it.level == "low") "high" else "low") else it
                    }
                },
                onKeyEdit = { newKey ->
                    profileItems = profileItems.map { 
                        if (it.id == item.id) it.copy(key = newKey) else it
                    }
                },
                onValueEdit = { newValue ->
                    profileItems = profileItems.map { 
                        if (it.id == item.id) it.copy(value = newValue) else it
                    }
                },
                onDelete = {
                    profileItems = profileItems.filter { it.id != item.id }
                    if (expandedItemId == item.id) expandedItemId = null
                }
            )
        }
        
        // App Permissions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App Permissions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showAddAppDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add App Permission")
                }
            }
        }
        
        // App Permissions
        items(appPermissions) { permission ->
            AppPermissionCard(
                permission = permission,
                onLevelToggle = {
                    appPermissions = appPermissions.map {
                        if (it.appName == permission.appName) 
                            it.copy(level = if (it.level == "low") "high" else "low") 
                        else it
                    }
                },
                onAppNameEdit = { newName ->
                    appPermissions = appPermissions.map {
                        if (it.appName == permission.appName) it.copy(appName = newName) else it
                    }
                },
                onDelete = {
                    appPermissions = appPermissions.filter { it.appName != permission.appName }
                }
            )
        }
    }
    
    // Add Profile Item Dialog
    if (showAddProfileDialog) {
        AddProfileItemDialog(
            onDismiss = { showAddProfileDialog = false },
            onAdd = { level, key, value ->
                profileItems = profileItems + ProfileItemDemo(level, key, value)
                showAddProfileDialog = false
            }
        )
    }
    
    // Add App Permission Dialog
    if (showAddAppDialog) {
        AddAppPermissionDialog(
            onDismiss = { showAddAppDialog = false },
            onAdd = { appName, level ->
                appPermissions = appPermissions + AppPermissionDemo(appName, level)
                showAddAppDialog = false
            }
        )
    }
}

// Data classes moved to com.phoneuse.imy.data package

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileItemCard(
    item: ProfileItemDemo,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onLevelToggle: () -> Unit,
    onKeyEdit: (String) -> Unit,
    onValueEdit: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showValue by remember { mutableStateOf(false) }
    var isEditingKey by remember { mutableStateOf(false) }
    var isEditingValue by remember { mutableStateOf(false) }
    var editedKey by remember { mutableStateOf(item.key) }
    var editedValue by remember { mutableStateOf(item.value) }
    
    // Reset edited values when item changes
    LaunchedEffect(item.key, item.value) {
        editedKey = item.key
        editedValue = item.value
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Level badge - clickable to toggle
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (item.level == "high") 
                            MaterialTheme.colorScheme.errorContainer 
                        else 
                            MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable { onLevelToggle() }
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = item.level.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Key - editable
                    if (isEditingKey) {
                        OutlinedTextField(
                            value = editedKey,
                            onValueChange = { editedKey = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            trailingIcon = {
                                TextButton(onClick = {
                                    if (editedKey.isNotBlank()) {
                                        onKeyEdit(editedKey)
                                    }
                                    isEditingKey = false
                                }) {
                                    Text("Save")
                                }
                            }
                        )
                    } else {
                        Text(
                            text = item.key,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { isEditingKey = true }
                                .weight(1f)
                        )
                    }
                }
                
                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            // Value display - LOW level always visible, HIGH level only when expanded and revealed
            if (item.level == "low") {
                // LOW level: always show value directly (no need to expand)
                Spacer(modifier = Modifier.height(8.dp))
                if (isEditingValue) {
                    OutlinedTextField(
                        value = editedValue,
                        onValueChange = { editedValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Value") },
                        trailingIcon = {
                            TextButton(onClick = {
                                onValueEdit(editedValue)
                                isEditingValue = false
                            }) {
                                Text("Save")
                            }
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEditingValue = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Value: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                // HIGH level: only show when expanded and revealed
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!showValue) {
                        Button(onClick = { showValue = true }) {
                            Text("Reveal Value")
                        }
                    } else {
                        if (isEditingValue) {
                            OutlinedTextField(
                                value = editedValue,
                                onValueChange = { editedValue = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Value") },
                                trailingIcon = {
                                    TextButton(onClick = {
                                        onValueEdit(editedValue)
                                        isEditingValue = false
                                    }) {
                                        Text("Save")
                                    }
                                }
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isEditingValue = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Value: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.value,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            
            // Expand/collapse button (only for HIGH level items)
            if (item.level == "high") {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onExpandClick) {
                    Text(if (isExpanded) "Collapse" else "Expand")
                }
            }
        }
    }
}

@Composable
fun AppPermissionCard(
    permission: AppPermissionDemo,
    onLevelToggle: () -> Unit,
    onAppNameEdit: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(permission.appName) }
    
    LaunchedEffect(permission.appName) {
        editedName = permission.appName
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App name - editable
            if (isEditingName) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = {
                            if (editedName.isNotBlank()) {
                                onAppNameEdit(editedName)
                            }
                            isEditingName = false
                        }) {
                            Text("Save")
                        }
                    }
                )
            } else {
                Text(
                    text = permission.appName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isEditingName = true }
                )
            }
            
            // Level badge - clickable to toggle
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (permission.level == "high") 
                    MaterialTheme.colorScheme.errorContainer 
                else 
                    MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clickable { onLevelToggle() }
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = permission.level.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            // Delete button
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddProfileItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var level by remember { mutableStateOf("low") }
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Profile Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Level:")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = level == "low",
                        onClick = { level = "low" }
                    )
                    Text("LOW")
                    RadioButton(
                        selected = level == "high",
                        onClick = { level = "high" }
                    )
                    Text("HIGH")
                }
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Key") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (key.isNotBlank() && value.isNotBlank()) {
                        onAdd(level, key, value)
                    }
                },
                enabled = key.isNotBlank() && value.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddAppPermissionDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var appName by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("low") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add App Permission") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Level:")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = level == "low",
                        onClick = { level = "low" }
                    )
                    Text("LOW")
                    RadioButton(
                        selected = level == "high",
                        onClick = { level = "high" }
                    )
                    Text("HIGH")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (appName.isNotBlank()) {
                        onAdd(appName, level)
                    }
                },
                enabled = appName.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
