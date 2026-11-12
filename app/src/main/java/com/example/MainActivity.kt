package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import com.example.ui.navigation.ZamerNavGraph
import com.example.ui.theme.PureWhite
import com.example.ui.theme.ZamerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = PureWhite.toArgb(),
                darkScrim = PureWhite.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = PureWhite.toArgb(),
                darkScrim = PureWhite.toArgb()
            )
        )
        setContent {
            ZamerTheme {
                ZamerNavGraph()
            }
        }
    }
}
