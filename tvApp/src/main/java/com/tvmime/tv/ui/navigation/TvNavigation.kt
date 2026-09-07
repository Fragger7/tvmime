package com.tvmime.tv.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tvmime.tv.ui.livetv.LiveTvRoute
import com.tvmime.tv.ui.onboarding.OnboardingScreen
import com.tvmime.tv.viewmodel.TvMainViewModel

object Destinations {
    const val ONBOARDING = "onboarding"
    const val LIVE_TV = "live_tv"
}

@Composable
fun TvMimeNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.ONBOARDING
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    
    // Fallback simple factory if we don't have DI. TvMainViewModel might need to be refactored too.
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
                    // Navigate to player. To be implemented in next sprint.
                }
            )
        }
    }
}
