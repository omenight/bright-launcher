package com.omenight.bright

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.omenight.bright.ui.LauncherRoot
import com.omenight.bright.ui.glass.BackdropEngine
import com.omenight.bright.ui.theme.BrightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BackdropEngine.start(this)
        setContent {
            BrightTheme {
                LauncherRoot()
            }
        }
    }
}
