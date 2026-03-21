package com.phoneuse.mthumbtack.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.phoneuse.mthumbtack.data.DataLoader
import com.phoneuse.mthumbtack.data.ProDemo

sealed class Screen {
    object Home : Screen()
    object MyRequests : Screen()
    data class ProList(val query: String, val serviceType: String? = null) : Screen()
    data class ProDetail(val pro: ProDemo) : Screen()
    data class ServiceRequestForm(val pro: ProDemo, val appointmentTime: String) : Screen()
    data class Confirmation(val pro: ProDemo, val appointmentTime: String) : Screen()
}

@Composable
fun MThumbtackApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var pros by remember { mutableStateOf<List<ProDemo>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            pros = DataLoader.loadAllFromJson(context)
        } catch (e: Exception) {
            e.printStackTrace()
            pros = DataLoader.getDefaultPros()
        }
    }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                pros = pros,
                onSearch = { query, serviceType ->
                    currentScreen = Screen.ProList(query, serviceType)
                },
                onProClick = { pro ->
                    currentScreen = Screen.ProDetail(pro)
                },
                onMyRequestsClick = {
                    currentScreen = Screen.MyRequests
                }
            )
        }
        is Screen.MyRequests -> {
            MyRequestsScreen(
                onBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.ProList -> {
            ProListScreen(
                pros = pros,
                query = screen.query,
                serviceType = screen.serviceType,
                onProClick = { pro ->
                    currentScreen = Screen.ProDetail(pro)
                },
                onBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.ProDetail -> {
            ProDetailScreen(
                pro = screen.pro,
                onRequestService = { appointmentTime ->
                    currentScreen = Screen.ServiceRequestForm(screen.pro, appointmentTime)
                },
                onBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.ServiceRequestForm -> {
            ServiceRequestFormScreen(
                pro = screen.pro,
                appointmentTime = screen.appointmentTime,
                onConfirm = {
                    currentScreen = Screen.Confirmation(screen.pro, screen.appointmentTime)
                },
                onBack = {
                    currentScreen = Screen.ProDetail(screen.pro)
                }
            )
        }
        is Screen.Confirmation -> {
            ConfirmationScreen(
                pro = screen.pro,
                appointmentTime = screen.appointmentTime,
                onDone = { currentScreen = Screen.Home },
                onViewRequests = { currentScreen = Screen.MyRequests }
            )
        }
    }
}
