package com.phoneuse.mzillow.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

object DataLoader {

    private const val INTERNAL_JSON_PATH =
        "/data/data/com.phoneuse.mzillow/files/mzillow_data.json"
    private const val DEFAULT_JSON_PATH = "/sdcard/mzillow_data.json"
    private const val FALLBACK_JSON_PATH = "/storage/emulated/0/mzillow_data.json"

    fun loadPropertiesFromJson(
        context: Context,
        jsonFilePath: String = DEFAULT_JSON_PATH,
    ): List<Property> {
        return try {
            var file = File(INTERNAL_JSON_PATH)
            if (!file.exists()) file = File(jsonFilePath)
            if (!file.exists()) file = File(FALLBACK_JSON_PATH)
            if (!file.exists()) {
                val ext = File(context.getExternalFilesDir(null), "mzillow_data.json")
                if (ext.exists()) file = ext
            }
            if (!file.exists()) return getDefaultProperties()

            val json = FileInputStream(file).bufferedReader().use { it.readText() }
            val arr = JSONObject(json).getJSONArray("properties")
            val list = mutableListOf<Property>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val slots = mutableListOf<String>()
                val sa = o.getJSONArray("available_viewing_slots")
                for (j in 0 until sa.length()) slots.add(sa.getString(j))
                list.add(
                    Property(
                        id = o.getInt("id"),
                        address = o.getString("address"),
                        neighborhood = o.getString("neighborhood"),
                        city = o.getString("city"),
                        state = o.getString("state"),
                        price = o.getLong("price"),
                        bedrooms = o.getInt("bedrooms"),
                        bathrooms = o.getInt("bathrooms"),
                        sqft = o.getInt("sqft"),
                        propertyType = o.getString("property_type"),
                        listingAgent = o.getString("listing_agent"),
                        description = o.optString("description", ""),
                        availableViewingSlots = slots,
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultProperties()
        }
    }

    fun getDefaultProperties(): List<Property> = listOf(
        Property(
            id = 1,
            address = "350 E 72nd St, Apt 12B",
            neighborhood = "Upper East Side",
            city = "New York",
            state = "NY",
            price = 850000,
            bedrooms = 2,
            bathrooms = 1,
            sqft = 950,
            propertyType = "Condo",
            listingAgent = "Smith Realty",
            description = "Bright 2BR condo with park views",
            availableViewingSlots = listOf(
                "2026-03-15 10:00", "2026-03-15 14:00", "2026-03-16 11:00"
            ),
        ),
        Property(
            id = 2,
            address = "100 Montague St, Unit 5A",
            neighborhood = "Brooklyn Heights",
            city = "New York",
            state = "NY",
            price = 625000,
            bedrooms = 1,
            bathrooms = 1,
            sqft = 720,
            propertyType = "Apartment",
            listingAgent = "Brooklyn Living Group",
            description = "Charming 1BR near the promenade",
            availableViewingSlots = listOf(
                "2026-03-15 09:00", "2026-03-16 10:00", "2026-03-16 15:00"
            ),
        ),
        Property(
            id = 3,
            address = "55 W 25th St, PH",
            neighborhood = "Chelsea",
            city = "New York",
            state = "NY",
            price = 1200000,
            bedrooms = 3,
            bathrooms = 2,
            sqft = 1400,
            propertyType = "Penthouse",
            listingAgent = "Luxury NYC Homes",
            description = "Stunning penthouse with skyline views",
            availableViewingSlots = listOf(
                "2026-03-17 11:00", "2026-03-17 14:00", "2026-03-18 10:00"
            ),
        ),
    )
}
