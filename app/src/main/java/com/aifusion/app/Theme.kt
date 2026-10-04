package com.aifusion.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AiFusionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF9BB9FF),
            onPrimary = Color(0xFF0C1118),
            primaryContainer = Color(0xFF1C2942),
            onPrimaryContainer = Color(0xFFD5E6FF),
            background = Color(0xFF090B10),
            onBackground = Color(0xFFE6E1E6),
            surface = Color(0xFF0B0E14),
            onSurface = Color(0xFFE6E1E6),
            surfaceVariant = Color(0xFF1A1F29),
            onSurfaceVariant = Color(0xFFC8C6CD),
            surfaceContainerHigh = Color(0xFF151A22)
        ),
        content = content
    )
}
