package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.AzkarScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.AiServicesScreen

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "الأذكار", Icons.Default.List)
    object AiServices : Screen("ai_services", "المساعد", Icons.Outlined.Chat)
    object Stats : Screen("stats", "الإحصائيات", Icons.Default.Star)
    object Settings : Screen("settings", "الإعدادات", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.AiServices,
    Screen.Stats,
    Screen.Settings
)

@Composable
fun AppNavGraph(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = "home"
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Show BottomBar only on top-level routes
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAzkar = { category ->
                        navController.navigate("azkar/$category")
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
                    viewModel = viewModel
                )
            }
            
            composable("ai_services") {
                AiServicesScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
