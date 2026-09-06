package com.tvmime.tv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tvmime.tv.ui.livetv.LiveTvScreen
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
    // Shared ViewModel across the graph
    val sharedViewModel: TvMainViewModel = hiltViewModel()

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
            LiveTvScreen(viewModel = sharedViewModel)
        }
    }
}
