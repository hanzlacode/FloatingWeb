package com.example.floatingweb

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState

import com.example.floatingweb.screens.AlertManagerScreen
import com.example.floatingweb.screens.FloatingWebScreen
import com.example.floatingweb.screens.LogsScreen

@Composable
fun AppNavHost(navController: NavHostController,context: MainActivity) {

    Scaffold(
        bottomBar = {
            BottomNavBar(navController)  // Simplified: Always show; add visibility logic later if needed
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { backStackEntry: NavBackStackEntry ->
                FloatingWebScreen(backStackEntry)  // Assume this is parameterless; add params if needed
            }
            composable("AlertManager") { backStackEntry: NavBackStackEntry ->
                AlertManagerScreen(context)
            }
//            composable("Logs") { backStackEntry: NavBackStackEntry ->
//                LogsScreen(context)
//            }
            // Add other routes here as needed (e.g., uncomment and fix if using)
        }
    }
}

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        NavItem("home", "Home", Icons.Default.Home),
        NavItem("AlertManager", "AlertManager", Icons.Default.Notifications),
//        NavItem("Logs", "Logs", Icons.Default.Info)
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}