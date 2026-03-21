package com.phoneuse.mbooking.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.phoneuse.mbooking.data.DataLoader
import com.phoneuse.mbooking.data.Hotel

sealed class Screen {
    object Home : Screen()
    object MyReservations : Screen()
    data class HotelList(val query: String, val neighborhood: String? = null) : Screen()
    data class HotelDetail(val hotel: Hotel) : Screen()
    data class BookingForm(val hotel: Hotel, val checkInDate: String) : Screen()
    data class Confirmation(val hotel: Hotel, val checkInDate: String) : Screen()
}

@Composable
fun MBookingApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var hotels by remember { mutableStateOf<List<Hotel>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            hotels = DataLoader.loadAllFromJson(context)
        } catch (e: Exception) {
            e.printStackTrace()
            hotels = DataLoader.getDefaultHotels()
        }
    }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                hotels = hotels,
                onSearch = { query, neighborhood ->
                    currentScreen = Screen.HotelList(query, neighborhood)
                },
                onHotelClick = { hotel ->
                    currentScreen = Screen.HotelDetail(hotel)
                },
                onMyReservationsClick = {
                    currentScreen = Screen.MyReservations
                }
            )
        }
        is Screen.MyReservations -> {
            MyReservationsScreen(
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.HotelList -> {
            HotelListScreen(
                hotels = hotels,
                query = screen.query,
                neighborhood = screen.neighborhood,
                onHotelClick = { hotel ->
                    currentScreen = Screen.HotelDetail(hotel)
                },
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.HotelDetail -> {
            HotelDetailScreen(
                hotel = screen.hotel,
                onBookHotel = { checkInDate ->
                    currentScreen = Screen.BookingForm(screen.hotel, checkInDate)
                },
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.BookingForm -> {
            BookingFormScreen(
                hotel = screen.hotel,
                checkInDate = screen.checkInDate,
                onConfirm = {
                    currentScreen = Screen.Confirmation(screen.hotel, screen.checkInDate)
                },
                onBack = {
                    currentScreen = Screen.HotelDetail(screen.hotel)
                }
            )
        }
        is Screen.Confirmation -> {
            ConfirmationScreen(
                hotel = screen.hotel,
                checkInDate = screen.checkInDate,
                onDone = {
                    currentScreen = Screen.Home
                },
                onViewReservations = {
                    currentScreen = Screen.MyReservations
                }
            )
        }
    }
}
