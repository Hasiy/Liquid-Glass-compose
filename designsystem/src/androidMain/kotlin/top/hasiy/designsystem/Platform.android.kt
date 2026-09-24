package top.hasiy.designsystem

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

internal actual val platformSdkInt: Int
    get() = Build.VERSION.SDK_INT

internal actual fun DrawScope.drawBlurredShadowPath(
    path: Path,
    offsetY: Float,
    blurRadiusPx: Float,
    color: Color,
): Boolean {
    // API 27 以下的硬體加速 Canvas 會忽略 BlurMaskFilter，畫出來是無模糊的實心塊，
    // 寧可回報 false 讓 common 層退回多層 Stroke 近似。
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            this.color = color.toArgb()
            maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)
        }
        val nativeCanvas = canvas.nativeCanvas
        val checkpoint = nativeCanvas.save()
        nativeCanvas.translate(0f, offsetY)
        nativeCanvas.drawPath(path.asAndroidPath(), paint)
        nativeCanvas.restoreToCount(checkpoint)
    }
    return true
}
