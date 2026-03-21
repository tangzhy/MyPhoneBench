package com.phoneuse.mcvspharmacy.data

data class Store(
    val id: Int,
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val distanceMiles: Double,
    val openHours: String,
    val services: List<String>
)

data class CareService(
    val id: Int,
    val name: String,
    val type: String,
    val durationMin: Int,
    val basePrice: Double,
    val availableOnlineBooking: Boolean
)

data class StoreWithSlots(
    val store: Store,
    val availableSlots: List<String>
)
