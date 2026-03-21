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

package com.phoneuse.imy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phoneuse.imy.data.ProfileDatabase

@Composable
fun IMyApp(database: ProfileDatabase) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Privacy) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == Screen.Privacy,
                    onClick = { currentScreen = Screen.Privacy },
                    icon = { Text("隐私") },
                    label = { Text("隐私管理") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.Chatbot,
                    onClick = { currentScreen = Screen.Chatbot },
                    icon = { Text("对话") },
                    label = { Text("Chatbot") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                Screen.Privacy -> PrivacyScreen(database = database)
                Screen.Chatbot -> ChatbotScreen()
            }
        }
    }
}

enum class Screen {
    Privacy,
    Chatbot
}

