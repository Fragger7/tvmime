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

@Composable
fun TvMimeNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.ONBOARDING
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    
    val sharedViewModel: TvMainViewModel = viewModel(
        factory = TvMainViewModel.Factory(application)
    )

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
    }
}
