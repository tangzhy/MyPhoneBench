package com.phoneuse.mdoordash.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.phoneuse.mdoordash.data.DataLoader
import com.phoneuse.mdoordash.data.RestaurantDemo

sealed class Screen {
    object Home : Screen()
    object MyOrders : Screen()
    data class RestaurantList(val query: String, val cuisine: String? = null) : Screen()
    data class RestaurantDetail(val restaurant: RestaurantDemo) : Screen()
    data class OrderForm(val restaurant: RestaurantDemo, val deliveryTime: String, val selectedItems: List<String>) : Screen()
    data class Confirmation(val restaurant: RestaurantDemo, val deliveryTime: String) : Screen()
}

@Composable
fun MDoordashApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var restaurants by remember { mutableStateOf<List<RestaurantDemo>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            restaurants = DataLoader.loadAllFromJson(context)
        } catch (e: Exception) {
            e.printStackTrace()
            restaurants = DataLoader.getDefaultRestaurants()
        }
    }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                restaurants = restaurants,
                onSearch = { query, cuisine ->
                    currentScreen = Screen.RestaurantList(query, cuisine)
                },
                onRestaurantClick = { restaurant ->
                    currentScreen = Screen.RestaurantDetail(restaurant)
                },
                onMyOrdersClick = {
                    currentScreen = Screen.MyOrders
                }
            )
        }
        is Screen.MyOrders -> {
            MyOrdersScreen(
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.RestaurantList -> {
            RestaurantListScreen(
                restaurants = restaurants,
                query = screen.query,
                cuisine = screen.cuisine,
                onRestaurantClick = { restaurant ->
                    currentScreen = Screen.RestaurantDetail(restaurant)
                },
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.RestaurantDetail -> {
            RestaurantDetailScreen(
                restaurant = screen.restaurant,
                onPlaceOrder = { deliveryTime, selectedItems ->
                    currentScreen = Screen.OrderForm(screen.restaurant, deliveryTime, selectedItems)
                },
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.OrderForm -> {
            OrderFormScreen(
                restaurant = screen.restaurant,
                deliveryTime = screen.deliveryTime,
                selectedItems = screen.selectedItems,
                onConfirm = {
                    currentScreen = Screen.Confirmation(screen.restaurant, screen.deliveryTime)
                },
                onBack = {
                    currentScreen = Screen.RestaurantDetail(screen.restaurant)
                }
            )
        }
        is Screen.Confirmation -> {
            ConfirmationScreen(
                restaurant = screen.restaurant,
                deliveryTime = screen.deliveryTime,
                onDone = {
                    currentScreen = Screen.Home
                },
                onViewOrders = {
                    currentScreen = Screen.MyOrders
                }
            )
        }
    }
}
