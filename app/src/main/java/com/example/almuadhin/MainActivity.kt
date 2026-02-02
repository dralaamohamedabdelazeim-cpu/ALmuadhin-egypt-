package com.example.almuadhin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import com.google.android.gms.ads.MobileAds
import com.example.almuadhin.ui.NavGraph
import com.example.almuadhin.ui.theme.MuadhinTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AdMob SDK (Test IDs should be used during development)
        MobileAds.initialize(this)

        setContent {
            MuadhinTheme {
                Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    NavGraph()
                }
            }
        }
    }
}
