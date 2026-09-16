package com.posepilot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.posepilot.app.ui.PosePilotNavHost
import com.posepilot.app.ui.theme.PosePilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PosePilotApplication).container
        setContent {
            PosePilotTheme {
                PosePilotNavHost(container)
            }
        }
    }
}
