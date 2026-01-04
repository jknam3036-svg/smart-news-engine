package com.example.make.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.make.ui.navigation.MakeTab
import com.example.make.ui.screens.CalendarScreen
import com.example.make.ui.screens.MarketScreen
import com.example.make.ui.screens.NewsScreen
import com.example.make.ui.screens.SettingsScreen
import com.example.make.ui.screens.SmsScreen

@Composable
fun MakeApp(
    database: com.example.make.data.local.AppDatabase,
    mailRepository: com.example.make.data.repository.MailRepository
) {
    val navController = rememberNavController()
    val dao = database.intelligenceDao()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                MakeTab.values().forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = tab.icon, 
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            ) 
                        },
                        label = { 
                            Text(
                                tab.title,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            ) 
                        },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        ),
                        onClick = {
                            navController.navigate(tab.route) {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MakeTab.NEWS.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MakeTab.MARKET.route) {
                MarketScreen(dao)
            }
            composable(MakeTab.NEWS.route) {
                NewsScreen()
            }
            composable(MakeTab.SMS.route) {
                SmsScreen(dao)
            }
            composable(MakeTab.CALENDAR.route) {
                CalendarScreen(dao)
            }
            composable(MakeTab.SETTINGS.route) {
                SettingsScreen(dao)
            }
        }
    }
}
