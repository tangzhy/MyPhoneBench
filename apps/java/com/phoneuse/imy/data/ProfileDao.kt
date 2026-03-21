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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProfileDao {
    // Profile Items
    @Query("SELECT * FROM profile_items ORDER BY id ASC")
    fun getAllProfileItems(): List<ProfileItem>
    
    @Query("SELECT * FROM profile_items WHERE level = :level ORDER BY id ASC")
    fun getProfileItemsByLevel(level: String): List<ProfileItem>
    
    @Query("SELECT * FROM profile_items WHERE key = :key LIMIT 1")
    fun getProfileItemByKey(key: String): ProfileItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProfileItem(item: ProfileItem): Long
    
    @Update
    fun updateProfileItem(item: ProfileItem)
    
    @Query("DELETE FROM profile_items WHERE id = :id")
    fun deleteProfileItem(id: Int)
    
    @Query("DELETE FROM profile_items WHERE key = :key")
    fun deleteProfileItemByKey(key: String)
    
    // App Permissions
    @Query("SELECT * FROM app_permissions ORDER BY app_name ASC")
    fun getAllAppPermissions(): List<AppPermission>
    
    @Query("SELECT * FROM app_permissions WHERE app_name = :appName LIMIT 1")
    fun getAppPermission(appName: String): AppPermission?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAppPermission(permission: AppPermission)
    
    @Query("DELETE FROM app_permissions WHERE app_name = :appName")
    fun deleteAppPermission(appName: String)
    
    // Settings
    @Query("SELECT * FROM settings WHERE key = :key LIMIT 1")
    fun getSetting(key: String): Setting?
    
    @Query("SELECT value FROM settings WHERE key = :key LIMIT 1")
    fun getSettingValue(key: String): String?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSetting(setting: Setting)
    
    // Write Permissions
    @Query("SELECT * FROM write_permissions WHERE key = :key AND value = :value AND level = :level AND consumed = 0 ORDER BY timestamp DESC LIMIT 1")
    fun getUnconsumedWritePermission(key: String, value: String, level: String): WritePermission?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWritePermission(permission: WritePermission): Long
    
    @Query("UPDATE write_permissions SET consumed = 1 WHERE id = :id")
    fun markWritePermissionConsumed(id: Int)
    
    // Access Log
    @Query("SELECT * FROM access_log ORDER BY timestamp DESC")
    fun getAllAccessLogs(): List<AccessLog>
    
    @Insert
    fun insertAccessLog(log: AccessLog): Long
}

