package com.phoneuse.mbooking.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

object DataLoader {

    private const val INTERNAL_JSON_PATH = "/data/data/com.phoneuse.mbooking/files/mbooking_data.json"
    private const val DEFAULT_JSON_PATH = "/sdcard/mbooking_data.json"
    private const val FALLBACK_JSON_PATH = "/storage/emulated/0/mbooking_data.json"

    fun loadHotelsFromJson(context: Context, jsonFilePath: String = DEFAULT_JSON_PATH): List<Hotel> {
        return try {
            var file = File(INTERNAL_JSON_PATH)
            if (!file.exists()) {
                file = File(jsonFilePath)
            }
            if (!file.exists()) {
                file = File(FALLBACK_JSON_PATH)
            }
            if (!file.exists()) {
                val externalFile = File(context.getExternalFilesDir(null), "mbooking_data.json")
                if (externalFile.exists()) {
                    file = externalFile
                }
            }
            if (!file.exists()) {
                return getDefaultHotels()
            }

            val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val hotelsArray = jsonObject.getJSONArray("hotels")

            val hotels = mutableListOf<Hotel>()
            for (i in 0 until hotelsArray.length()) {
                val obj = hotelsArray.getJSONObject(i)

                val amenitiesArr = obj.getJSONArray("amenities")
                val amenities = mutableListOf<String>()
                for (j in 0 until amenitiesArr.length()) {
                    amenities.add(amenitiesArr.getString(j))
                }

                val roomTypesArr = obj.getJSONArray("room_types")
                val roomTypes = mutableListOf<String>()
                for (j in 0 until roomTypesArr.length()) {
                    roomTypes.add(roomTypesArr.getString(j))
                }

                val datesArr = obj.getJSONArray("available_dates")
                val dates = mutableListOf<String>()
                for (j in 0 until datesArr.length()) {
                    dates.add(datesArr.getString(j))
                }

                hotels.add(
                    Hotel(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        neighborhood = obj.getString("neighborhood"),
                        city = obj.getString("city"),
                        starRating = obj.getInt("star_rating"),
                        pricePerNight = obj.getInt("price_per_night"),
                        amenities = amenities,
                        roomTypes = roomTypes,
                        availableDates = dates
                    )
                )
            }
            hotels
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultHotels()
        }
    }

    fun loadAllFromJson(context: Context, jsonFilePath: String = DEFAULT_JSON_PATH): List<Hotel> {
        return loadHotelsFromJson(context, jsonFilePath)
    }

    fun getDefaultHotels(): List<Hotel> {
        return listOf(
            Hotel(
                id = 1,
                name = "The Manhattan Grand",
                neighborhood = "Midtown",
                city = "New York",
                starRating = 4,
                pricePerNight = 250,
                amenities = listOf("WiFi", "Pool", "Gym"),
                roomTypes = listOf("Standard Double", "King Suite", "Deluxe Queen"),
                availableDates = listOf("2026-03-15", "2026-03-16", "2026-03-17", "2026-03-18")
            ),
            Hotel(
                id = 2,
                name = "Brooklyn Bridge Inn",
                neighborhood = "Brooklyn Heights",
                city = "New York",
                starRating = 3,
                pricePerNight = 150,
                amenities = listOf("WiFi", "Breakfast"),
                roomTypes = listOf("Standard Double", "Twin Room"),
                availableDates = listOf("2026-03-15", "2026-03-16", "2026-03-17")
            ),
            Hotel(
                id = 3,
                name = "Central Park Luxury",
                neighborhood = "Upper West Side",
                city = "New York",
                starRating = 5,
                pricePerNight = 450,
                amenities = listOf("WiFi", "Pool", "Spa", "Gym", "Restaurant"),
                roomTypes = listOf("King Suite", "Presidential Suite", "Deluxe Queen"),
                availableDates = listOf("2026-03-16", "2026-03-17", "2026-03-18", "2026-03-19")
            )
        )
    }
}
