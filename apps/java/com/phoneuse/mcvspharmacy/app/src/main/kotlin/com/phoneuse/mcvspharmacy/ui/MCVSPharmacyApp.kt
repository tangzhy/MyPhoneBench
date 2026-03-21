package com.phoneuse.mcvspharmacy.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.phoneuse.mcvspharmacy.data.CareService
import com.phoneuse.mcvspharmacy.data.DataLoader
import com.phoneuse.mcvspharmacy.data.Store

sealed class Screen {
    object Home : Screen()
    object MyBookings : Screen()
    data class ServiceList(val query: String, val serviceType: String? = null) : Screen()
    data class StoreSelection(val service: CareService) : Screen()
    data class BookingForm(
        val service: CareService,
        val store: Store,
        val bookingTime: String
    ) : Screen()
    data class Confirmation(
        val service: CareService,
        val store: Store,
        val bookingTime: String
    ) : Screen()
}

@Composable
fun MCVSPharmacyApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var careServices by remember { mutableStateOf<List<CareService>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            stores = DataLoader.loadStoresFromJson(context)
            careServices = DataLoader.loadCareServicesFromJson(context)
        } catch (e: Exception) {
            e.printStackTrace()
            stores = DataLoader.getDefaultStores()
            careServices = DataLoader.getDefaultCareServices()
        }
    }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                careServices = careServices,
                onSearch = { query, serviceType ->
                    currentScreen = Screen.ServiceList(query, serviceType)
                },
                onServiceClick = { service ->
                    currentScreen = Screen.StoreSelection(service)
                },
                onMyBookingsClick = {
                    currentScreen = Screen.MyBookings
                }
            )
        }
        is Screen.MyBookings -> {
            MyBookingsScreen(onBack = { currentScreen = Screen.Home })
        }
        is Screen.ServiceList -> {
            ServiceListScreen(
                careServices = careServices,
                query = screen.query,
                serviceType = screen.serviceType,
                onServiceClick = { service ->
                    currentScreen = Screen.StoreSelection(service)
                },
                onBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.StoreSelection -> {
            StoreSelectionScreen(
                service = screen.service,
                stores = stores,
                onSelectSlot = { store, time ->
                    currentScreen = Screen.BookingForm(screen.service, store, time)
                },
                onBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.BookingForm -> {
            BookingFormScreen(
                service = screen.service,
                store = screen.store,
                bookingTime = screen.bookingTime,
                onConfirm = {
                    currentScreen = Screen.Confirmation(
                        screen.service, screen.store, screen.bookingTime
                    )
                },
                onBack = {
                    currentScreen = Screen.StoreSelection(screen.service)
                }
            )
        }
        is Screen.Confirmation -> {
            ConfirmationScreen(
                service = screen.service,
                store = screen.store,
                bookingTime = screen.bookingTime,
                onDone = { currentScreen = Screen.Home },
                onViewBookings = { currentScreen = Screen.MyBookings }
            )
        }
    }
}
