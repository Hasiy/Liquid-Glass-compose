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
 * Paint 与滤镜由调用方在绘制缓存中创建一次，尺寸或主题变化后再重建。
 * @param blurRadiusPx 模糊半径，单位为像素，须大于 0。
 * @param color 阴影颜色。
 * @return 缓存的绘制函数；null 表示退回 common 的多层 Stroke 近似实现。
 */
internal expect fun createBlurredShadowPainter(
    blurRadiusPx: Float,
    color: Color,
): ((DrawScope, Path, Float) -> Unit)?
