package com.phoneuse.mzillow.data

data class Property(
    val id: Int,
    val address: String,
    val neighborhood: String,
    val city: String,
    val state: String,
    val price: Long,
    val bedrooms: Int,
    val bathrooms: Int,
    val sqft: Int,
    val propertyType: String,
    val listingAgent: String,
    val description: String,
    val availableViewingSlots: List<String>
)
