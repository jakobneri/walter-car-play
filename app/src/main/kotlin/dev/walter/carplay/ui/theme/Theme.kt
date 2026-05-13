package dev.walter.carplay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(primary = Blue80, secondary = BlueGrey80)
private val LightColors = lightColorScheme(primary = Blue40, secondary = BlueGrey40)

@Composable
fun WalterCarPlayTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
