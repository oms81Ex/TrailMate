package com.dev_oms.trailmate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dev_oms.trailmate.ui.screens.*

sealed class PeakPalScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : PeakPalScreen("home", "홈", Icons.Default.Home)
    object Tracking : PeakPalScreen("tracking", "트래킹", Icons.Default.DirectionsRun)
    object Records : PeakPalScreen("records", "기록", Icons.Default.Assessment)
    object Community : PeakPalScreen("community", "커뮤니티", Icons.Default.Group)
    object Settings : PeakPalScreen("settings", "설정", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeakPalApp(
    navController: NavHostController = rememberNavController()
) {
    val bottomBarScreens = listOf(
        PeakPalScreen.Home,
        PeakPalScreen.Tracking,
        PeakPalScreen.Records,
        PeakPalScreen.Community,
        PeakPalScreen.Settings
    )

    Scaffold(
        bottomBar = {
            PeakPalBottomNavigation(
                screens = bottomBarScreens,
                navController = navController
            )
        }
    ) { innerPadding ->
        PeakPalNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun PeakPalBottomNavigation(
    screens: List<PeakPalScreen>,
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        screens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
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

@Composable
fun PeakPalNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = PeakPalScreen.Home.route,
        modifier = modifier
    ) {
        composable(PeakPalScreen.Home.route) {
            HomeScreen(
                onNavigateToTracking = {
                    navController.navigate(PeakPalScreen.Tracking.route)
                }
            )
        }
        composable(PeakPalScreen.Tracking.route) {
            TrackingScreen()
        }
        composable(PeakPalScreen.Records.route) {
            RecordsScreen()
        }
        composable(PeakPalScreen.Community.route) {
            CommunityScreen()
        }
        composable(PeakPalScreen.Settings.route) {
            SettingsScreen()
        }
    }
} 