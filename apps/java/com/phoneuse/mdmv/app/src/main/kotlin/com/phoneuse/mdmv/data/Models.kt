package com.phoneuse.mdmv.data

data class DmvOffice(
    val id: Int,
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String = "",
    val phone: String = "",
    val servicesOffered: List<String> = emptyList(),
    val hours: String = "",
    val waitTimeMinutes: Int = 30
)
