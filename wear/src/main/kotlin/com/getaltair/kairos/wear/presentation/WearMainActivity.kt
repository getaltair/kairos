package com.getaltair.kairos.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.getaltair.kairos.wear.presentation.theme.KairosWearTheme

/**
 * Main entry point for the Kairos WearOS app.
 * Sets up the OLED-optimized theme and swipe-dismissable navigation.
 */
class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KairosWearTheme {
                val navController = rememberSwipeDismissableNavController()
                WearNavGraph(navController = navController)
            }
        }
    }
}
