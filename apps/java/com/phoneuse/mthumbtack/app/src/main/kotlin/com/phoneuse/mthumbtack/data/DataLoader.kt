package com.phoneuse.mthumbtack.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

object DataLoader {

    private const val INTERNAL_JSON_PATH = "/data/data/com.phoneuse.mthumbtack/files/mthumbtack_data.json"
    private const val DEFAULT_JSON_PATH = "/sdcard/mthumbtack_data.json"
    private const val FALLBACK_JSON_PATH = "/storage/emulated/0/mthumbtack_data.json"

    fun loadProsFromJson(context: Context, jsonFilePath: String = DEFAULT_JSON_PATH): List<ProDemo> {
        return try {
            var file = File(INTERNAL_JSON_PATH)
            if (!file.exists()) file = File(jsonFilePath)
            if (!file.exists()) file = File(FALLBACK_JSON_PATH)
            if (!file.exists()) {
                val externalFile = File(context.getExternalFilesDir(null), "mthumbtack_data.json")
                if (externalFile.exists()) file = externalFile
            }
            if (!file.exists()) return getDefaultPros()

            val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val prosArray = jsonObject.getJSONArray("pros")

            val pros = mutableListOf<ProDemo>()
            for (i in 0 until prosArray.length()) {
                val proObj = prosArray.getJSONObject(i)
                val slotsArray = proObj.getJSONArray("available_slots")
                val slots = mutableListOf<String>()
                for (j in 0 until slotsArray.length()) {
                    slots.add(slotsArray.getString(j))
                }
                pros.add(
                    ProDemo(
                        id = proObj.getInt("id"),
                        name = proObj.getString("name"),
                        serviceType = proObj.getString("service_type"),
                        company = proObj.getString("company"),
                        city = proObj.getString("city"),
                        rating = if (proObj.has("rating")) proObj.getDouble("rating") else 4.5,
                        availableSlots = slots
                    )
                )
            }
            pros
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultPros()
        }
    }

    fun loadAllFromJson(context: Context, jsonFilePath: String = DEFAULT_JSON_PATH): List<ProDemo> {
        return loadProsFromJson(context, jsonFilePath)
    }

    fun getDefaultPros(): List<ProDemo> {
        return listOf(
            ProDemo(
                id = 1,
                name = "Mike Johnson",
                serviceType = "Plumbing",
                company = "Quick Fix Plumbing",
                city = "New York",
                rating = 4.9,
                availableSlots = listOf(
                    "2026-03-15 09:00", "2026-03-15 10:00", "2026-03-15 14:00",
                    "2026-03-16 09:00", "2026-03-16 11:00"
                )
            ),
            ProDemo(
                id = 2,
                name = "Sarah Williams",
                serviceType = "House Cleaning",
                company = "Sparkle Clean Services",
                city = "New York",
                rating = 4.7,
                availableSlots = listOf(
                    "2026-03-15 08:00", "2026-03-15 11:00", "2026-03-15 15:00"
                )
            ),
            ProDemo(
                id = 3,
                name = "David Chen",
                serviceType = "Electrical",
                company = "PowerUp Electric",
                city = "New York",
                rating = 4.8,
                availableSlots = listOf(
                    "2026-03-15 13:00", "2026-03-16 08:00", "2026-03-16 13:00"
                )
            )
        )
    }
}
