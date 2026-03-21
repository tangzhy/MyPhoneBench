package com.phoneuse.mopentable.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.phoneuse.mopentable.data.DataLoader
import com.phoneuse.mopentable.data.Restaurant

sealed class Screen {
    object Home : Screen()
    data class RestaurantList(
        val query: String,
        val cuisine: String?,
        val partySize: Int
    ) : Screen()
    data class RestaurantDetail(val restaurant: Restaurant) : Screen()
    data class ReservationForm(
        val restaurant: Restaurant,
        val reservationTime: String,
        val partySize: Int
    ) : Screen()
    data class Confirmation(
        val restaurant: Restaurant,
        val reservationTime: String,
        val partySize: Int
    ) : Screen()
    object MyReservations : Screen()
}

@Composable
fun MOpenTableApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var restaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            restaurants = DataLoader.loadRestaurantsFromJson(context)
        } catch (e: Exception) {
            e.printStackTrace()
            restaurants = DataLoader.getDefaultRestaurants()
        }
    }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                restaurants = restaurants,
                onSearch = { query, cuisine, partySize ->
                    currentScreen = Screen.RestaurantList(query, cuisine, partySize)
                },
                onRestaurantClick = { restaurant ->
                    currentScreen = Screen.RestaurantDetail(restaurant)
                },
                onMyReservationsClick = {
                    currentScreen = Screen.MyReservations
                }
            )
        }
        is Screen.RestaurantList -> {
            RestaurantListScreen(
                restaurants = restaurants,
                query = screen.query,
                cuisine = screen.cuisine,
                partySize = screen.partySize,
                onRestaurantClick = { restaurant ->
                    currentScreen = Screen.RestaurantDetail(restaurant)
                },
                onBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.RestaurantDetail -> {
            RestaurantDetailScreen(
                restaurant = screen.restaurant,
                onSelectSlot = { time, partySize ->
                    currentScreen = Screen.ReservationForm(
                        screen.restaurant,
                        time,
                        partySize
                    )
                },
                onBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.ReservationForm -> {
            ReservationFormScreen(
                restaurant = screen.restaurant,
                reservationTime = screen.reservationTime,
                partySize = screen.partySize,
                onConfirm = {
                    currentScreen = Screen.Confirmation(
                        screen.restaurant,
                        screen.reservationTime,
                        screen.partySize
                    )
                },
                onBack = {
                    currentScreen = Screen.RestaurantDetail(screen.restaurant)
                }
            )
        }
        is Screen.Confirmation -> {
            ConfirmationScreen(
                restaurant = screen.restaurant,
                reservationTime = screen.reservationTime,
                partySize = screen.partySize,
                onDone = { currentScreen = Screen.Home },
                onViewReservations = { currentScreen = Screen.MyReservations }
            )
        }
        is Screen.MyReservations -> {
            MyReservationsScreen(
                restaurants = restaurants,
                onBack = { currentScreen = Screen.Home }
            )
        }
    }
}
