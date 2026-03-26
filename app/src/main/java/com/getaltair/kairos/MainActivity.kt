package com.getaltair.kairos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.getaltair.kairos.navigation.KairosNavGraph
import com.getaltair.kairos.ui.theme.KairosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KairosTheme {
                KairosNavGraph()
            }
        }
    }
}
