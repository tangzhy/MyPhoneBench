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

package com.phoneuse.imy.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "profile_items")
data class ProfileItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "level")
    val level: String,  // "low" or "high"
    
    @ColumnInfo(name = "key")
    val key: String,
    
    @ColumnInfo(name = "value")
    val value: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: String = "",
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = ""
)

@Entity(tableName = "app_permissions")
data class AppPermission(
    @PrimaryKey
    @ColumnInfo(name = "app_name")
    val appName: String,
    
    @ColumnInfo(name = "level")
    val level: String  // "low" or "high"
)

@Entity(tableName = "settings")
data class Setting(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,
    
    @ColumnInfo(name = "value")
    val value: String
)

@Entity(tableName = "write_permissions")
data class WritePermission(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: String = "",
    
    @ColumnInfo(name = "key")
    val key: String,
    
    @ColumnInfo(name = "value")
    val value: String,
    
    @ColumnInfo(name = "level")
    val level: String,
    
    @ColumnInfo(name = "status")
    val status: String,  // "approved", "modified", "denied"
    
    @ColumnInfo(name = "approved_key")
    val approvedKey: String? = null,
    
    @ColumnInfo(name = "approved_value")
    val approvedValue: String? = null,
    
    @ColumnInfo(name = "approved_level")
    val approvedLevel: String? = null,
    
    @ColumnInfo(name = "consumed")
    val consumed: Int = 0  // 0 = not consumed, 1 = consumed
)

@Entity(tableName = "access_log")
data class AccessLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: String = "",
    
    @ColumnInfo(name = "tool")
    val tool: String,  // "request_permission", "save_profile", "gui_access_blocked"
    
    @ColumnInfo(name = "action")
    val action: String,
    
    @ColumnInfo(name = "item_key")
    val itemKey: String? = null,
    
    @ColumnInfo(name = "item_level")
    val itemLevel: String? = null,
    
    @ColumnInfo(name = "reason")
    val reason: String? = null,
    
    @ColumnInfo(name = "source")
    val source: String? = null,
    
    @ColumnInfo(name = "details")
    val details: String? = null  // JSON blob
)

