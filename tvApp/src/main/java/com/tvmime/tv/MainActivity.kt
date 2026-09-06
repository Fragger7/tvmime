package com.tvmime.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tvmime.tv.ui.common.TvMimeTheme
import com.tvmime.tv.ui.navigation.TvMimeNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvMimeTheme {
                TvMimeNavHost()
            }
        }
    }
}
