package com.fortioridesign.moxykaroo.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    val scheme = lightColorScheme(
        primary = Color(0xff0045a5),
        secondary = Color(0xff1a1818),
        tertiary = Color(0xFFFFFFFF),
    )

    MaterialTheme(
        content = content,
        colorScheme = scheme
    )
}