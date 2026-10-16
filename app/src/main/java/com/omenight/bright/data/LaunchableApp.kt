package com.omenight.bright.data

import android.content.ComponentName

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val component: ComponentName,
    /** True when the app declares a TV (leanback) entry point. */
    val isTvApp: Boolean,
)
