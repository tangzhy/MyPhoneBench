package com.phoneuse.mthumbtack.data

data class Pro(
    val id: Int,
    val name: String,
    val serviceType: String,
    val company: String,
    val city: String,
    val rating: Double = 4.5,
    val availableSlots: String  // JSON array string
)

data class ServiceRequest(
    val id: Int = 0,
    val proId: Int,
    val customerName: String,
    val customerPhone: String? = null,
    val customerDob: String? = null,
    val customerEmail: String? = null,
    val customerAddress: String? = null,
    val customerOccupation: String? = null,
    val insuranceProvider: String? = null,
    val insuranceId: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val projectDescription: String,
    val appointmentTime: String,
    val status: String = "confirmed",
    val createdAt: String = ""
)

data class ProDemo(
    val id: Int,
    val name: String,
    val serviceType: String,
    val company: String,
    val city: String,
    val rating: Double = 4.5,
    val availableSlots: List<String>
)
