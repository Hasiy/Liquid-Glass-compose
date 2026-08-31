package com.example.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

private val LocalGlassBackdropState = staticCompositionLocalOf<HazeState?> { null }
private val LocalGlassOverlayBlurRadius = staticCompositionLocalOf<Dp?> { null }

/**
 * SDK 的背景取樣宿主。頁面內容只會被記錄成背景來源，本身不會變模糊；所有 Glass 彈層
 * 透過同一個 [HazeState]，只在自己的輪廓內繪製取樣後的高斯模糊。
 *
 * 頁面根節點應包一層此元件，Dialog、Popup、Dropdown、ContextMenu、Picker、Drawer、
 * ModalBottomSheet 與 Tooltip 都會自動取得背景來源，不必逐個傳遞狀態。
 */
@Composable
fun GlassBackdropHost(
    modifier: Modifier = Modifier,
    blurRadius: Dp? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val hazeState = remember { HazeState() }
    CompositionLocalProvider(
        LocalGlassBackdropState provides hazeState,
        LocalGlassOverlayBlurRadius provides blurRadius,
    ) {
        Box(modifier = modifier) {
            Box(modifier = Modifier.hazeSource(state = hazeState), content = content)
            overlay()
        }
    }
}

/**
 * 前景內容模糊工具。這會模糊元件自己的內容，不是彈層背景模糊；彈出組件請使用
 * [GlassBackdropHost]，SDK 會自動把背景模糊限制在彈層自身輪廓內。
 */
fun Modifier.glassBlur(
    enabled: Boolean,
    radius: Dp = 24.dp,
): Modifier = if (enabled) blur(radius) else this

/**
 * 舊 API 相容層。語義已改為「記錄頁面背景 + 只模糊 Dialog 自身」，不再模糊整個頁面。
 */
@Composable
fun GlassDialogBlurHost(
    visible: Boolean,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 24.dp,
    dialog: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val hazeState = remember { HazeState() }
    CompositionLocalProvider(
        LocalGlassBackdropState provides hazeState,
        LocalGlassOverlayBlurRadius provides blurRadius,
    ) {
        Box(modifier = modifier) {
            Box(modifier = Modifier.hazeSource(state = hazeState), content = content)
            if (visible) dialog()
        }
    }
}

/**
 * 舊 API 相容層。Anchor 不再被模糊；Popup 由 SDK 在自身輪廓內取樣並模糊背景。
 * [blurRadius] 保留以維持來源相容，實際半徑由 Popup 的 [GlassConfig] 控制。
 */
@Composable
fun GlassPopupBlurBox(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 18.dp,
    popupContent: @Composable BoxScope.() -> Unit,
    anchorContent: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        anchorContent()
        if (expanded) {
            CompositionLocalProvider(LocalGlassOverlayBlurRadius provides blurRadius) {
                popupContent()
            }
        }
    }
}

/**
 * 讓元件在自己的 [shape] 輪廓內，對背後的頁面做高斯模糊（毛玻璃）。
 *
 * 用於任何浮在頁面之上、需要透出底層內容的元件——彈層之外，浮動的底部導覽列、
 * 頂欄、FAB 容器都適用。頁面根節點要包一層 [GlassBackdropHost] 作為取樣來源，
 * 而這個元件本身必須在 host 的 `overlay` 而不是 `content` 裡，否則會取樣到自己。
 *
 * 沒有宿主時降級為不可穿字的磨砂底色，不會變成完全透明。
 *
 * @param shape 模糊要裁切成的形狀
 * @param config 玻璃主題參數，決定模糊半徑與降級時的磨砂濃度
 * @param blurRadius 覆寫模糊半徑；null 時依序取宿主設定與 [GlassConfig.overlayBlurRadius]
 */
@Composable
fun Modifier.glassBackdrop(
    shape: Shape,
    config: GlassConfig,
    blurRadius: Dp? = null,
): Modifier {
    if (config.native) return this
    val state = LocalGlassBackdropState.current
    val resolvedBlurRadius = blurRadius
        ?: LocalGlassOverlayBlurRadius.current
        ?: config.overlayBlurRadius
    return clip(shape).then(
        if (state != null) {
            Modifier.hazeEffect(state = state) {
                this.blurRadius = resolvedBlurRadius
                backgroundColor = config.baseColor.copy(alpha = config.overlayFallbackAlpha)
                noiseFactor = 0.08f
            }
        } else {
            Modifier.background(
                color = config.baseColor.copy(alpha = config.overlayFallbackAlpha),
                shape = shape
            )
        }
    )
}

/** SDK 內部沿用的舊名，語義等同 [glassBackdrop] */
@Composable
internal fun Modifier.glassOverlayBackdrop(
    shape: Shape,
    config: GlassConfig,
    blurRadius: Dp? = null,
): Modifier = glassBackdrop(shape = shape, config = config, blurRadius = blurRadius)
