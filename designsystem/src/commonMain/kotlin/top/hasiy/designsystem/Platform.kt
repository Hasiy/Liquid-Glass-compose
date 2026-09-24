package top.hasiy.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * 平台 API 级别。非 Android 平台返回 [Int.MAX_VALUE]（视为能力齐备）。
 * 仅用于 [shouldUseGlassBackdropBlur] 这类以 SDK_INT 为门槛的能力判定。
 */
internal expect val platformSdkInt: Int

/**
 * 尝试以平台原生高斯模糊绘制向外偏移的软阴影路径（一次成型，无近似阶梯）。
 *
 * @return false 表示平台不支持原生模糊，调用方退回 common 的多层 Stroke 近似实现
 */
internal expect fun DrawScope.drawBlurredShadowPath(
    path: Path,
    offsetY: Float,
    blurRadiusPx: Float,
    color: Color,
): Boolean
