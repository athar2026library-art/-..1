package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.AzkarScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.AiServicesScreen

@Composable
fun AppNavGraph(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = "home"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToAzkar = { category ->
                    navController.navigate("azkar/$category")
                },
                onNavigateToStats = {
                    navController.navigate("stats")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToAiServices = {
                    navController.navigate("ai_services")
                }
            )
        }
        composable("azkar/{category}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "sabah"
            AzkarScreen(
                category = category,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("stats") {
            StatsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("ai_services") {
            AiServicesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
