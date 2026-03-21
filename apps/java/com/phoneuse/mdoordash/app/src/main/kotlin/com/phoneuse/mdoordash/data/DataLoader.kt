package com.phoneuse.mdoordash.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

/**
 * Data loader for loading restaurants from JSON files.
 */
object DataLoader {

    private const val INTERNAL_JSON_PATH = "/data/data/com.phoneuse.mdoordash/files/mdoordash_data.json"
    private const val DEFAULT_JSON_PATH = "/sdcard/mdoordash_data.json"
    private const val FALLBACK_JSON_PATH = "/storage/emulated/0/mdoordash_data.json"

    fun loadRestaurantsFromJson(context: Context, jsonFilePath: String = DEFAULT_JSON_PATH): List<RestaurantDemo> {
        return try {
            var file = File(INTERNAL_JSON_PATH)
            if (!file.exists()) {
                file = File(jsonFilePath)
            }
            if (!file.exists()) {
                file = File(FALLBACK_JSON_PATH)
            }
            if (!file.exists()) {
                val externalFile = File(context.getExternalFilesDir(null), "mdoordash_data.json")
                if (externalFile.exists()) {
                    file = externalFile
                }
            }
            if (!file.exists()) {
                return getDefaultRestaurants()
            }

            val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val restaurantsArray = jsonObject.getJSONArray("restaurants")

            val restaurants = mutableListOf<RestaurantDemo>()
            for (i in 0 until restaurantsArray.length()) {
                val rObj = restaurantsArray.getJSONObject(i)

                // Parse menu items
                val menuItems = mutableListOf<MenuItem>()
                if (rObj.has("menu_items")) {
                    val menuArray = rObj.getJSONArray("menu_items")
                    for (j in 0 until menuArray.length()) {
                        val mObj = menuArray.getJSONObject(j)
                        menuItems.add(
                            MenuItem(
                                name = mObj.getString("name"),
                                price = mObj.getDouble("price"),
                                category = if (mObj.has("category")) mObj.getString("category") else ""
                            )
                        )
                    }
                }

                // Parse available delivery slots
                val slots = mutableListOf<String>()
                if (rObj.has("available_slots")) {
                    val slotsArray = rObj.getJSONArray("available_slots")
                    for (j in 0 until slotsArray.length()) {
                        slots.add(slotsArray.getString(j))
                    }
                }

                restaurants.add(
                    RestaurantDemo(
                        id = rObj.getInt("id"),
                        name = rObj.getString("name"),
                        cuisine = rObj.getString("cuisine"),
                        neighborhood = if (rObj.has("neighborhood")) rObj.getString("neighborhood") else "",
                        city = if (rObj.has("city")) rObj.getString("city") else "New York",
                        rating = if (rObj.has("rating")) rObj.getDouble("rating") else 4.5,
                        deliveryTimeMin = if (rObj.has("delivery_time_min")) rObj.getInt("delivery_time_min") else 30,
                        menuItems = menuItems,
                        availableSlots = slots
                    )
                )
            }
            restaurants
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultRestaurants()
        }
    }

    fun loadAllFromJson(context: Context, jsonFilePath: String = DEFAULT_JSON_PATH): List<RestaurantDemo> {
        return loadRestaurantsFromJson(context, jsonFilePath)
    }

    fun getDefaultRestaurants(): List<RestaurantDemo> {
        return listOf(
            RestaurantDemo(
                id = 1,
                name = "Trattoria Bella Vita",
                cuisine = "Italian",
                neighborhood = "Manhattan",
                city = "New York",
                rating = 4.7,
                deliveryTimeMin = 30,
                menuItems = listOf(
                    MenuItem("Bruschetta al Pomodoro", 9.99),
                    MenuItem("Spaghetti Carbonara", 16.99),
                    MenuItem("Margherita Pizza", 14.99),
                    MenuItem("Tiramisu", 8.99)
                ),
                availableSlots = listOf(
                    "2026-03-11 12:00", "2026-03-11 18:00", "2026-03-12 12:00",
                    "2026-03-13 19:00", "2026-03-14 13:00"
                )
            ),
            RestaurantDemo(
                id = 2,
                name = "Golden Dragon Palace",
                cuisine = "Chinese",
                neighborhood = "Brooklyn",
                city = "New York",
                rating = 4.3,
                deliveryTimeMin = 25,
                menuItems = listOf(
                    MenuItem("Pork Dumplings", 8.99),
                    MenuItem("Kung Pao Chicken", 14.99),
                    MenuItem("Fried Rice", 10.99)
                ),
                availableSlots = listOf(
                    "2026-03-11 11:00", "2026-03-11 19:00", "2026-03-12 12:00"
                )
            ),
            RestaurantDemo(
                id = 3,
                name = "Sakura Sushi House",
                cuisine = "Japanese",
                neighborhood = "Queens",
                city = "New York",
                rating = 4.8,
                deliveryTimeMin = 35,
                menuItems = listOf(
                    MenuItem("Edamame", 7.99),
                    MenuItem("Salmon Sashimi Platter", 22.99),
                    MenuItem("Spicy Tuna Roll", 14.99)
                ),
                availableSlots = listOf(
                    "2026-03-11 12:00", "2026-03-11 19:00", "2026-03-12 13:00"
                )
            )
        )
    }
}
