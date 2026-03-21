package com.phoneuse.mdmv.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.phoneuse.mdmv.data.DataLoader
import com.phoneuse.mdmv.data.DmvOffice

sealed class Screen {
    object Home : Screen()
    data class AppointmentForm(val office: DmvOffice) : Screen()
    data class Confirmation(val office: DmvOffice, val serviceName: String) : Screen()
}

@Composable
fun MdmvApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var offices by remember { mutableStateOf<List<DmvOffice>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            offices = DataLoader.loadAllFromJson(context)
        } catch (e: Exception) {
            e.printStackTrace()
            offices = DataLoader.getDefaultOffices()
        }
    }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                offices = offices,
                onOfficeClick = { office ->
                    currentScreen = Screen.AppointmentForm(office)
                }
            )
        }
        is Screen.AppointmentForm -> {
            AppointmentFormScreen(
                office = screen.office,
                onConfirm = { serviceName ->
                    currentScreen = Screen.Confirmation(screen.office, serviceName)
                },
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.Confirmation -> {
            ConfirmationScreen(
                office = screen.office,
                serviceName = screen.serviceName,
                onDone = {
                    currentScreen = Screen.Home
                }
            )
        }
    }
}
