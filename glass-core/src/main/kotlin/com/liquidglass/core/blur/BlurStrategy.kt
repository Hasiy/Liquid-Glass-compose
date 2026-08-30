package com.liquidglass.core.blur

/**
 * SPEC-02 §2 双模糊策略。
 * - [RENDER_EFFECT]：Modifier.blur 实时 GPU 模糊（API 31+，低版本自动降级为纯半透明）
 * - [OFFSCREEN_CAPTURE]：GraphicsLayer 离屏快照 → 降采样 → StackBlur → 回填
 */
enum class BlurStrategy {
    RENDER_EFFECT,
    OFFSCREEN_CAPTURE,
}
