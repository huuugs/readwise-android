package com.readwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.readwise.bookshelf.ui.BookshelfScreen
import com.readwise.ui.theme.ReadWiseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReadWiseApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadWiseApp() {
    ReadWiseTheme {
        val navController = rememberNavController()
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tabs.values().forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) {
                                        tab.selectedIcon
                                    } else {
                                        tab.icon
                                    },
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Tabs.BOOKSHELF.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Tabs.BOOKSHELF.route) {
                    BookshelfScreen(
                        onBookClick = { bookId ->
                            // Navigate to reader
                        }
                    )
                }
                composable(Tabs.DISCOVERY.route) {
                    DiscoveryScreen()
                }
                composable(Tabs.DICTIONARY.route) {
                    DictionaryScreen()
                }
                composable(Tabs.SETTINGS.route) {
                    SettingsScreen()
                }
            }
        }
    }
}

enum class Tabs(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    BOOKSHELF(
        title = "Bookshelf",
        route = "bookshelf",
        icon = androidx.compose.material.icons.Outlined.MenuBook,
        selectedIcon = androidx.compose.material.icons.Filled.MenuBook
    ),
    DISCOVERY(
        title = "Discovery",
        route = "discovery",
        icon = androidx.compose.material.icons.Outlined.Explore,
        selectedIcon = androidx.compose.material.icons.Filled.Explore
    ),
    DICTIONARY(
        title = "Dictionary",
        route = "dictionary",
        icon = androidx.compose.material.icons.Outlined.Translate,
        selectedIcon = androidx.compose.material.icons.Filled.Translate
    ),
    SETTINGS(
        title = "Settings",
        route = "settings",
        icon = androidx.compose.material.icons.Outlined.Settings,
        selectedIcon = androidx.compose.material.icons.Filled.Settings
    )
}

@Composable
fun DiscoveryScreen() {
    // TODO: Implement discovery screen
}

@Composable
fun DictionaryScreen() {
    // TODO: Implement dictionary screen
}

@Composable
fun SettingsScreen() {
    // TODO: Implement settings screen
}
