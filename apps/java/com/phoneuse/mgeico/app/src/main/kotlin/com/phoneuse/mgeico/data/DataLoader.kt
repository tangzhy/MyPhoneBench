package com.phoneuse.mgeico.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

object DataLoader {

    private const val INTERNAL_JSON_PATH = "/data/data/com.phoneuse.mgeico/files/mgeico_data.json"
    private const val DEFAULT_JSON_PATH = "/sdcard/mgeico_data.json"
    private const val FALLBACK_JSON_PATH = "/storage/emulated/0/mgeico_data.json"

    fun loadPlansFromJson(context: Context): List<InsurancePlan> {
        return try {
            var file = File(INTERNAL_JSON_PATH)
            if (!file.exists()) file = File(DEFAULT_JSON_PATH)
            if (!file.exists()) file = File(FALLBACK_JSON_PATH)
            if (!file.exists()) {
                val externalFile = File(context.getExternalFilesDir(null), "mgeico_data.json")
                if (externalFile.exists()) file = externalFile
            }
            if (!file.exists()) return getDefaultPlans()

            val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val plansArray = jsonObject.getJSONArray("plans")

            val plans = mutableListOf<InsurancePlan>()
            for (i in 0 until plansArray.length()) {
                val obj = plansArray.getJSONObject(i)
                plans.add(
                    InsurancePlan(
                        id = obj.getInt("id"),
                        planName = obj.getString("plan_name"),
                        coverageType = obj.getString("coverage_type"),
                        deductible = obj.getInt("deductible"),
                        monthlyPremium = obj.getDouble("monthly_premium"),
                        annualPremium = obj.getDouble("annual_premium"),
                        coverageLimit = obj.getString("coverage_limit"),
                        includesCollision = obj.getBoolean("includes_collision"),
                        includesComprehensive = obj.getBoolean("includes_comprehensive"),
                        provider = obj.getString("provider"),
                        rating = if (obj.has("rating")) obj.getDouble("rating") else 4.5
                    )
                )
            }
            plans
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultPlans()
        }
    }

    fun loadAllFromJson(context: Context): List<InsurancePlan> {
        return loadPlansFromJson(context)
    }

    fun getDefaultPlans(): List<InsurancePlan> {
        return listOf(
            InsurancePlan(
                id = 1, planName = "Basic Liability", coverageType = "Liability Only",
                deductible = 500, monthlyPremium = 65.0, annualPremium = 780.0,
                coverageLimit = "25/50/25", includesCollision = false,
                includesComprehensive = false, provider = "GEICO", rating = 4.2
            ),
            InsurancePlan(
                id = 2, planName = "Standard Coverage", coverageType = "Standard",
                deductible = 500, monthlyPremium = 120.0, annualPremium = 1440.0,
                coverageLimit = "50/100/50", includesCollision = true,
                includesComprehensive = false, provider = "GEICO", rating = 4.5
            ),
            InsurancePlan(
                id = 3, planName = "Premium Full Coverage", coverageType = "Comprehensive",
                deductible = 250, monthlyPremium = 195.0, annualPremium = 2340.0,
                coverageLimit = "100/300/100", includesCollision = true,
                includesComprehensive = true, provider = "GEICO", rating = 4.8
            )
        )
    }
}
