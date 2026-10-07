package com.example.ui

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.ui.components.BaqiyatNavItem
import com.example.ui.components.FloatingNavBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AiServicesScreen
import com.example.ui.screens.AzkarScreen
import com.example.ui.screens.FeedbackScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen

sealed class Screen(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : Screen("home", "الأذكار", Icons.AutoMirrored.Filled.List)
    object AiServices : Screen("ai_services", "المساعد", Icons.AutoMirrored.Outlined.Chat)
    /** أيقونة مختلفة عن المساعد لتجنب الالتباس في شريط التنقل. */
    object Feedback : Screen("feedback", "تواصل معنا", Icons.Default.Email)
    object Stats : Screen("stats", "الإحصائيات", Icons.Default.Star)
    object Settings : Screen("settings", "الإعدادات", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.AiServices,
    Screen.Feedback,
    Screen.Stats,
    Screen.Settings
)

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AppNavGraph(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = "home"
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                val navItems = remember { bottomNavItems.map { BaqiyatNavItem(it.title, it.icon) } }
                val selectedIndex = bottomNavItems.indexOfFirst { screen ->
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                }.coerceAtLeast(0)
                FloatingNavBar(
                    items = navItems,
                    selectedIndex = selectedIndex,
                    onSelect = { index ->
                        navController.navigate(bottomNavItems[index].route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(280)) + slideInHorizontally(tween(280, easing = EaseOutCubic)) { it / 10 } },
            exitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(280, easing = EaseOutCubic)) { -it / 10 } },
            popEnterTransition = { fadeIn(tween(280)) },
            popExitTransition = { fadeOut(tween(200)) }
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAzkar = { category ->
                        navController.navigate("azkar/$category")
                    },
                    onNavigateToSearch = { navController.navigate("search") }
                )
            }

            composable("search") {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenCategory = { category ->
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

            composable("feedback") { FeedbackScreen(viewModel = viewModel) }

            composable("stats") {
                StatsScreen(viewModel = viewModel)
            }

            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }

            composable("ai_services") {
                AiServicesScreen(viewModel = viewModel)
            }
        }
    }
}
