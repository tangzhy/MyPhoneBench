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

package com.phoneuse.meventbrite.data

/**
 * Event data class for JSON loading (before Room integration).
 */
data class EventDemo(
    val id: Int,
    val title: String,
    val category: String,
    val venue: String,
    val city: String,
    val date: String,       // "2026-03-15"
    val time: String,       // "09:00"
    val price: Double = 0.0, // 0 = free
    val organizer: String,
    val description: String,
    val availableTickets: Int = 100
)

/**
 * Registration data class (Room annotations removed for now).
 */
data class Registration(
    val id: Int = 0,
    val eventId: Int,
    val attendeeName: String,
    val attendeePhone: String? = null,
    val attendeeDob: String? = null,
    val attendeeGender: String? = null,
    val bloodType: String? = null,
    val attendeeEmail: String? = null,
    val homeAddress: String? = null,
    val numTickets: Int = 1,
    val occupation: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val reasonForAttending: String = "",
    val smsNotificationPhone: String? = null,
    val recommendationEmail: String? = null,
    val status: String = "confirmed",
    val createdAt: String = ""
)
