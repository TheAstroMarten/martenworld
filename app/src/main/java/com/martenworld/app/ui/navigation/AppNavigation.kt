package com.martenworld.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.martenworld.app.R
import com.martenworld.app.ui.play.PlayScreen
import com.martenworld.app.ui.settings.ConnectionEditorScreen
import com.martenworld.app.ui.settings.ConnectionsScreen
import com.martenworld.app.ui.settings.SettingsScreen
import com.martenworld.app.ui.worlds.WorldsScreen

object Routes {
    const val WORLDS = "worlds"
    const val PLAY = "play"
    const val SETTINGS = "settings"
    const val CONNECTIONS = "settings/connections"
    const val CONNECTION_EDIT = "settings/connections/{id}"
    const val CONNECTION_NEW_ID = "new"

    fun connectionEdit(id: String) = "settings/connections/$id"
}

private data class TopLevelDestination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.WORLDS, R.string.tab_worlds, Icons.Filled.Public),
    TopLevelDestination(Routes.PLAY, R.string.tab_play, Icons.Filled.AutoStories),
    TopLevelDestination(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings),
)

@Composable
fun MartenWorldApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route?.startsWith(destination.route) == true
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.PLAY,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.WORLDS) { WorldsScreen() }
            composable(Routes.PLAY) { PlayScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(onOpenConnections = { navController.navigate(Routes.CONNECTIONS) })
            }
            composable(Routes.CONNECTIONS) {
                ConnectionsScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(Routes.connectionEdit(Routes.CONNECTION_NEW_ID)) },
                    onEdit = { id -> navController.navigate(Routes.connectionEdit(id)) },
                )
            }
            composable(Routes.CONNECTION_EDIT) { entry ->
                val id = entry.arguments?.getString("id") ?: Routes.CONNECTION_NEW_ID
                ConnectionEditorScreen(
                    connectionId = id.takeUnless { it == Routes.CONNECTION_NEW_ID },
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}
