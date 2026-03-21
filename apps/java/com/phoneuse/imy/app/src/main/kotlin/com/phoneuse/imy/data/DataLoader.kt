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

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

/**
 * Data loader for loading profile items and app permissions from JSON files.
 * This allows easy testing by replacing the JSON file without rebuilding the app.
 */
object DataLoader {
    
    /**
     * Load profile items from a JSON file.
     * Expected JSON format:
     * {
     *   "profile_items": [
     *     {"level": "low", "key": "name", "value": "John Doe"},
     *     {"level": "high", "key": "id_number", "value": "1234567890"}
     *   ]
     * }
     */
    fun loadProfileItemsFromJson(context: Context, jsonFilePath: String): List<ProfileItemDemo> {
        return try {
            val file = File(jsonFilePath)
            if (!file.exists()) {
                return getDefaultProfileItems()
            }
            
            val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val itemsArray = jsonObject.getJSONArray("profile_items")
            
            val items = mutableListOf<ProfileItemDemo>()
            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.getJSONObject(i)
                items.add(
                    ProfileItemDemo(
                        level = itemObj.getString("level"),
                        key = itemObj.getString("key"),
                        value = itemObj.getString("value")
                    )
                )
            }
            items
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultProfileItems()
        }
    }
    
    /**
     * Load app permissions from a JSON file.
     * Expected JSON format:
     * {
     *   "app_permissions": [
     *     {"app_name": "calendar", "level": "low"},
     *     {"app_name": "contacts", "level": "high"}
     *   ]
     * }
     */
    fun loadAppPermissionsFromJson(context: Context, jsonFilePath: String): List<AppPermissionDemo> {
        return try {
            val file = File(jsonFilePath)
            if (!file.exists()) {
                return getDefaultAppPermissions()
            }
            
            val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val permissionsArray = jsonObject.getJSONArray("app_permissions")
            
            val permissions = mutableListOf<AppPermissionDemo>()
            for (i in 0 until permissionsArray.length()) {
                val permObj = permissionsArray.getJSONObject(i)
                permissions.add(
                    AppPermissionDemo(
                        appName = permObj.getString("app_name"),
                        level = permObj.getString("level")
                    )
                )
            }
            permissions
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultAppPermissions()
        }
    }
    
    /**
     * Load privacy mode from a JSON file.
     * Expected JSON format:
     * {
     *   "privacy_mode": "full_control" or "semi_control"
     * }
     */
    fun loadPrivacyModeFromJson(context: Context, jsonFilePath: String): String {
        return try {
            val file = File(jsonFilePath)
            if (!file.exists()) {
                return "full_control"
            }
            
            val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            
            if (jsonObject.has("privacy_mode")) {
                val mode = jsonObject.getString("privacy_mode")
                if (mode == "full_control" || mode == "semi_control") {
                    mode
                } else {
                    "full_control"
                }
            } else {
                "full_control"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "full_control"
        }
    }
    
    /**
     * Load both profile items and app permissions from a single JSON file.
     * Expected JSON format:
     * {
     *   "privacy_mode": "full_control" or "semi_control",
     *   "profile_items": [...],
     *   "app_permissions": [...]
     * }
     */
    fun loadAllFromJson(context: Context, jsonFilePath: String): Triple<String, List<ProfileItemDemo>, List<AppPermissionDemo>> {
        return try {
            val file = File(jsonFilePath)
            if (!file.exists()) {
                return Triple("full_control", getDefaultProfileItems(), getDefaultAppPermissions())
            }
            
            val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            
            val profileItems = if (jsonObject.has("profile_items")) {
                val itemsArray = jsonObject.getJSONArray("profile_items")
                val items = mutableListOf<ProfileItemDemo>()
                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(i)
                    items.add(
                        ProfileItemDemo(
                            level = itemObj.getString("level"),
                            key = itemObj.getString("key"),
                            value = itemObj.getString("value")
                        )
                    )
                }
                items
            } else {
                getDefaultProfileItems()
            }
            
            val appPermissions = if (jsonObject.has("app_permissions")) {
                val permissionsArray = jsonObject.getJSONArray("app_permissions")
                val permissions = mutableListOf<AppPermissionDemo>()
                for (i in 0 until permissionsArray.length()) {
                    val permObj = permissionsArray.getJSONObject(i)
                    permissions.add(
                        AppPermissionDemo(
                            appName = permObj.getString("app_name"),
                            level = permObj.getString("level")
                        )
                    )
                }
                permissions
            } else {
                getDefaultAppPermissions()
            }
            
            val privacyMode = if (jsonObject.has("privacy_mode")) {
                val mode = jsonObject.getString("privacy_mode")
                if (mode == "full_control" || mode == "semi_control") {
                    mode
                } else {
                    "full_control"
                }
            } else {
                "full_control"
            }
            
            Triple(privacyMode, profileItems, appPermissions)
        } catch (e: Exception) {
            e.printStackTrace()
            Triple("full_control", getDefaultProfileItems(), getDefaultAppPermissions())
        }
    }
    
    private fun getDefaultProfileItems(): List<ProfileItemDemo> {
        return listOf(
            ProfileItemDemo("low", "name", "John Doe"),
            ProfileItemDemo("low", "language", "English"),
            ProfileItemDemo("low", "city", "New York"),
            ProfileItemDemo("high", "id_number", "1234567890"),
            ProfileItemDemo("high", "bank_card", "1234567890123456"),
        )
    }
    
    private fun getDefaultAppPermissions(): List<AppPermissionDemo> {
        return listOf(
            AppPermissionDemo("calendar", "low"),
            AppPermissionDemo("contacts", "high"),
            AppPermissionDemo("sms", "high"),
            AppPermissionDemo("camera", "high"),
        )
    }
}

// Move data classes here so they can be used by DataLoader
data class ProfileItemDemo(
    val level: String,
    val key: String,
    val value: String,
    val id: Int = key.hashCode()
)

data class AppPermissionDemo(
    val appName: String,
    val level: String
)

