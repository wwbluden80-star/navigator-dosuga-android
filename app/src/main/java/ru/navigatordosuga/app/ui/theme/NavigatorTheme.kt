package ru.navigatordosuga.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    background=Color(0xFF0B1112), surface=Color(0xFF11191B),
    primary=Color(0xFF66E1B0), secondary=Color(0xFF83D7F4), tertiary=Color(0xFFFFC66D),
    onBackground=Color(0xFFF4F7F7), onSurface=Color(0xFFF4F7F7)
)
private val Light = lightColorScheme(
    background=Color(0xFFF1F5F4), surface=Color(0xFFF8FBFA),
    primary=Color(0xFF087A58), secondary=Color(0xFF006D88), tertiary=Color(0xFF9A5A00),
    onBackground=Color(0xFF10201D), onSurface=Color(0xFF10201D)
)
@Composable fun NavigatorTheme(dark:Boolean=isSystemInDarkTheme(), content: @Composable () -> Unit){
    MaterialTheme(colorScheme=if(dark) Dark else Light, typography=Typography(), content=content)
}
