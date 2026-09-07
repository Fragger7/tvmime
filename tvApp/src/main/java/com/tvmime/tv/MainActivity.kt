package com.tvmime.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tvmime.tv.ui.common.TvMimeTheme
import com.tvmime.tv.ui.navigation.TvMimeNavHost
import com.tvmime.tv.viewmodel.TvMainViewModel
import com.tvmime.tv.ui.navigation.Destinations

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sharedViewModel: TvMainViewModel = viewModel(
                factory = TvMainViewModel.Factory(application)
            )
            val startDest = if (sharedViewModel.isUserLoggedIn) Destinations.LIVE_TV else Destinations.ONBOARDING

            TvMimeTheme {
                TvMimeNavHost(startDestination = startDest, sharedViewModel = sharedViewModel)
            }
        }
    }
}
