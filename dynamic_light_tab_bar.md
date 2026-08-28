# Dynamic Light Tab Bar with AGSL Shader

本文實作一個具有 **Apple Liquid Glass 風格** 的底部 Tab Bar：

- 拖動時選擇指示器即時跟隨手指位置
- 鬆手後自動吸附到最近的 Item 並回呼
- 動態光影隨觸摸點移動（AGSL RuntimeShader）
- 毛玻璃背景（RenderEffect Blur）
- Haptic Feedback + 點擊/選中縮放反饋

## 效果預覽

- 整個 Tab Bar 底部為半透明毛玻璃容器
- 選中的 Pill 會隨手指左右滑動，並在手指位置產生高光與反光
- 手指跨過每個 Item 時有輕微震動，鬆手時有確認震動

## 系統要求

| 功能 | 最低 API |
|---|---|
| `RenderEffect.createBlurEffect`（毛玻璃模糊） | API 31 (Android 12) |
| `RuntimeShader` + `RenderEffect.createRuntimeShaderEffect`（AGSL 動態光照） | API 33 (Android 13) |
| HapticFeedback / 縮放動畫 | API 21+ |

低於 API 33 的裝置會自動降級為 Compose 漸層模擬光照，仍可正常使用。

## AGSL Shader

將以下 shader 字串放在 Kotlin 檔案中，作為 `RuntimeShader` 的輸入。

```agsl
uniform shader content;
uniform float2 resolution;
uniform float2 touchPos;
uniform float intensity;    // 0.0 - 1.0
uniform float radius;       // 光源半徑（像素）

const half3 kLightColor   = half3(1.0, 1.0, 1.0);
const half3 kAmbient      = half3(0.10, 0.10, 0.16);
const half3 kSpecularTint = half3(0.95, 0.95, 1.0);

half4 main(float2 coord) {
    half4 base = content.eval(coord);

    // 1. 從觸摸點向外擴散的主光源
    float dist = length(coord - touchPos);
    float norm = clamp(dist / radius, 0.0, 1.0);
    float glow = pow(1.0 - norm, 2.8) * intensity;

    // 2. 鏡面高光（略微偏上，製造 3D 凸起感）
    float2 specCenter = touchPos + float2(-radius * 0.12, -radius * 0.22);
    float specDist = length(coord - specCenter);
    float spec = exp(-(specDist * specDist) / (radius * radius * 0.12)) * intensity * 0.75;

    // 3. 邊緣菲涅爾反光，讓 Pill 看起來像玻璃
    float2 uv = coord / resolution;
    float edgeX = min(uv.x, 1.0 - uv.x) * 2.0;
    float edgeY = min(uv.y, 1.0 - uv.y) * 2.0;
    float fresnel = pow(1.0 - min(edgeX, edgeY), 2.0) * 0.18;

    half3 lighting = kLightColor * glow
                   + kSpecularTint * spec
                   + kAmbient * fresnel;

    // 疊加光源（保留原內容 alpha）
    half3 result = base.rgb + lighting * base.a;

    return half4(result, base.a);
}
```

### Shader 參數說明

| 參數 | 型別 | 說明 |
|---|---|---|
| `content` | `shader` | 被渲染的原始內容，由 `RenderEffect` 自動注入 |
| `resolution` | `float2` | 目標區域寬高（像素） |
| `touchPos` | `float2` | 手指相對於目標區域左上角的座標（像素） |
| `intensity` | `float` | 光照強度；拖動時 1.0，停止時 0.0 |
| `radius` | `float` | 光源影響半徑，通常設為 Pill 寬度的 0.7~0.9 倍 |

## 完整 Compose 實作

```kotlin
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntOffset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val INDICATOR_SHADER_SRC = """
uniform shader content;
uniform float2 resolution;
uniform float2 touchPos;
uniform float intensity;
uniform float radius;

const half3 kLightColor   = half3(1.0, 1.0, 1.0);
const half3 kAmbient      = half3(0.10, 0.10, 0.16);
const half3 kSpecularTint = half3(0.95, 0.95, 1.0);

half4 main(float2 coord) {
    half4 base = content.eval(coord);

    float dist = length(coord - touchPos);
    float norm = clamp(dist / radius, 0.0, 1.0);
    float glow = pow(1.0 - norm, 2.8) * intensity;

    float2 specCenter = touchPos + float2(-radius * 0.12, -radius * 0.22);
    float specDist = length(coord - specCenter);
    float spec = exp(-(specDist * specDist) / (radius * radius * 0.12)) * intensity * 0.75;

    float2 uv = coord / resolution;
    float edgeX = min(uv.x, 1.0 - uv.x) * 2.0;
    float edgeY = min(uv.y, 1.0 - uv.y) * 2.0;
    float fresnel = pow(1.0 - min(edgeX, edgeY), 2.0) * 0.18;

    half3 lighting = kLightColor * glow
                   + kSpecularTint * spec
                   + kAmbient * fresnel;

    half3 result = base.rgb + lighting * base.a;
    return half4(result, base.a);
}
"""

/**
 * 動態光照底部 Tab Bar
 *
 * @param items Tab 項目文字列表
 * @param selectedIndex 當前選中索引
 * @param onSelect 選中回呼，拖動結束或點擊時觸發
 * @param modifier 外部修飾符
 */
@Composable
fun DynamicLightTabBar(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var touchX by remember { mutableFloatStateOf(Float.NaN) }
    var isDragging by remember { mutableStateOf(false) }

    var barWidthPx by remember { mutableFloatStateOf(0f) }
    var barHeightPx by remember { mutableFloatStateOf(0f) }
    var lastHapticIndex by remember { mutableIntStateOf(-1) }

    // 選中時 Indicator 的縮放反彈
    var targetScale by remember { mutableFloatStateOf(1f) }
    val indicatorScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = 450f, dampingRatio = 0.55f),
        label = "indicatorScale"
    )
    LaunchedEffect(selectedIndex) {
        targetScale = 0.90f
        delay(70)
        targetScale = 1f
    }

    // 毛玻璃背景模糊（API 31+）
    val blurEffect = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP)
        } else null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .onSizeChanged {
                barWidthPx = it.width.toFloat()
                barHeightPx = it.height.toFloat()
            }
            // 先套用模糊，再畫半透明背景，製造毛玻璃感
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurEffect != null) {
                    renderEffect = blurEffect
                }
            }
            .background(
                color = Color(0xFF15151A).copy(alpha = 0.72f),
                shape = RoundedCornerShape(36.dp)
            )
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(36.dp),
                ambientColor = Color(0xFF4444FF).copy(alpha = 0.12f),
                spotColor = Color(0xFF4444FF).copy(alpha = 0.08f)
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        touchX = offset.x.coerceIn(0f, barWidthPx)
                        isDragging = true
                        lastHapticIndex = -1
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        touchX = change.position.x.coerceIn(0f, barWidthPx)

                        // 跨過 item 邊界時給 tick 反饋
                        if (barWidthPx > 0 && items.isNotEmpty()) {
                            val index = ((touchX / barWidthPx) * items.size)
                                .toInt()
                                .coerceIn(0, items.lastIndex)
                            if (index != lastHapticIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                lastHapticIndex = index
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        if (barWidthPx > 0 && !touchX.isNaN() && items.isNotEmpty()) {
                            val index = ((touchX / barWidthPx) * items.size)
                                .toInt()
                                .coerceIn(0, items.lastIndex)
                            onSelect(index)
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        }
                        touchX = Float.NaN
                        lastHapticIndex = -1
                    },
                    onDragCancel = {
                        isDragging = false
                        touchX = Float.NaN
                        lastHapticIndex = -1
                    }
                )
            }
    ) {
        val itemWidthPx = if (items.isNotEmpty()) barWidthPx / items.size else 0f
        val itemWidthDp = with(density) { itemWidthPx.toDp() }

        // API < 33 的後備：整個 Bar 的光暈
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && !touchX.isNaN()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = Offset(touchX, size.height / 2),
                                radius = size.width * 0.45f
                            ),
                            center = Offset(touchX, size.height / 2),
                            radius = size.width * 0.45f,
                            blendMode = androidx.compose.ui.graphics.BlendMode.Screen
                        )
                    }
            )
        }

        // 選中指示器位置
        val targetOffsetPx = if (isDragging && !touchX.isNaN() && itemWidthPx > 0) {
            (touchX - itemWidthPx / 2).coerceIn(0f, barWidthPx - itemWidthPx)
        } else {
            selectedIndex * itemWidthPx
        }
        val animatedOffsetPx by animateFloatAsState(
            targetValue = targetOffsetPx,
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = 0.8f
            ),
            label = "indicatorOffset"
        )

        // 動態光照 Shader（API 33+）
        val indicatorShader = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RuntimeShader(INDICATOR_SHADER_SRC)
            } else null
        }
        val indicatorRenderEffect = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            indicatorShader != null &&
            isDragging &&
            !touchX.isNaN() &&
            itemWidthPx > 0 &&
            barHeightPx > 0
        ) {
            indicatorShader.setFloatUniform("resolution", itemWidthPx, barHeightPx)
            // touchX 是相對於 Bar 左上角；Shader 座標系與目標區域一致
            indicatorShader.setFloatUniform("touchPos", touchX - animatedOffsetPx, barHeightPx / 2f)
            indicatorShader.setFloatUniform("intensity", 1.0f)
            indicatorShader.setFloatUniform("radius", itemWidthPx * 0.75f)
            RenderEffect.createRuntimeShaderEffect(indicatorShader, "content")
        } else null

        // 選中 Pill
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetPx.toInt(), 0) }
                .width(itemWidthDp)
                .fillMaxHeight()
                .padding(4.dp)
                .graphicsLayer {
                    scaleX = indicatorScale
                    scaleY = indicatorScale
                    transformOrigin = TransformOrigin.Center
                    if (indicatorRenderEffect != null) {
                        renderEffect = indicatorRenderEffect
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFF2C2C34),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = Color.Black.copy(alpha = 0.3f)
                    )
            )
        }

        // Tab Items
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, label ->
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val itemScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.88f else 1f,
                    animationSpec = spring(stiffness = 400f, dampingRatio = 0.6f),
                    label = "itemScale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = itemScale
                            scaleY = itemScale
                        }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                onSelect(index)
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val selected = index == selectedIndex
                    BasicText(
                        text = label,
                        style = LocalTextStyle.current.copy(
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}
```

## 使用方式

```kotlin
var selectedTab by remember { mutableIntStateOf(0) }
val tabs = listOf("賽程", "排名", "小組件", "資訊", "我的")

DynamicLightTabBar(
    items = tabs,
    selectedIndex = selectedTab,
    onSelect = { selectedTab = it },
    modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 8.dp)
)
```

## 關鍵實作細節

### 1. RenderEffect 毛玻璃背景

```kotlin
val blurEffect = RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP)
```

- 套用在最外層 `Box` 的 `graphicsLayer` 上
- 模糊的是該 Composable 自身的渲染內容；若要真正「模糊背後內容」，需要額外把背景截圖並模糊，或讓 Tab Bar 與可滾動內容分層處理
- 搭配半透明背景色 `Color(0xFF15151A).copy(alpha = 0.72f)` 即可產生玻璃質感

### 2. AGSL RuntimeShader 動態光照

```kotlin
val shader = RuntimeShader(INDICATOR_SHADER_SRC)
shader.setFloatUniform("touchPos", localTouchX, localTouchY)
val renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
```

- `content` 是保留字，代表被 RenderEffect 處理的原始像素
- `touchPos` 必須轉換成**相對於目標區域（Pill）左上角**的座標，所以程式碼中用 `touchX - animatedOffsetPx`
- 只在 `isDragging` 時啟用 Shader，避免靜止時多餘運算

### 3. Haptic Feedback

| 時機 | 類型 | 效果 |
|---|---|---|
| 開始拖動 | `LongPress` | 告知用戶已進入拖動模式 |
| 跨過 Item | `SegmentTick` | 分段感，像 iOS Segment Control |
| 鬆手/點擊 | `Confirm` | 選中確認 |

### 4. 縮放反饋

- **Indicator**：`selectedIndex` 改變時先縮到 0.90 再彈回 1.0
- **Item**：單個 Tab 被按下時縮到 0.88，放開後恢復
- 使用 `spring()` 讓動畫有彈性，而不是線性插值

### 5. 版本降級

```kotlin
if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
    // 用 Compose radialGradient 模擬整體光暈
}
```

- API 33 以下沒有 `RuntimeShader`，自動用漸層產生類似效果
- 模糊效果在 API 31 以下會被跳過，只剩半透明背景

## 進階：把光照應用到整個 Tab Bar

如果想讓整個 Bar 的光影都跟隨手指（不只是選中 Pill），可以把 `RuntimeShader` 套用到最外層 `Box` 的 `graphicsLayer` 上，並把 `touchPos` 設為相對於整個 Bar 的座標：

```kotlin
val barRenderEffect = if (
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    isDragging &&
    !touchX.isNaN()
) {
    barShader.setFloatUniform("resolution", barWidthPx, barHeightPx)
    barShader.setFloatUniform("touchPos", touchX, barHeightPx / 2f)
    barShader.setFloatUniform("intensity", 0.6f)
    barShader.setFloatUniform("radius", barWidthPx * 0.35f)
    RenderEffect.createRuntimeShaderEffect(barShader, "content")
} else null
```

這會讓光暈從手指位置向整個 Bar 擴散，更接近「環境光照隨觸摸點改變」的視覺。

## 注意事項

1. **RenderEffect 與 Shader 的建立成本**：雖然範例在每次拖動幀都重新建立 `RenderEffect`，但 Tab Bar 區域很小，效能影響可忽略。若需極致優化，可保留 `RuntimeShader` 實例，僅更新 uniform 並觸發 recomposition。
2. **Haptic 裝置差異**：部分中低端機型 `SegmentTick` 效果不明顯，可降級為 `HapticFeedbackType.TextHandleMove`。
3. **AGSL 除錯**：如果 Shader 編譯失敗，Logcat 會輸出錯誤行號；建議先在 [AGSL Playground](https://shaders.skia.org/) 用 SkSL 驗證邏輯。
4. **背景模糊的真實性**：Compose 目前沒有內建「模糊背後內容」的 API；若需要真實的玻璃擬態，可考慮把 Tab Bar 背後區域預先渲染到 `Bitmap` 並套用 `RenderEffect.createBlurEffect(bitmap)`，或使用 `AndroidView` + `View.setRenderEffect` 配合底層截圖。
