package com.cryptotracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cryptotracker.ui.detail.DetailScreen
import com.cryptotracker.ui.favorites.FavoritesScreen
import com.cryptotracker.ui.home.HomeScreen
import com.cryptotracker.ui.search.SearchScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector?) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object Favorites : Screen("favorites", "Favorites", Icons.Default.Star)
    data object Detail : Screen("detail/{coinId}", "Detail", null) {
        fun createRoute(coinId: String) = "detail/$coinId"
    }
}

private val bottomNavItems = listOf(Screen.Home, Screen.Search, Screen.Favorites)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                screen.icon?.let { Icon(it, contentDescription = screen.label) }
                            },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onCoinClick = { coinId ->
                        navController.navigate(Screen.Detail.createRoute(coinId))
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onCoinClick = { coinId ->
                        navController.navigate(Screen.Detail.createRoute(coinId))
                    }
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onCoinClick = { coinId ->
                        navController.navigate(Screen.Detail.createRoute(coinId))
                    }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("coinId") { type = NavType.StringType }
                )
            ) {
                DetailScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
