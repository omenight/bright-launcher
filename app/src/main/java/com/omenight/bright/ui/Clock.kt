package com.omenight.bright.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.omenight.bright.ui.theme.BrightColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Clock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            // Wake exactly on the next minute boundary instead of polling
            delay(60_000 - value % 60_000)
        }
    }
    // Follows the system 12/24h setting
    val time = remember(now) { DateFormat.getTimeFormat(context).format(Date(now)) }
    val date = remember(now) {
        val locale = Locale.getDefault()
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale).format(Date(now))
    }

    Column(modifier) {
        Text(
            text = time,
            style = TextStyle(
                fontSize = 72.sp,
                fontWeight = FontWeight.Thin,
                letterSpacing = (-1.5).sp,
                shadow = BrightColors.TextShadow,
            ),
        )
        Text(
            text = date,
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.5.sp,
                shadow = BrightColors.TextShadow,
            ),
            color = BrightColors.TextMuted,
        )
    }
}
