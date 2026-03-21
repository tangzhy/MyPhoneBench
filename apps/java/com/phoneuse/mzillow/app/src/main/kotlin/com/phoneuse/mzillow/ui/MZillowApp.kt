package com.phoneuse.mzillow.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.phoneuse.mzillow.data.DataLoader
import com.phoneuse.mzillow.data.Property

sealed class Screen {
    object Home : Screen()
    object MyViewings : Screen()
    data class PropertyDetail(val property: Property) : Screen()
    data class ViewingRequestForm(
        val property: Property,
        val viewingTime: String,
    ) : Screen()
    data class Confirmation(
        val property: Property,
        val viewingTime: String,
    ) : Screen()
}

@Composable
fun MZillowApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var properties by remember { mutableStateOf<List<Property>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            properties = DataLoader.loadPropertiesFromJson(context)
        } catch (e: Exception) {
            e.printStackTrace()
            properties = DataLoader.getDefaultProperties()
        }
    }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                properties = properties,
                onPropertyClick = { property ->
                    currentScreen = Screen.PropertyDetail(property)
                },
                onMyViewingsClick = { currentScreen = Screen.MyViewings },
            )
        }
        is Screen.MyViewings -> {
            MyViewingsScreen(onBack = { currentScreen = Screen.Home })
        }
        is Screen.PropertyDetail -> {
            PropertyDetailScreen(
                property = screen.property,
                onSelectSlot = { time ->
                    currentScreen = Screen.ViewingRequestForm(screen.property, time)
                },
                onBack = { currentScreen = Screen.Home },
            )
        }
        is Screen.ViewingRequestForm -> {
            ViewingRequestFormScreen(
                property = screen.property,
                viewingTime = screen.viewingTime,
                onConfirm = {
                    currentScreen = Screen.Confirmation(screen.property, screen.viewingTime)
                },
                onBack = {
                    currentScreen = Screen.PropertyDetail(screen.property)
                },
            )
        }
        is Screen.Confirmation -> {
            ConfirmationScreen(
                property = screen.property,
                viewingTime = screen.viewingTime,
                onDone = { currentScreen = Screen.Home },
                onViewViewings = { currentScreen = Screen.MyViewings },
            )
        }
    }
}
