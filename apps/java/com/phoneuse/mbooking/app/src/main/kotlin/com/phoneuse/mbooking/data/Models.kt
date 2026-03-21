package com.phoneuse.mbooking.data

data class Hotel(
    val id: Int,
    val name: String,
    val neighborhood: String,
    val city: String,
    val starRating: Int,
    val pricePerNight: Int,
    val amenities: List<String>,
    val roomTypes: List<String>,
    val availableDates: List<String>
)

data class Reservation(
    val id: Int = 0,
    val hotelId: Int,
    val guestName: String,
    val guestPhone: String? = null,
    val guestDob: String? = null,
    val guestEmail: String? = null,
    val guestGender: String? = null,
    val loyaltyProgram: String? = null,
    val loyaltyId: String? = null,
    val passportNumber: String? = null,
    val checkInDate: String,
    val checkOutDate: String,
    val roomType: String? = null,
    val specialRequests: String? = null,
    val status: String = "confirmed",
    val createdAt: String = ""
)
