package lk.fuelbuddy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PetrolBlue = Color(0xFF00E5FF)
val DieselAmber = Color(0xFFFFC107)
val DarkBackground = Color(0xFF121212)
val GlassWhite = Color(0xFFFFFFFF).copy(alpha = 0.1f)
val GlassOutline = Color(0xFFFFFFFF).copy(alpha = 0.2f)

private val DarkColorScheme = darkColorScheme(
    primary = PetrolBlue,
    secondary = DieselAmber,
    tertiary = Color(0xFFBB86FC),
    background = DarkBackground,
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun FuelBuddyTheme(
    darkTheme: Boolean = true, // Force dark mode as per requirements
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
