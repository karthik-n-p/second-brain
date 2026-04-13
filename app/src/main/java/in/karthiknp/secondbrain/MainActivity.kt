package `in`.karthiknp.secondbrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import `in`.karthiknp.secondbrain.ui.chat.ChatScreen
import `in`.karthiknp.secondbrain.ui.chat.ChatViewModel
import `in`.karthiknp.secondbrain.ui.history.HistoryScreen
import `in`.karthiknp.secondbrain.ui.history.HistoryViewModel
import `in`.karthiknp.secondbrain.ui.today.TodayScreen
import `in`.karthiknp.secondbrain.ui.today.TodayViewModel
import `in`.karthiknp.secondbrain.ui.theme.SecondBrainTheme
import `in`.karthiknp.secondbrain.ui.theme.AccentBlue

/**
 * Main entry point for Second Brain v4 — Unified Daily Log.
 * 3-tab navigation: Today | History | Brain
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecondBrainTheme {
                MainApp()
            }
        }
    }
}

// ─── Navigation Routes ──────────────────────────────────────────────

private sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Today : Screen("today", "Today", Icons.Default.CalendarToday)
    data object History : Screen("history", "History", Icons.Default.History)
    data object Brain : Screen("brain", "Brain", Icons.Default.Psychology)
}

private val bottomNavItems = listOf(Screen.Today, Screen.History, Screen.Brain)

// ─── Main App Composable ────────────────────────────────────────────

@Composable
private fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == screen.route
                    } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            indicatorColor = AccentBlue.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Today.route) {
                val todayViewModel: TodayViewModel = viewModel()
                TodayScreen(viewModel = todayViewModel)
            }
            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = viewModel()
                HistoryScreen(viewModel = historyViewModel)
            }
            composable(Screen.Brain.route) {
                val chatViewModel: ChatViewModel = viewModel()
                ChatScreen(viewModel = chatViewModel)
            }
        }
    }
}