package com.phoneuse.mcvspharmacy.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

object DataLoader {

    private const val INTERNAL_JSON_PATH =
        "/data/data/com.phoneuse.mcvspharmacy/files/mcvspharmacy_data.json"
    private const val DEFAULT_JSON_PATH = "/sdcard/mcvspharmacy_data.json"
    private const val FALLBACK_JSON_PATH = "/storage/emulated/0/mcvspharmacy_data.json"

    fun loadStoresFromJson(context: Context): List<Store> {
        val json = loadJsonObject(context) ?: return getDefaultStores()
        return try {
            val arr = json.getJSONArray("stores")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val svcs = mutableListOf<String>()
                val sa = o.getJSONArray("services")
                for (j in 0 until sa.length()) svcs.add(sa.getString(j))
                Store(
                    id = o.getInt("id"),
                    name = o.getString("name"),
                    address = o.getString("address"),
                    city = o.getString("city"),
                    state = o.getString("state"),
                    zipCode = o.getString("zip_code"),
                    distanceMiles = o.getDouble("distance_miles"),
                    openHours = o.getString("open_hours"),
                    services = svcs
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultStores()
        }
    }

    fun loadCareServicesFromJson(context: Context): List<CareService> {
        val json = loadJsonObject(context) ?: return getDefaultCareServices()
        return try {
            val arr = json.getJSONArray("care_services")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CareService(
                    id = o.getInt("id"),
                    name = o.getString("name"),
                    type = o.getString("type"),
                    durationMin = o.getInt("duration_min"),
                    basePrice = o.getDouble("base_price"),
                    availableOnlineBooking = o.getBoolean("available_online_booking")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultCareServices()
        }
    }

    private fun loadJsonObject(context: Context): JSONObject? {
        return try {
            var file = File(INTERNAL_JSON_PATH)
            if (!file.exists()) file = File(DEFAULT_JSON_PATH)
            if (!file.exists()) file = File(FALLBACK_JSON_PATH)
            if (!file.exists()) {
                val ext = File(context.getExternalFilesDir(null), "mcvspharmacy_data.json")
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

    fun getDefaultStores(): List<Store> = listOf(
        Store(1, "CVS Pharmacy - Brooklyn #1", "509 Queens Blvd, Brooklyn, NY",
            "Brooklyn", "NY", "10502", 3.6, "24 hours",
            listOf("pharmacy", "pickup", "minuteclinic", "vaccination")),
        Store(2, "CVS Pharmacy - Manhattan #2", "1234 Broadway, Manhattan, NY",
            "Manhattan", "NY", "10001", 1.2, "8AM-10PM",
            listOf("pharmacy", "pickup", "minuteclinic")),
        Store(3, "CVS Pharmacy - Hoboken #3", "3711 Bergen Ave, Hoboken, NJ",
            "Hoboken", "NJ", "07030", 4.4, "8AM-9PM",
            listOf("pharmacy", "pickup", "drive-thru"))
    )

    fun getDefaultCareServices(): List<CareService> = listOf(
        CareService(201, "Flu Shot", "vaccination", 15, 0.0, true),
        CareService(202, "COVID-19 Vaccine", "vaccination", 20, 0.0, true),
        CareService(203, "Health Screening", "health_screening", 30, 49.99, true)
    )
}
