package com.bizilabs.streeek.lib.presentation

import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import com.bizilabs.streeek.feature.landing.LandingScreen
import com.bizilabs.streeek.feature.tabs.TabsScreen
import com.bizilabs.streeek.lib.design.helpers.SafiBarColors
import com.bizilabs.streeek.lib.domain.models.notifications.NotificationResult
import com.bizilabs.streeek.lib.domain.models.notifications.asNotificationResult

@Composable
fun MainNavigation(
    intent: Intent,
    onValueChangeBarColors: (SafiBarColors) -> Unit,
) {
    val destination = intent.asNotificationResult().asNavigationDestination()
    Navigator(destination) {
        val screen = it.lastItem
        onValueChangeBarColors(screen.getBarColors())
        screen.Content()
    }
}

@Composable
fun Screen.getBarColors(): SafiBarColors {
    return when (this) {
        is LandingScreen -> {
            SafiBarColors(
                top = MaterialTheme.colorScheme.background,
                bottom = MaterialTheme.colorScheme.background,
            )
        }

        is TabsScreen -> {
            SafiBarColors(
                top = MaterialTheme.colorScheme.surface,
                bottom = MaterialTheme.colorScheme.surface,
            )
        }

        else -> {
            SafiBarColors(
                top = MaterialTheme.colorScheme.surface,
                bottom = MaterialTheme.colorScheme.background,
            )
        }
    }
}

fun NotificationResult?.asNavigationDestination(): Screen =
    when (this) {
        null -> LandingScreen
        else -> LandingScreen
    }
