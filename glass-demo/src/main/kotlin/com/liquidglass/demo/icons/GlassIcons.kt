package com.liquidglass.demo.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** 自绘 Material 风格图标，避免引入 material-icons-extended */
object GlassIcons {

    /** tune 风格设置图标：三条横线 + 三个圆点 */
    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(4f, 7f); lineTo(20f, 7f)
                moveTo(4f, 12f); lineTo(20f, 12f)
                moveTo(4f, 17f); lineTo(20f, 17f)
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 5f); arcToRelative(2f, 2f, 0f, true, true, 0f, 4f); arcToRelative(2f, 2f, 0f, true, true, 0f, -4f); close()
                moveTo(15f, 10f); arcToRelative(2f, 2f, 0f, true, true, 0f, 4f); arcToRelative(2f, 2f, 0f, true, true, 0f, -4f); close()
                moveTo(7f, 15f); arcToRelative(2f, 2f, 0f, true, true, 0f, 4f); arcToRelative(2f, 2f, 0f, true, true, 0f, -4f); close()
            }
        }.build()
    }

    val Notifications: ImageVector by lazy {
        ImageVector.Builder(
            name = "notifications",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 22f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
                horizontalLineToRelative(-4f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
                close()
                moveTo(18f, 16f)
                verticalLineToRelative(-5f)
                arcToRelative(6f, 6f, 0f, false, false, -12f, 0f)
                verticalLineToRelative(5f)
                lineToRelative(-2f, 2f)
                verticalLineToRelative(1f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(-1f)
                close()
            }
        }.build()
    }

    /** 白圆 + 深色 i（对齐参考截图第三行） */
    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "info",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                arcToRelative(10f, 10f, 0f, true, true, 0f, 20f)
                arcToRelative(10f, 10f, 0f, true, true, 0f, -20f)
                close()
            }
            path(fill = SolidColor(Color(0xFF1A1622))) {
                moveTo(11f, 7f); horizontalLineToRelative(2f); verticalLineToRelative(2.5f); horizontalLineToRelative(-2f); close()
                moveTo(11f, 10.5f); horizontalLineToRelative(2f); verticalLineToRelative(6.5f); horizontalLineToRelative(-2f); close()
            }
        }.build()
    }

    val Add: ImageVector by lazy {
        ImageVector.Builder(
            name = "add",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(12f, 5f)
                lineTo(12f, 19f)
                moveTo(5f, 12f)
                lineTo(19f, 12f)
            }
        }.build()
    }

    val Favorite: ImageVector by lazy {
        ImageVector.Builder(
            name = "favorite",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 21.35f)
                lineToRelative(-1.45f, -1.32f)
                curveToRelative(-5.4f, -4.92f, -8.55f, -7.78f, -8.55f, -11.28f)
                curveToRelative(0f, -2.86f, 2.24f, -5.1f, 5.1f, -5.1f)
                curveToRelative(1.62f, 0f, 3.18f, 0.75f, 4.2f, 1.94f)
                curveToRelative(1.02f, -1.19f, 2.58f, -1.94f, 4.2f, -1.94f)
                curveToRelative(2.86f, 0f, 5.1f, 2.24f, 5.1f, 5.1f)
                curveToRelative(0f, 3.5f, -3.15f, 6.36f, -8.55f, 11.28f)
                close()
            }
        }.build()
    }

    val FavoriteBorder: ImageVector by lazy {
        ImageVector.Builder(
            name = "favorite_border",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 20.5f)
                curveToRelative(-5.2f, -4.75f, -8.5f, -7.7f, -8.5f, -11.2f)
                curveToRelative(0f, -2.6f, 2f, -4.6f, 4.6f, -4.6f)
                curveToRelative(1.5f, 0f, 3f, 0.7f, 3.9f, 1.9f)
                curveToRelative(0.9f, -1.2f, 2.4f, -1.9f, 3.9f, -1.9f)
                curveToRelative(2.6f, 0f, 4.6f, 2f, 4.6f, 4.6f)
                curveToRelative(0f, 3.5f, -3.3f, 6.45f, -8.5f, 11.2f)
                close()
            }
        }.build()
    }
}
