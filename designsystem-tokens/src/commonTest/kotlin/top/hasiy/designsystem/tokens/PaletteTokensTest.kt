/*
 * Copyright 2026 FitDash contributors.
 */
package top.hasiy.designsystem.tokens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * 六组主题色走同一条组装路径，差异只在色值。
 *
 * 这组测试守的是**结构**不是观感：一旦有人给某一组主题单独改了屏底或面板，
 * 或者给它开一条自己的组装分支，这里就会红。观感由参考稿对照人工验收。
 */
class PaletteTokensTest {

    /** 黑色系：荧光绿、品牌天蓝、熔岩橙、极光紫。 */
    private val darkFamily = listOf(
        ThemePalette.Lime,
        ThemePalette.Sky,
        ThemePalette.Ember,
        ThemePalette.Aurora,
    )

    /** 白色系：北欧雾绿、冰川蓝。 */
    private val lightFamily = listOf(ThemePalette.Nordic, ThemePalette.Ocean)

    /** 一组主题里所有「表面」性质的字段，逐一取出来比。 */
    private fun surfacesOf(palette: ThemePalette) = listOf(
        "track" to palette.track,
        "danger" to palette.danger,
        "background" to palette.background,
        "screen" to palette.screen,
        "panel" to palette.panel,
        "line" to palette.line,
        "muted" to palette.muted,
        "ink" to palette.ink,
        "screenContent" to palette.screenContent,
        "lift" to palette.lift,
        "sunk" to palette.sunk,
        "glass" to palette.glass,
        "glassDeep" to palette.glassDeep,
        "sheen" to palette.sheen,
    )

    @Test
    fun `dark family shares one set of surfaces`() {
        val reference = surfacesOf(ThemePalette.Lime)
        darkFamily.forEach { palette ->
            assertEquals(reference, surfacesOf(palette), "${palette.id.id} 的表面色偏离了黑色系")
        }
    }

    @Test
    fun `dark family surfaces come from the token file`() {
        assertEquals(
            surfacesOf(ThemePalette.Lime).map { it.second },
            with(PaletteTokens.Dark) {
                listOf(
                    track, danger, background, screen, panel, line,
                    muted, ink, screenContent, lift, sunk, glass, glassDeep, sheen,
                )
            },
        )
    }

    @Test
    fun `light family surfaces come from the token file`() {
        lightFamily.forEach { palette ->
            assertEquals(
                with(PaletteTokens.Light) {
                    listOf(
                        track, danger, background, screen, panel, line,
                        muted, ink, screenContent, lift, sunk, glass, glassDeep, sheen,
                    )
                },
                surfacesOf(palette).map { it.second },
                "${palette.id.id} 的表面色偏离了白色系",
            )
        }
    }

    /** 分家的依据是屏内明暗，不是画布——Tactile 正是画布浅、屏内深。 */
    @Test
    fun `families are split by screen lightness`() {
        darkFamily.forEach { assertTrue(!it.isLight, "${it.id.id} 应属黑色系") }
        lightFamily.forEach { assertTrue(it.isLight, "${it.id.id} 应属白色系") }
    }

    @Test
    fun `every palette has a distinct accent`() {
        val accents = ThemePalette.All.map { it.accent }
        assertEquals(accents.size, accents.toSet().size, "有两组主题用了同一个强调色")
    }

    /**
     * 没给 `accentDeep` 的配色要回落到主色本身。
     *
     * 这是 [AccentSet] 用 `0L` 表示「未指定」的那条回退——写死成 0 的话，
     * 压在浅底上的小字会变成透明黑。
     */
    @Test
    fun `accent deep falls back to the accent itself`() {
        darkFamily.forEach {
            assertEquals(it.accent, it.accentDeep, "${it.id.id} 的 accentDeep 应回落到主色")
        }
        // 白色系必须真的给一档更深的：主色压白字都只有 3.5~3.7:1，不够 AA
        lightFamily.forEach {
            assertNotEquals(it.accent, it.accentDeep, "${it.id.id} 应给一档更深的强调色")
        }
    }

    /** 降调色是浅色系才需要的东西；深色系不给，留 0 表示「不降」。 */
    @Test
    fun `quiet accent is only set where the accent cannot carry small text`() {
        darkFamily.forEach {
            assertEquals(0L, it.quietAccent, "${it.id.id} 不该设降调色")
        }
        lightFamily.forEach {
            assertNotEquals(0L, it.quietAccent, "${it.id.id} 应给降调色")
        }
    }
}
