package com.aifusion.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AiFusionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF8AB4F8),
            onPrimary = Color(0xFF0C1118),
            primaryContainer = Color(0xFF203A5D),
            onPrimaryContainer = Color(0xFFD5E6FF),
            background = Color(0xFF101114),
            onBackground = Color(0xFFE6E1E6),
            surface = Color(0xFF101114),
            onSurface = Color(0xFFE6E1E6),
            surfaceVariant = Color(0xFF27282D),
            onSurfaceVariant = Color(0xFFC8C6CD),
            surfaceContainerHigh = Color(0xFF2B2C31)
        ),
        content = content
    )
}
