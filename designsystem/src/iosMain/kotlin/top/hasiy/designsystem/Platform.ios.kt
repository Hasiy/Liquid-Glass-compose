package top.hasiy.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

internal actual val platformSdkInt: Int
    get() = Int.MAX_VALUE

// iOS 沒有 BlurMaskFilter 等價物，一律回報 false，
// 由 common 層的多層 Stroke 近似實現接管（視覺驗收見遷移方案 §8）。
internal actual fun DrawScope.drawBlurredShadowPath(
    path: Path,
    offsetY: Float,
    blurRadiusPx: Float,
    color: Color,
): Boolean = false
