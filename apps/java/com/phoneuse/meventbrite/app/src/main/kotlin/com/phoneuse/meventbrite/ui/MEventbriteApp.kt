// Copyright 2025 PhoneUse Privacy Protocol Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.phoneuse.meventbrite.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.phoneuse.meventbrite.data.DataLoader
import com.phoneuse.meventbrite.data.EventDemo

sealed class Screen {
    object Home : Screen()
    object MyRegistrations : Screen()
    data class EventList(val query: String, val category: String? = null) : Screen()
    data class EventDetail(val event: EventDemo) : Screen()
    data class RegistrationForm(val event: EventDemo) : Screen()
    data class Confirmation(val event: EventDemo) : Screen()
}

@Composable
fun MEventbriteApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var events by remember { mutableStateOf<List<EventDemo>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            events = DataLoader.loadAllFromJson(context)
        } catch (e: Exception) {
            e.printStackTrace()
            events = DataLoader.getDefaultEvents()
        }
    }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                events = events,
                onSearch = { query, category ->
                    currentScreen = Screen.EventList(query, category)
                },
                onEventClick = { event ->
                    currentScreen = Screen.EventDetail(event)
                },
                onMyRegistrationsClick = {
                    currentScreen = Screen.MyRegistrations
                }
            )
        }
        is Screen.MyRegistrations -> {
            MyRegistrationsScreen(
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.EventList -> {
            EventListScreen(
                events = events,
                query = screen.query,
                category = screen.category,
                onEventClick = { event ->
                    currentScreen = Screen.EventDetail(event)
                },
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.EventDetail -> {
            EventDetailScreen(
                event = screen.event,
                onRegister = {
                    currentScreen = Screen.RegistrationForm(screen.event)
                },
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.RegistrationForm -> {
            RegistrationFormScreen(
                event = screen.event,
                onConfirm = {
                    currentScreen = Screen.Confirmation(screen.event)
                },
                onBack = {
                    currentScreen = Screen.EventDetail(screen.event)
                }
            )
        }
        is Screen.Confirmation -> {
            ConfirmationScreen(
                event = screen.event,
                onDone = {
                    currentScreen = Screen.Home
                },
                onViewRegistrations = {
                    currentScreen = Screen.MyRegistrations
                }
            )
        }
    }
}
