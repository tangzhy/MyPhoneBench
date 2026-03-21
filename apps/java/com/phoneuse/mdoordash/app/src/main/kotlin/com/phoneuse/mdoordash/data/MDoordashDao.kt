package com.phoneuse.mdoordash.data

interface MDoordashDao {
    fun getAllRestaurants(): List<RestaurantDemo> = emptyList()
    fun getAllOrders(): List<Order> = emptyList()
}
