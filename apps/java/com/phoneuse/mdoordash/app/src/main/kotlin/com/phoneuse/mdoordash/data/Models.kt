package com.phoneuse.mdoordash.data

/**
 * Menu item within a restaurant.
 */
data class MenuItem(
    val name: String,
    val price: Double,
    val category: String = ""
)

/**
 * Restaurant data class for JSON loading.
 */
data class RestaurantDemo(
    val id: Int,
    val name: String,
    val cuisine: String,
    val neighborhood: String,
    val city: String,
    val rating: Double = 4.5,
    val deliveryTimeMin: Int = 30,
    val menuItems: List<MenuItem> = emptyList(),
    val availableSlots: List<String> = emptyList()
)

/**
 * Order data class matching the database schema.
 */
data class Order(
    val id: Int = 0,
    val restaurantId: Int,
    val customerName: String,
    val customerPhone: String? = null,
    val customerDob: String? = null,
    val deliveryAddress: String? = null,
    val occupation: String? = null,
    val customerEmail: String? = null,
    val mealPlanProvider: String? = null,
    val mealPlanId: String? = null,
    val alternateContactName: String? = null,
    val alternateContactPhone: String? = null,
    val specialInstructions: String,
    val orderItems: String? = null,
    val orderTotal: Double? = null,
    val deliveryTime: String = "",
    val status: String = "confirmed",
    val createdAt: String = ""
)
