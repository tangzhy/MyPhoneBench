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

package com.phoneuse.meventbrite.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

/**
 * Data loader for loading events from JSON files.
 * This allows easy testing by replacing the JSON file without rebuilding the app.
 */
object DataLoader {

    private const val INTERNAL_JSON_PATH = "/data/data/com.phoneuse.meventbrite/files/meventbrite_data.json"
    private const val DEFAULT_JSON_PATH = "/sdcard/meventbrite_data.json"
    private const val FALLBACK_JSON_PATH = "/storage/emulated/0/meventbrite_data.json"

    /**
     * Load events from a JSON file.
     * Expected JSON format:
     * {
     *   "events": [
     *     {
     *       "id": 1,
     *       "title": "NYC Tech Summit 2026",
     *       "category": "Technology",
     *       "venue": "Javits Center",
     *       "city": "New York",
     *       "date": "2026-03-15",
     *       "time": "09:00",
     *       "price": 0.0,
     *       "organizer": "TechConnect NYC",
     *       "description": "Annual technology conference",
     *       "available_tickets": 200
     *     }
     *   ]
     * }
     */
    fun loadEventsFromJson(context: Context, jsonFilePath: String = DEFAULT_JSON_PATH): List<EventDemo> {
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
                val externalFile = File(context.getExternalFilesDir(null), "meventbrite_data.json")
                if (externalFile.exists()) {
                    file = externalFile
                }
            }
            if (!file.exists()) {
                return getDefaultEvents()
            }

            val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val eventsArray = jsonObject.getJSONArray("events")

            val events = mutableListOf<EventDemo>()
            for (i in 0 until eventsArray.length()) {
                val eventObj = eventsArray.getJSONObject(i)

                events.add(
                    EventDemo(
                        id = eventObj.getInt("id"),
                        title = eventObj.getString("title"),
                        category = eventObj.getString("category"),
                        venue = eventObj.getString("venue"),
                        city = eventObj.getString("city"),
                        date = eventObj.getString("date"),
                        time = eventObj.getString("time"),
                        price = if (eventObj.has("price")) eventObj.getDouble("price") else 0.0,
                        organizer = eventObj.getString("organizer"),
                        description = eventObj.getString("description"),
                        availableTickets = if (eventObj.has("available_tickets")) eventObj.getInt("available_tickets") else 100
                    )
                )
            }
            events
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultEvents()
        }
    }

    /**
     * Load all data from JSON file.
     */
    fun loadAllFromJson(context: Context, jsonFilePath: String = DEFAULT_JSON_PATH): List<EventDemo> {
        return loadEventsFromJson(context, jsonFilePath)
    }

    fun getDefaultEvents(): List<EventDemo> {
        return listOf(
            EventDemo(
                id = 1,
                title = "NYC Tech Summit 2026",
                category = "Technology",
                venue = "Javits Center",
                city = "New York",
                date = "2026-03-15",
                time = "09:00",
                price = 0.0,
                organizer = "TechConnect NYC",
                description = "Annual technology conference featuring AI and cloud computing",
                availableTickets = 200
            ),
            EventDemo(
                id = 2,
                title = "Jazz Night at Blue Note",
                category = "Music",
                venue = "Blue Note Jazz Club",
                city = "New York",
                date = "2026-03-16",
                time = "20:00",
                price = 45.0,
                organizer = "NYC Jazz Foundation",
                description = "An evening of live jazz performances",
                availableTickets = 80
            ),
            EventDemo(
                id = 3,
                title = "Spring Food Festival",
                category = "Food & Drink",
                venue = "Central Park",
                city = "New York",
                date = "2026-03-17",
                time = "11:00",
                price = 25.0,
                organizer = "NYC Foodies",
                description = "Taste dishes from top NYC restaurants",
                availableTickets = 500
            )
        )
    }
}
