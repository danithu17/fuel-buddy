package lk.fuelbuddy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import lk.fuelbuddy.app.ui.theme.DarkBackground

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isSolid: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val alpha1 = if (isSolid) 0.95f else 0.15f
    val alpha2 = if (isSolid) 0.85f else 0.05f
    val blurValue = if (isSolid) 0.dp else 0.dp // Blur on content is usually unwanted, we keep it 0
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        if (isSolid) DarkBackground.copy(alpha = alpha1) else Color.White.copy(alpha = alpha1),
                        if (isSolid) DarkBackground.copy(alpha = alpha2) else Color.White.copy(alpha = alpha2)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}
