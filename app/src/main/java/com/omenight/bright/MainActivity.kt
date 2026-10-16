package com.omenight.bright

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.omenight.bright.ui.LauncherRoot
import com.omenight.bright.ui.glass.DawnBackdrop
import com.omenight.bright.ui.theme.BrightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DawnBackdrop.bake.start()
        setContent {
            BrightTheme {
                LauncherRoot()
            }
        }
    }
}
