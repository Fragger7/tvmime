package com.tvmime.tv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tvmime.tv.ui.livetv.LiveTvScreen
// import com.tvmime.tv.ui.settings.SettingsScreen

object Destinations {
    const val LIVE_TV = "live_tv"
    const val SETTINGS = "settings"
}

@Composable
fun TvMimeNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.LIVE_TV
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Destinations.LIVE_TV) {
            LiveTvScreen()
        }
        // composable(Destinations.SETTINGS) {
        //     SettingsScreen()
        // }
    }
}
