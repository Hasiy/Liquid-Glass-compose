package com.liquidglass.core.surface

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.liquidglass.core.blur.BlurStrategy
import com.liquidglass.core.blur.StackBlur
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LocalBlurStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 页面背景绘制函数：以整页尺寸绘制（光斑等），供页面与玻璃面共享（SPEC-02 §2）。
 */
typealias BackdropPainter = androidx.compose.ui.graphics.drawscope.DrawScope.(pageSize: Size) -> Unit

/**
 * SPEC-02 玻璃面容器。
 *
 * 分层（自下而上）：模糊背景层 → 玻璃叠层（fill/高光/阴影/rim）→ 前景 content。
 * 双模糊策略：[BlurStrategy.RENDER_EFFECT] 实时 GPU 模糊；
 * [BlurStrategy.OFFSCREEN_CAPTURE] GraphicsLayer 离屏快照 + StackBlur。
 *
 * @param backdrop 页面背景绘制器；玻璃面按自身在页面中的位置裁取对齐副本
 * @param strategy 覆盖全局 [LocalBlurStrategy]，用于同屏对比
 * @param captureKey 离屏模式重捕触发键（如滚动偏移），配合 100ms 节流
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(GlassTokens.radiusCard),
    strategy: BlurStrategy? = null,
    raised: Boolean = false,
    blurStrength: Float = GlassTokens.blurMedium,
    captureKey: Any? = null,
    backdrop: BackdropPainter? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val resolved = strategy ?: LocalBlurStrategy.current
    val palette = com.liquidglass.core.theme.LocalGlassPalette.current
    val fill = if (raised) palette.glassFillRaised else null

    // 玻璃面在 root 中的位置与整页尺寸，用于背景副本对齐
    var pageOffset by remember { mutableStateOf(Offset.Zero) }
    var pageSize by remember { mutableStateOf(Size.Zero) }
    val positionModifier = Modifier.onGloballyPositioned { coords ->
        pageOffset = coords.positionInRoot()
        val rootSize = coords.findRootCoordinates().size
        pageSize = Size(rootSize.width.toFloat(), rootSize.height.toFloat())
    }

    Box(modifier = modifier.then(positionModifier)) {
        if (backdrop != null) {
            when (resolved) {
                BlurStrategy.RENDER_EFFECT -> RenderEffectBackdrop(
                    backdrop = backdrop,
                    pageOffset = pageOffset,
                    pageSize = pageSize,
                    blurStrength = blurStrength,
                )
                BlurStrategy.OFFSCREEN_CAPTURE -> OffscreenBackdrop(
                    backdrop = backdrop,
                    pageOffset = pageOffset,
                    pageSize = pageSize,
                    blurStrength = blurStrength,
                    captureKey = captureKey,
                )
            }
        }
        // SPEC-02 §3 玻璃叠层
        Box(modifier = Modifier.fillMaxSize().glassOverlay(shape, fill))
        content()
    }
}

/** SPEC-02 §2.1：Modifier.blur 实时模糊背景副本（API<31 自动 no-op 降级） */
@Composable
private fun BoxScope.RenderEffectBackdrop(
    backdrop: BackdropPainter,
    pageOffset: Offset,
    pageSize: Size,
    blurStrength: Float,
) {
    BackdropCanvas(
        modifier = Modifier
            .fillMaxSize()
            .blur(blurStrength.dp),
        backdrop = backdrop,
        pageOffset = pageOffset,
        pageSize = pageSize,
    )
}

/** SPEC-02 §2.2：GraphicsLayer 记录背景副本 → 降采样快照 → StackBlur → 回填 */
@Composable
private fun BoxScope.OffscreenBackdrop(
    backdrop: BackdropPainter,
    pageOffset: Offset,
    pageSize: Size,
    blurStrength: Float,
    captureKey: Any?,
) {
    val graphicsLayer = rememberGraphicsLayer()
    var blurred by remember { mutableStateOf<ImageBitmap?>(null) }
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    var recorded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { layerSize = IntSize(it.size.width, it.size.height) }
            .drawWithContent {
                if (size.width >= 1f && size.height >= 1f) {
                    graphicsLayer.record(
                        density = this,
                        layoutDirection = layoutDirection,
                        size = IntSize(size.width.toInt(), size.height.toInt()),
                    ) {
                        this@drawWithContent.drawContent()
                    }
                    recorded = true
                    // 不调用 drawLayer：内容只进离屏层，屏幕显示模糊快照
                }
            },
    ) {
        BackdropCanvas(
            modifier = Modifier.fillMaxSize(),
            backdrop = backdrop,
            pageOffset = pageOffset,
            pageSize = pageSize,
        )
    }

    LaunchedEffect(recorded, layerSize, captureKey, pageOffset) {
        if (!recorded || layerSize == IntSize.Zero || pageSize == Size.Zero) return@LaunchedEffect
        delay(GlassTokens.snapshotThrottleMs)
        // 全链路 try/catch：离屏任一环节异常时降级为仅半透明玻璃（不崩溃），
        // 并用 Log 暴露根因便于后续定位（REQ-GS-2 安全兜底）。
        val result = try {
            val full = graphicsLayer.toImageBitmap()
            withContext(Dispatchers.Default) {
                val src = full.asAndroidBitmap()
                if (src.width < 2 || src.height < 2) null else {
                    val w = (src.width * GlassTokens.offscreenScale).toInt().coerceAtLeast(1)
                    val h = (src.height * GlassTokens.offscreenScale).toInt().coerceAtLeast(1)
                    val small = Bitmap.createScaledBitmap(src, w, h, true)
                    val radius = (blurStrength * GlassTokens.offscreenBlurRadius / GlassTokens.blurMedium)
                        .toInt()
                        .coerceAtLeast(1)
                    runCatching { StackBlur.blur(small, radius) }
                    small.asImageBitmap()
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("GlassSurface", "off-screen blur degraded", t)
            null
        }
        blurred = result
    }

    blurred?.let { bmp ->
        Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** 背景副本画布：平移到玻璃面在页面中的负偏移后按整页尺寸绘制 */
@Composable
private fun BackdropCanvas(
    modifier: Modifier,
    backdrop: BackdropPainter,
    pageOffset: Offset,
    pageSize: Size,
) {
    Canvas(modifier) {
        if (pageSize != Size.Zero) {
            translate(-pageOffset.x, -pageOffset.y) {
                backdrop(pageSize)
            }
        }
    }
}
