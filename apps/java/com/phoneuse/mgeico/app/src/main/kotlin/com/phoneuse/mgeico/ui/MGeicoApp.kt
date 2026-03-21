package com.phoneuse.mgeico.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.phoneuse.mgeico.data.DataLoader
import com.phoneuse.mgeico.data.InsurancePlan

sealed class Screen {
    object Home : Screen()
    object MyQuotes : Screen()
    data class PlanList(val query: String, val coverageType: String? = null) : Screen()
    data class PlanDetail(val plan: InsurancePlan) : Screen()
    data class QuoteForm(val plan: InsurancePlan) : Screen()
    data class Confirmation(val plan: InsurancePlan) : Screen()
}

@Composable
fun MGeicoApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var plans by remember { mutableStateOf<List<InsurancePlan>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            plans = DataLoader.loadAllFromJson(context)
        } catch (e: Exception) {
            e.printStackTrace()
            plans = DataLoader.getDefaultPlans()
        }
    }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                plans = plans,
                onSearch = { query, coverageType ->
                    currentScreen = Screen.PlanList(query, coverageType)
                },
                onPlanClick = { plan ->
                    currentScreen = Screen.PlanDetail(plan)
                },
                onMyQuotesClick = {
                    currentScreen = Screen.MyQuotes
                }
            )
        }
        is Screen.MyQuotes -> {
            MyQuotesScreen(
                plans = plans,
                onBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.PlanList -> {
            PlanListScreen(
                plans = plans,
                query = screen.query,
                coverageType = screen.coverageType,
                onPlanClick = { plan ->
                    currentScreen = Screen.PlanDetail(plan)
                },
                onBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.PlanDetail -> {
            PlanDetailScreen(
                plan = screen.plan,
                onGetQuote = {
                    currentScreen = Screen.QuoteForm(screen.plan)
                },
                onBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.QuoteForm -> {
            QuoteFormScreen(
                plan = screen.plan,
                onConfirm = {
                    currentScreen = Screen.Confirmation(screen.plan)
                },
                onBack = { currentScreen = Screen.PlanDetail(screen.plan) }
            )
        }
        is Screen.Confirmation -> {
            ConfirmationScreen(
                plan = screen.plan,
                onDone = { currentScreen = Screen.Home },
                onViewQuotes = { currentScreen = Screen.MyQuotes }
            )
        }
    }
}
