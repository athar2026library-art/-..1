package com.example.ui

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.BaqiyatNavItem
import com.example.ui.components.FloatingNavBar
import com.example.ui.screens.AiServicesScreen
import com.example.ui.screens.AzkarScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.FeedbackScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TasbihScreen

sealed class Screen(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : Screen("home", "الرئيسية", Icons.Default.Home)
    object Tasbih : Screen("tasbih", "المسبحة", Icons.Default.AutoAwesome)
    /** المسار القديم "stats" محفوظ حتى لا ينكسر أي رابط داخلي. */
    object Journey : Screen("stats", "رحلتي", Icons.Default.Spa)
    object More : Screen("more", "المزيد", Icons.Default.MoreHoriz)
}

val bottomNavItems = listOf(Screen.Home, Screen.Tasbih, Screen.Journey, Screen.More)

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
    val unreadFeedback by viewModel.unreadFeedbackCount.collectAsStateWithLifecycle()

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    fun openTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                val navItems = remember(unreadFeedback) {
                    bottomNavItems.map { BaqiyatNavItem(it.title, it.icon, badge = it == Screen.More && unreadFeedback > 0) }
                }
                val selectedIndex = bottomNavItems.indexOfFirst { screen ->
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                }.coerceAtLeast(0)
                FloatingNavBar(
                    items = navItems,
                    selectedIndex = selectedIndex,
                    onSelect = { index -> openTab(bottomNavItems[index].route) }
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
                    onNavigateToAzkar = { category -> navController.navigate("azkar/$category") },
                    onNavigateToSearch = { navController.navigate("search") },
                    onNavigateToTasbih = { openTab("tasbih") },
                    onNavigateToAssistant = { navController.navigate("ai_services") },
                    onNavigateToJourney = { openTab("stats") },
                    onNavigateToFavorites = { navController.navigate("favorites") }
                )
            }

            composable("tasbih") { TasbihScreen(viewModel = viewModel) }

            composable("stats") { StatsScreen(viewModel = viewModel) }

            composable("more") {
                MoreScreen(
                    viewModel = viewModel,
                    onOpenAssistant = { navController.navigate("ai_services") },
                    onOpenFeedback = { navController.navigate("feedback") },
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenFavorites = { navController.navigate("favorites") }
                )
            }

            composable("favorites") {
                FavoritesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onRead = { navController.navigate("azkar/favorites") }
                )
            }

            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenCategory = { category -> navController.navigate("azkar/$category") }
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

            composable("feedback") {
                FeedbackScreen(viewModel = viewModel)
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
