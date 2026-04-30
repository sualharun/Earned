package com.focusguard.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.focusguard.state.PetProfile
import com.focusguard.ui.theme.EarnedColors

@Composable
fun PetSprite(
    pet: PetProfile,
    size: Dp,
    modifier: Modifier = Modifier,
    glow: Boolean = true
) {
    val context = LocalContext.current
    val fileName = "${pet.species}-${pet.stage.coerceIn(1, 5)}.png"
    val bitmap = remember(fileName) {
        runCatching {
            context.assets.open(fileName).use { stream ->
                BitmapFactory.decodeStream(stream).asImageBitmap()
            }
        }.getOrNull()
    }
    val transition = rememberInfiniteTransition(label = "petIdle")
    val bob = transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "petBob"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (glow) {
            Box(
                modifier = Modifier
                    .size(size * 0.9f)
                    .alpha(0.82f)
                    .scale(bob.value)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                EarnedColors.Primary.copy(alpha = 0.34f),
                                EarnedColors.Focus.copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = pet.name,
                modifier = Modifier
                    .size(size)
                    .scale(bob.value),
                contentScale = ContentScale.Fit
            )
        }
    }
}
