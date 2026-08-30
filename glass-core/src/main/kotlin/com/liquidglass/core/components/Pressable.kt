package com.liquidglass.core.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.liquidglass.core.theme.GlassTokens

/**
 * SPEC-03 §9：按压缩放 + 回弹，无水波纹（玻璃无水纹 REQ-NO-RIPPLE）。
 */
@Composable
fun Modifier.pressable(onClick: () -> Unit): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) GlassTokens.pressScale else 1f,
        animationSpec = spring(
            dampingRatio = GlassTokens.springDamping,
            stiffness = GlassTokens.springStiffness,
        ),
        label = "pressScale",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                },
                onTap = { onClick() },
            )
        }
}
