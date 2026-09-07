package com.tvmime.tv.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tvmime.tv.ui.livetv.LiveTvRoute
import com.tvmime.tv.ui.player.PlayerRoute
import com.tvmime.tv.ui.onboarding.OnboardingScreen
import com.tvmime.tv.viewmodel.TvMainViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Destinations {
    const val ONBOARDING = "onboarding"
    const val LIVE_TV = "live_tv"
    const val PLAYER = "player/{streamUrl}"
    
    fun createPlayerRoute(streamUrl: String): String {
        val encodedUrl = URLEncoder.encode(streamUrl, StandardCharsets.UTF_8.toString())
        return "player/$encodedUrl"
    }
}

import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tvmime.tv.ui.common.StatelessAppShell

@Composable
fun TvMimeNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.ONBOARDING,
    sharedViewModel: TvMainViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination

    val isFullScreen = currentRoute == Destinations.ONBOARDING || currentRoute == Destinations.PLAYER

    if (isFullScreen) {
        NavHostComponent(navController, sharedViewModel, startDestination)
    } else {
        StatelessAppShell(
            currentRoute = currentRoute,
            onNavigate = { route ->
                val dest = when(route) {
                    "LIVETV" -> Destinations.LIVE_TV
                    else -> Destinations.LIVE_TV
                }
                navController.navigate(dest) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        ) {
            NavHostComponent(navController, sharedViewModel, startDestination)
        }
    }
}

@Composable
fun NavHostComponent(
    navController: NavHostController,
    sharedViewModel: TvMainViewModel,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                viewModel = sharedViewModel,
                onLoginSuccess = {
                    navController.navigate(Destinations.LIVE_TV) {
                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Destinations.LIVE_TV) {
            LiveTvRoute(
                onNavigateToPlayer = { streamUrl ->
                    navController.navigate(Destinations.createPlayerRoute(streamUrl))
                }
            )
        }
        composable(
            route = Destinations.PLAYER,
            arguments = listOf(navArgument("streamUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val streamUrl = backStackEntry.arguments?.getString("streamUrl") ?: ""
            PlayerRoute(
                streamUrl = streamUrl,
                viewModel = sharedViewModel
            )
        }
        composable("SETTINGS") {
            com.tvmime.tv.ui.settings.SettingsScreen(
                viewModel = sharedViewModel,
                onLogoutSuccess = {
                    navController.navigate(Destinations.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
