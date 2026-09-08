package top.hasiy.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import top.hasiy.designsystem.tokens.GlassVisualStyle

/**
 * 玻璃主題預設變體。
 *
 * 提供四種主題，可透過主題切換設定動態選用：
 * - [Drop]：半透明白 + 左上柔光 + 底部聚光（水珠感）
 * - [Neutral]：淺灰頁面上的近白卡片，無方向性柔光，靠大擴散淡陰影分層
 * - [Dark]：深色半透明玻璃，適合強調按鈕 / 深色介面
 * - [Native]：Compose 原生 Material3 樣式（無玻璃效果）
 *
 * 底下的靜態欄位不讀資源，是給非 Composable 情境（例如元件的預設參數）用的回退值。
 * **配色的權威來源是 `res/values/colors.xml`**，要換色請改那裡並改用 [themed]、
 * [drop]、[neutral]、[dark]、[native] 這幾個 @Composable 入口；使用端在自己的
 * colors.xml 定義同名資源即可覆蓋 SDK 的預設配色。
 */
object GlassPresets {

    /** 水滴：半透明白玻璃，左上柔光、底部聚光，現有 Tab Bar 的效果 */
    val Drop = GlassConfig(
        baseColor = Color(0xFF9A9AA8),
        bodyTopAlpha = 0.30f,
        bodyBottomAlpha = 0.32f,
        highlightInnerAlpha = 0.16f,
        highlightOuterAlpha = 0.035f,
        highlightCenterX = 0.20f,
        highlightCenterY = 0.08f,
        highlightRadiusFactor = 0.72f,
        borderWidth = 1.dp,
        borderTopAlpha = 0.20f,
        borderBottomAlpha = 0.02f,
        shadowElevation = 5.dp,
        shadowAmbientAlpha = 0.30f,
        shadowSpotAlpha = 0.22f,
        contentColor = Color.White,
        followTouchHighlight = true,
        hideHighlightOnTouch = false,
    )

    /**
     * 中性：淺灰頁面上的近白卡片，無方向性柔光，靠大擴散淡陰影分層。
     *
     * 這是唯一的淺色表面主題，因此柔光改成「壓暗」——白底上用 [BlendMode.Screen]
     * 提亮是無效的，靜態柔光與跟手光影都會消失。
     */
    val Neutral = GlassConfig(
        // 容器是半透明的毛玻璃：會透出頁面底色而略偏灰，內嵌的白色控件
        // （見 GlassConfig.asControlSurface）才浮得出來。兩者同色就會糊在一起。
        baseColor = Color.White,
        bodyTopAlpha = 0.60f,
        bodyBottomAlpha = 0.52f,

        // 柔光壓暗：居中大半徑低透明度的暗角，只給表面一點體積感，不讀成高光
        highlightColor = Color.Black,
        highlightBlendMode = BlendMode.Multiply,
        highlightInnerAlpha = 0.05f,
        highlightOuterAlpha = 0.012f,
        highlightCenterX = 0.50f,
        highlightCenterY = 0.50f,
        highlightRadiusFactor = 0.85f,
        // Multiply 的感知強度高於 Screen，指尖高光要更淡
        touchSpotPeakAlpha = 0.045f,

        // 描邊只留很淡的一圈，輪廓主要交給外陰影
        borderWidth = 1.dp,
        borderColor = Color.Black,
        borderTopAlpha = 0.06f,
        borderBottomAlpha = 0.03f,

        // ── 外陰影 ──────────────────────────────────────────────────────────
        // 層次主要靠「白控件 vs 灰容器」的色差撐起來，陰影只補一點浮起感，
        // 刻意調到「似有似無」——讀得出一圈灰就過頭了。
        //
        // 踩過的坑（都會讓陰影變得突兀）：
        //   · 擴散大過元件之間的空隙時，相鄰元件的陰影會互相疊加，空隙糊成一片灰
        //   · 濃度太高在淺色底上顯髒
        //   · 每個小控件都投影會讓整頁佈滿灰暈；只有容器該投影，
        //     控件由 GlassConfig.asControlSurface() 把 shadowElevation 歸零關掉
        //
        // 三個旋鈕各管一件事：
        //   softShadowAlpha  深淺。實測貼邊最深處與背景的灰階差：
        //                      0.032f → 3 階（目前）
        //                      0.05f  → 4 階
        //                      0.075f → 6 階
        //   softShadowSpread 範圍
        //   SOFT_SHADOW_BLUR_FACTOR（在 GlassModifier.kt）模糊半徑比例。
        //                      注意它會連帶影響深淺：高斯模糊是把固定的濃度攤開，
        //                      調大擴散就會同時變淡。想單獨改深淺只動 alpha。
        //
        // shadowElevation 在這裡只當開關用：設為 0 的元件不畫外陰影。
        shadowElevation = 10.dp,
        softShadowSpread = 26.dp,
        softShadowOffsetY = 7.dp,
        softShadowAlpha = 0.032f,

        contentColor = Color(0xFF111111),
        // 淺色表面沒有提亮空間，選中態與填充段一律靠強調色，因此預設開啟；
        // 深色的 Drop / Dark 維持關閉，保留原本的玻璃提亮質感。
        accentEnabled = true,
        accentColor = Color(0xFF00A15C),
        pageBackgroundTop = Color(0xFFE3E3E3),
        pageBackgroundBottom = Color(0xFFD1D1D1),

        followTouchHighlight = true,
        hideHighlightOnTouch = false,

        // 表面接近不透明，彈層的模糊意義下降，順帶省開銷
        overlayBlurRadius = 12.dp,
        overlayFallbackAlpha = 0.88f,
    )

    /** 深色玻璃：深色半透明基底，柔光較強，適合強調按鈕 */
    val Dark = GlassConfig(
        baseColor = Color(0xFF2E2E3A),
        bodyTopAlpha = 0.38f,
        bodyBottomAlpha = 0.42f,
        highlightInnerAlpha = 0.16f,
        highlightOuterAlpha = 0.06f,
        highlightCenterX = 0.22f,
        highlightCenterY = 0.10f,
        highlightRadiusFactor = 0.80f,
        borderWidth = 1.dp,
        borderTopAlpha = 0.30f,
        borderBottomAlpha = 0.04f,
        shadowElevation = 6.dp,
        shadowAmbientAlpha = 0.34f,
        shadowSpotAlpha = 0.26f,
        contentColor = Color.White,
        followTouchHighlight = true,
        hideHighlightOnTouch = false,
    )

    /**
     * 數碼白：淺色數字儀表。
     *
     * 這不是玻璃——參考稿的卡片是 `box-shadow: none` 的實色面、7px 低圓角、
     * 沒有柔光也不跟手。層次全交給描邊與底色明度台階（屏底 #F4F5F2 → 卡片 #EBEEEA
     * → dock #E4E8E4），所以柔光、指尖高光與外陰影一律關掉；留著會把平面儀表
     * 讀成玻璃。
     *
     * 配色的權威來源是 [top.hasiy.designsystem.tokens.ThemePalette.Digital]，
     * 這裡的顏色只是給非 Composable 情境用的回退值。
     */
    val Digital = GlassConfig(
        baseColor = Color(0xFFEBEEEA),
        bodyTopAlpha = 0.86f,
        bodyBottomAlpha = 0.86f,

        // 淺色表面：柔光方向必須是壓暗，但這個風格根本不畫柔光，只保留正確的
        // 混合模式讓 GlassConfig.isLightSurface 判斷得出來
        highlightColor = Color.Black,
        highlightBlendMode = BlendMode.Multiply,
        highlightInnerAlpha = 0f,
        highlightOuterAlpha = 0f,
        highlightCenterX = 0.50f,
        highlightCenterY = 0.50f,
        highlightRadiusFactor = 0.85f,
        touchSpotPeakAlpha = 0f,

        borderWidth = 1.dp,
        borderColor = Color(0xFF0C110F),
        borderTopAlpha = 0.13f,
        borderBottomAlpha = 0.13f,

        shadowEnabled = false,
        shadowElevation = 0.dp,
        shadowAmbientAlpha = 0f,
        shadowSpotAlpha = 0f,
        softShadowSpread = 0.dp,

        contentColor = Color(0xFF111514),
        accentEnabled = true,
        accentColor = Color(0xFFFF3B30),
        pageBackgroundTop = Color(0xFFF4F5F2),
        pageBackgroundBottom = Color(0xFFF4F5F2),

        followTouchHighlight = false,

        // 表面幾乎不透明，浮層再模糊也看不出來，順帶省開銷
        overlayBlurRadius = 8.dp,
        overlayFallbackAlpha = 0.98f,
    )

    /**
     * 實體按鍵：深色機械控件。
     *
     * 凸起胶帽的做法是「內緣左上白高光 + 內緣右下黑陰影 + 外投影」：
     * [GlassConfig.highlightInnerAlpha] 給左上高光，[GlassConfig.innerShadowAlpha]
     * 給內緣壓深，外投影把胶帽從底座上抬起來。
     *
     * 參考稿的卡片底是 `#3B3C3D → #292A2B → #1B1C1D` 的三段斜向漸層，而
     * [GlassConfig] 的基底漸層只能同色不同透明度，所以這裡取中段色近似。
     * 分段漸層與機械按壓位移屬於獨立 renderer 的範圍，不在配色層處理。
     */
    val Tactile = GlassConfig(
        baseColor = Color(0xFF2A2B2C),
        bodyTopAlpha = 1f,
        bodyBottomAlpha = 1f,

        highlightColor = Color.White,
        highlightBlendMode = BlendMode.Screen,
        highlightInnerAlpha = 0.11f,
        highlightOuterAlpha = 0.02f,
        highlightCenterX = 0.18f,
        highlightCenterY = 0.10f,
        highlightRadiusFactor = 0.60f,
        touchSpotPeakAlpha = 0.05f,

        // 胶帽之間靠近黑的切邊分隔，描邊上下同濃度才不會讀成單向打光
        borderWidth = 1.dp,
        borderColor = Color(0xFF080909),
        borderTopAlpha = 0.74f,
        borderBottomAlpha = 0.74f,

        innerShadowAlpha = 0.58f,
        innerShadowWidth = 7.dp,

        shadowEnabled = true,
        shadowElevation = 4.dp,
        shadowAmbientAlpha = 0.52f,
        shadowSpotAlpha = 0.42f,

        contentColor = Color(0xFFF0F1EE),
        accentEnabled = true,
        accentColor = Color(0xFF10E66B),
        pageBackgroundTop = Color(0xFF191A1B),
        pageBackgroundBottom = Color(0xFF191A1B),

        // 實體按鍵的按壓反饋是位移與壓深，不是跟手的光斑
        followTouchHighlight = false,
        hideHighlightOnTouch = false,

        overlayBlurRadius = 10.dp,
        overlayFallbackAlpha = 0.94f,
    )

    /** 原生：Compose 原生 Material3 樣式（元件內部自動改用原生渲染） */
    val Native = GlassConfig(
        native = true,
        baseColor = Color(0xFFCFCFD8),
        bodyTopAlpha = 0.9f,
        bodyBottomAlpha = 0.9f,
        highlightInnerAlpha = 0.0f,
        highlightOuterAlpha = 0.0f,
        highlightCenterX = 0.5f,
        highlightCenterY = 0.2f,
        highlightRadiusFactor = 0.6f,
        borderWidth = 0.dp,
        borderTopAlpha = 0f,
        borderBottomAlpha = 0f,
        shadowElevation = 0.dp,
        shadowAmbientAlpha = 0f,
        shadowSpotAlpha = 0f,
        contentColor = Color.White,
        followTouchHighlight = false,
        hideHighlightOnTouch = false,
    )

    /**
     * SDK 自帶的四組視覺預設，供設定頁展示（靜態回退值，配色不隨 colors.xml 變動）。
     *
     * 這裡刻意不含 [Digital] 與 [Tactile]：那兩者是綁定配色的完整風格，
     * 入口是 `ThemePalette` 的 8 組主題，不是「換個材質」的結構選項。
     */
    val All: List<GlassConfig> = listOf(Drop, Neutral, Dark, Native)

    /**
     * 依視覺結構取對應的結構預設。
     *
     * 只回傳結構參數；顏色要由呼叫端用主題令牌覆蓋。
     */
    fun forVisualStyle(style: GlassVisualStyle): GlassConfig = when (style) {
        GlassVisualStyle.DROP -> Drop
        GlassVisualStyle.NEUTRAL -> Neutral
        GlassVisualStyle.DARK -> Dark
        GlassVisualStyle.NATIVE -> Native
        GlassVisualStyle.DIGITAL -> Digital
        GlassVisualStyle.TACTILE -> Tactile
    }

    /**
     * 從 `colors.xml` 取色的水滴主題。
     *
     * 結構參數（透明度、圓角、陰影）沿用 [Drop]，只把顏色換成資源值。
     */
    @Composable
    fun drop(): GlassConfig = Drop.withColorsFrom(
        base = colorResource(R.color.glass_drop_base),
        content = colorResource(R.color.glass_drop_content),
        highlight = colorResource(R.color.glass_drop_highlight),
        border = colorResource(R.color.glass_drop_border),
        pageTop = colorResource(R.color.glass_drop_page_top),
        pageBottom = colorResource(R.color.glass_drop_page_bottom),
        accent = colorResource(R.color.glass_accent),
    )

    /** 從 `colors.xml` 取色的中性主題 */
    @Composable
    fun neutral(): GlassConfig = Neutral.withColorsFrom(
        base = colorResource(R.color.glass_neutral_base),
        content = colorResource(R.color.glass_neutral_content),
        highlight = colorResource(R.color.glass_neutral_highlight),
        border = colorResource(R.color.glass_neutral_border),
        pageTop = colorResource(R.color.glass_neutral_page_top),
        pageBottom = colorResource(R.color.glass_neutral_page_bottom),
        accent = colorResource(R.color.glass_accent),
    )

    /** 從 `colors.xml` 取色的深色玻璃主題 */
    @Composable
    fun dark(): GlassConfig = Dark.withColorsFrom(
        base = colorResource(R.color.glass_dark_base),
        content = colorResource(R.color.glass_dark_content),
        highlight = colorResource(R.color.glass_dark_highlight),
        border = colorResource(R.color.glass_dark_border),
        pageTop = colorResource(R.color.glass_dark_page_top),
        pageBottom = colorResource(R.color.glass_dark_page_bottom),
        accent = colorResource(R.color.glass_accent),
    )

    /** 從 `colors.xml` 取色的原生主題 */
    @Composable
    fun native(): GlassConfig = Native.withColorsFrom(
        base = colorResource(R.color.glass_native_base),
        content = colorResource(R.color.glass_native_content),
        highlight = colorResource(R.color.glass_native_highlight),
        border = colorResource(R.color.glass_native_border),
        pageTop = colorResource(R.color.glass_native_page_top),
        pageBottom = colorResource(R.color.glass_native_page_bottom),
        accent = colorResource(R.color.glass_accent),
    )

    /** 從 `colors.xml` 取色的所有主題，順序與 [All] 一致，供設定頁展示 */
    @Composable
    fun themed(): List<GlassConfig> {
        val drop = drop()
        val neutral = neutral()
        val dark = dark()
        val native = native()
        return remember(drop, neutral, dark, native) { listOf(drop, neutral, dark, native) }
    }
}

/** 把一組資源顏色套到既有主題的結構參數上 */
private fun GlassConfig.withColorsFrom(
    base: Color,
    content: Color,
    highlight: Color,
    border: Color,
    pageTop: Color,
    pageBottom: Color,
    accent: Color,
): GlassConfig = copy(
    baseColor = base,
    contentColor = content,
    highlightColor = highlight,
    borderColor = border,
    pageBackgroundTop = pageTop,
    pageBackgroundBottom = pageBottom,
    accentColor = accent,
)
