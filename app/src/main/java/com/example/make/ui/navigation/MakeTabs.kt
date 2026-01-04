package com.example.make.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sms
import androidx.compose.ui.graphics.vector.ImageVector

enum class MakeTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    NEWS("news", "뉴스", Icons.Filled.Newspaper),
    MARKET("market", "마켓", Icons.Filled.ShowChart),
    SMS("sms", "문자", Icons.Filled.Sms),
    CALENDAR("calendar", "캘린더", Icons.Filled.CalendarMonth),
    SETTINGS("settings", "설정", Icons.Filled.Settings)
}
