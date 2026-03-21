package com.phoneuse.mopentable.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

object DataLoader {

    private const val INTERNAL_JSON_PATH =
        "/data/data/com.phoneuse.mopentable/files/mopentable_data.json"
    private const val DEFAULT_JSON_PATH = "/sdcard/mopentable_data.json"
    private const val FALLBACK_JSON_PATH = "/storage/emulated/0/mopentable_data.json"

    fun loadRestaurantsFromJson(context: Context): List<Restaurant> {
        val json = loadJsonObject(context) ?: return getDefaultRestaurants()
        return try {
            val arr = json.getJSONArray("restaurants")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val slots = mutableListOf<String>()
                val sa = o.getJSONArray("available_slots")
                for (j in 0 until sa.length()) slots.add(sa.getString(j))
                Restaurant(
                    id = o.getInt("id"),
                    name = o.getString("name"),
                    cuisine = o.getString("cuisine"),
                    neighborhood = o.getString("neighborhood"),
                    city = o.getString("city"),
                    rating = o.getDouble("rating"),
                    priceLevel = o.getInt("price_level"),
                    hours = o.getString("hours"),
                    availableSlots = slots
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultRestaurants()
        }
    }

    private fun loadJsonObject(context: Context): JSONObject? {
        return try {
            var file = File(INTERNAL_JSON_PATH)
            if (!file.exists()) file = File(DEFAULT_JSON_PATH)
            if (!file.exists()) file = File(FALLBACK_JSON_PATH)
            if (!file.exists()) {
                val ext = File(context.getExternalFilesDir(null), "mopentable_data.json")
                if (ext.exists()) file = ext
            }
            if (!file.exists()) return null
            val text = FileInputStream(file).bufferedReader().use { it.readText() }
            JSONObject(text)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getDefaultRestaurants(): List<Restaurant> = listOf(
        Restaurant(
            id = 1,
            name = "Le Petit Bistro",
            cuisine = "French",
            neighborhood = "SoHo",
            city = "New York",
            rating = 4.5,
            priceLevel = 3,
            hours = "11:00 AM - 10:00 PM",
            availableSlots = listOf("6:00 PM", "6:30 PM", "7:00 PM", "8:00 PM", "9:00 PM")
        ),
        Restaurant(
            id = 2,
            name = "Sakura Sushi",
            cuisine = "Japanese",
            neighborhood = "Midtown",
            city = "New York",
            rating = 4.8,
            priceLevel = 2,
            hours = "12:00 PM - 11:00 PM",
            availableSlots = listOf("5:30 PM", "6:00 PM", "7:30 PM", "8:30 PM", "9:30 PM")
        ),
        Restaurant(
            id = 3,
            name = "Casa Italiana",
            cuisine = "Italian",
            neighborhood = "Brooklyn Heights",
            city = "Brooklyn",
            rating = 4.2,
            priceLevel = 2,
            hours = "5:00 PM - 10:30 PM",
            availableSlots = listOf("5:00 PM", "6:00 PM", "7:00 PM", "8:00 PM", "9:00 PM", "10:00 PM")
        )
    )
}
