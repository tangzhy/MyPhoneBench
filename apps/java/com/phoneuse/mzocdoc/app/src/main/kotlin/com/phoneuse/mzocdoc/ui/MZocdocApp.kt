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

package com.phoneuse.mzocdoc.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.phoneuse.mzocdoc.data.DataLoader
import com.phoneuse.mzocdoc.data.DoctorDemo

sealed class Screen {
    object Home : Screen()
    object MyAppointments : Screen()
    data class DoctorList(val query: String, val specialty: String? = null) : Screen()
    data class DoctorDetail(val doctor: DoctorDemo) : Screen()
    data class BookingForm(val doctor: DoctorDemo, val appointmentTime: String) : Screen()
    data class Confirmation(val doctor: DoctorDemo, val appointmentTime: String) : Screen()
}

@Composable
fun MZocdocApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var doctors by remember { mutableStateOf<List<DoctorDemo>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        try {
            doctors = DataLoader.loadAllFromJson(context)
        } catch (e: Exception) {
            e.printStackTrace()
            // Use default doctors if loading fails
            doctors = DataLoader.getDefaultDoctors()
        }
    }
    
    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                doctors = doctors,
                onSearch = { query, specialty ->
                    currentScreen = Screen.DoctorList(query, specialty)
                },
                onDoctorClick = { doctor ->
                    currentScreen = Screen.DoctorDetail(doctor)
                },
                onMyAppointmentsClick = {
                    currentScreen = Screen.MyAppointments
                }
            )
        }
        is Screen.MyAppointments -> {
            MyAppointmentsScreen(
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.DoctorList -> {
            DoctorListScreen(
                doctors = doctors,
                query = screen.query,
                specialty = screen.specialty,
                onDoctorClick = { doctor ->
                    currentScreen = Screen.DoctorDetail(doctor)
                },
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.DoctorDetail -> {
            DoctorDetailScreen(
                doctor = screen.doctor,
                onBookAppointment = { appointmentTime ->
                    currentScreen = Screen.BookingForm(screen.doctor, appointmentTime)
                },
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.BookingForm -> {
            BookingFormScreen(
                doctor = screen.doctor,
                appointmentTime = screen.appointmentTime,
                onConfirm = {
                    currentScreen = Screen.Confirmation(screen.doctor, screen.appointmentTime)
                },
                onBack = {
                    currentScreen = Screen.DoctorDetail(screen.doctor)
                }
            )
        }
        is Screen.Confirmation -> {
            ConfirmationScreen(
                doctor = screen.doctor,
                appointmentTime = screen.appointmentTime,
                onDone = {
                    currentScreen = Screen.Home
                },
                onViewAppointments = {
                    currentScreen = Screen.MyAppointments
                }
            )
        }
    }
}

