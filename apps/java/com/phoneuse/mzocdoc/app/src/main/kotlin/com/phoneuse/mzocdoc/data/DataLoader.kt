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

package com.phoneuse.mzocdoc.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

/**
 * Data loader for loading doctors and appointments from JSON files.
 * This allows easy testing by replacing the JSON file without rebuilding the app.
 */
object DataLoader {
    
    private const val INTERNAL_JSON_PATH = "/data/data/com.phoneuse.mzocdoc/files/mzocdoc_data.json"
    private const val DEFAULT_JSON_PATH = "/sdcard/mzocdoc_data.json"
    private const val FALLBACK_JSON_PATH = "/storage/emulated/0/mzocdoc_data.json"
    
    /**
     * Load doctors from a JSON file.
     * Expected JSON format:
     * {
     *   "doctors": [
     *     {
     *       "id": 1,
     *       "name": "Dr. Sarah Smith",
     *       "specialty": "Dermatology",
     *       "hospital": "NYC Medical Center",
     *       "city": "New York",
     *       "rating": 4.8,
     *       "available_slots": ["2026-02-11 09:00", "2026-02-11 10:00"]
     *     }
     *   ]
     * }
     */
    fun loadDoctorsFromJson(context: Context, jsonFilePath: String = DEFAULT_JSON_PATH): List<DoctorDemo> {
        return try {
            // Try internal data directory first (no permissions needed)
            var file = File(INTERNAL_JSON_PATH)
            // Then try /sdcard/ path
            if (!file.exists()) {
                file = File(jsonFilePath)
            }
            // Try fallback path if default doesn't exist
            if (!file.exists()) {
                file = File(FALLBACK_JSON_PATH)
            }
            // Try app's external files directory
            if (!file.exists()) {
                val externalFile = File(context.getExternalFilesDir(null), "mzocdoc_data.json")
                if (externalFile.exists()) {
                    file = externalFile
                }
            }
            if (!file.exists()) {
                return getDefaultDoctors()
            }
            
            val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val doctorsArray = jsonObject.getJSONArray("doctors")
            
            val doctors = mutableListOf<DoctorDemo>()
            for (i in 0 until doctorsArray.length()) {
                val doctorObj = doctorsArray.getJSONObject(i)
                val slotsArray = doctorObj.getJSONArray("available_slots")
                val slots = mutableListOf<String>()
                for (j in 0 until slotsArray.length()) {
                    slots.add(slotsArray.getString(j))
                }
                
                doctors.add(
                    DoctorDemo(
                        id = doctorObj.getInt("id"),
                        name = doctorObj.getString("name"),
                        specialty = doctorObj.getString("specialty"),
                        hospital = doctorObj.getString("hospital"),
                        city = doctorObj.getString("city"),
                        rating = if (doctorObj.has("rating")) doctorObj.getDouble("rating") else 4.5,
                        availableSlots = slots
                    )
                )
            }
            doctors
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultDoctors()
        }
    }
    
    /**
     * Load all data from JSON file.
     */
    fun loadAllFromJson(context: Context, jsonFilePath: String = DEFAULT_JSON_PATH): List<DoctorDemo> {
        return loadDoctorsFromJson(context, jsonFilePath)
    }
    
    fun getDefaultDoctors(): List<DoctorDemo> {
        return listOf(
            DoctorDemo(
                id = 1,
                name = "Dr. Sarah Smith",
                specialty = "Dermatology",
                hospital = "NYC Medical Center",
                city = "New York",
                rating = 4.8,
                availableSlots = listOf(
                    "2026-02-11 09:00",
                    "2026-02-11 10:00",
                    "2026-02-11 14:00",
                    "2026-02-12 09:00",
                    "2026-02-12 11:00"
                )
            ),
            DoctorDemo(
                id = 2,
                name = "Dr. Michael Johnson",
                specialty = "General Practice",
                hospital = "Manhattan Health Clinic",
                city = "New York",
                rating = 4.6,
                availableSlots = listOf(
                    "2026-02-11 08:00",
                    "2026-02-11 11:00",
                    "2026-02-11 15:00"
                )
            ),
            DoctorDemo(
                id = 3,
                name = "Dr. Emily Chen",
                specialty = "Cardiology",
                hospital = "NYC Medical Center",
                city = "New York",
                rating = 4.9,
                availableSlots = listOf(
                    "2026-02-11 13:00",
                    "2026-02-12 08:00",
                    "2026-02-12 13:00"
                )
            )
        )
    }
}

