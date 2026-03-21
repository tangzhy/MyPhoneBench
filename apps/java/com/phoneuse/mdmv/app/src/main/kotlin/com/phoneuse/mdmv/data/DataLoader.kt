package com.phoneuse.mdmv.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

object DataLoader {

    private const val INTERNAL_JSON_PATH = "/data/data/com.phoneuse.mdmv/files/mdmv_data.json"
    private const val DEFAULT_JSON_PATH = "/sdcard/mdmv_data.json"
    private const val FALLBACK_JSON_PATH = "/storage/emulated/0/mdmv_data.json"

    fun loadOfficesFromJson(context: Context, jsonFilePath: String = DEFAULT_JSON_PATH): List<DmvOffice> {
        return try {
            var file = File(INTERNAL_JSON_PATH)
            if (!file.exists()) {
                file = File(jsonFilePath)
            }
            if (!file.exists()) {
                file = File(FALLBACK_JSON_PATH)
            }
            if (!file.exists()) {
                val externalFile = File(context.getExternalFilesDir(null), "mdmv_data.json")
                if (externalFile.exists()) {
                    file = externalFile
                }
            }
            if (!file.exists()) {
                return getDefaultOffices()
            }

            val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val officesArray = jsonObject.getJSONArray("offices")

            val offices = mutableListOf<DmvOffice>()
            for (i in 0 until officesArray.length()) {
                val obj = officesArray.getJSONObject(i)
                val servicesArray = obj.optJSONArray("services_offered")
                val services = mutableListOf<String>()
                if (servicesArray != null) {
                    for (j in 0 until servicesArray.length()) {
                        services.add(servicesArray.getString(j))
                    }
                }

                offices.add(
                    DmvOffice(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        address = obj.optString("address", ""),
                        city = obj.getString("city"),
                        state = obj.getString("state"),
                        zipCode = obj.optString("zip_code", ""),
                        phone = obj.optString("phone", ""),
                        servicesOffered = services,
                        hours = obj.optString("hours", "Mon-Fri 8:00-16:00"),
                        waitTimeMinutes = obj.optInt("wait_time_minutes", 30)
                    )
                )
            }
            offices
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultOffices()
        }
    }

    fun loadAllFromJson(context: Context, jsonFilePath: String = DEFAULT_JSON_PATH): List<DmvOffice> {
        return loadOfficesFromJson(context, jsonFilePath)
    }

    fun getDefaultOffices(): List<DmvOffice> {
        return listOf(
            DmvOffice(
                id = 1,
                name = "Manhattan DMV Office",
                address = "11 Greenwich St",
                city = "New York",
                state = "NY",
                zipCode = "10004",
                phone = "(212) 555-0100",
                servicesOffered = listOf("License Renewal", "Vehicle Registration", "Title Transfer", "State ID", "Permit Test"),
                hours = "Mon-Fri 8:00-16:00",
                waitTimeMinutes = 45
            ),
            DmvOffice(
                id = 2,
                name = "Brooklyn DMV Office",
                address = "625 Atlantic Ave",
                city = "Brooklyn",
                state = "NY",
                zipCode = "11217",
                phone = "(718) 555-0200",
                servicesOffered = listOf("License Renewal", "Vehicle Registration", "State ID"),
                hours = "Mon-Fri 8:30-16:00",
                waitTimeMinutes = 60
            ),
            DmvOffice(
                id = 3,
                name = "Queens DMV Office",
                address = "168-46 91st Ave",
                city = "Queens",
                state = "NY",
                zipCode = "11432",
                phone = "(718) 555-0300",
                servicesOffered = listOf("License Renewal", "Vehicle Registration", "Title Transfer", "Permit Test"),
                hours = "Mon-Fri 8:00-15:30",
                waitTimeMinutes = 55
            )
        )
    }
}
