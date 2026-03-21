package com.phoneuse.mopentable.data

data class Restaurant(
    val id: Int,
    val name: String,
    val cuisine: String,
    val neighborhood: String,
    val city: String,
    val rating: Double,
    val priceLevel: Int,    // 1-4 ($, $$, $$$, $$$$)
    val hours: String,
    val availableSlots: List<String>
)
