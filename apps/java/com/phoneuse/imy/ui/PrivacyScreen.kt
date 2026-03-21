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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.phoneuse.imy.data.*

@Composable
fun PrivacyScreen(database: ProfileDatabase) {
    val dao = database.profileDao()
    val scope = rememberCoroutineScope()
    
    var privacyMode by remember { mutableStateOf("full_control") }
    var profileItems by remember { mutableStateOf<List<ProfileItem>>(emptyList()) }
    var appPermissions by remember { mutableStateOf<List<AppPermission>>(emptyList()) }
    var expandedItemId by remember { mutableStateOf<Int?>(null) }
    
    // Load initial data
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            privacyMode = dao.getSettingValue("privacy_mode") ?: "full_control"
            profileItems = dao.getAllProfileItems()
            appPermissions = dao.getAllAppPermissions()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "隐私管理",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Privacy Mode Slider
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "隐私模式",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("完全可控")
                    Switch(
                        checked = privacyMode == "semi_control",
                        onCheckedChange = { checked ->
                            scope.launch(Dispatchers.IO) {
                                privacyMode = if (checked) "semi_control" else "full_control"
                                dao.insertSetting(Setting("privacy_mode", privacyMode))
                            }
                        }
                    )
                    Text("半可控")
                }
            }
        }
        
        // Profile Items Section
        Text(
            text = "个人数据",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(profileItems) { item ->
                ProfileItemCard(
                    item = item,
                    isExpanded = expandedItemId == item.id,
                    onExpandClick = { 
                        expandedItemId = if (expandedItemId == item.id) null else item.id
                    },
                    onDelete = {
                        scope.launch(Dispatchers.IO) {
                            dao.deleteProfileItem(item.id)
                            profileItems = dao.getAllProfileItems()
                        }
                    }
                )
            }
        }
        
        // App Permissions Section
        Text(
            text = "App 资源权限",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(appPermissions) { permission ->
                AppPermissionCard(permission = permission)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileItemCard(
    item: ProfileItem,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showValue by remember { mutableStateOf(false) }
    
    Card(
        onClick = onExpandClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (item.level == "high") 
                            MaterialTheme.colorScheme.errorContainer 
                        else 
                            MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = item.level.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.key,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDelete) {
                    Text("删除")
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                if (item.level == "high" && !showValue) {
                    Button(onClick = { showValue = true }) {
                        Text("显示值 (点击揭示)")
                    }
                } else {
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AppPermissionCard(permission: AppPermission) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = permission.appName,
                style = MaterialTheme.typography.titleMedium
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (permission.level == "high") 
                    MaterialTheme.colorScheme.errorContainer 
                else 
                    MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = permission.level.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            )
        }
    }
}

