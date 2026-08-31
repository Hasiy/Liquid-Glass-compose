@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package top.hasiyliquidglass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefreshIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 指示器內圈進度圈直徑與線寬 */
private val REFRESH_SPINNER_SIZE = 20.dp
private val REFRESH_SPINNER_STROKE = 2.dp

/**
 * 玻璃下拉刷新容器。手势、阈值与状态沿用 Material3，指示器改为透明玻璃表面。
 *
 * 玻璃層必須加在 [pullToRefreshIndicator] **之後**：該 modifier 內部以 graphicsLayer
 * 控制指示器的位移與縮放，玻璃層放在它之前會脫離該圖層，變成一顆常駐在列表上方、
 * 不隨下拉縮放的靜止圓點。
 *
 * @param isRefreshing 是否正在刷新
 * @param onRefresh 觸發刷新的回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param state 下拉狀態（與 Material3 保持一致）
 * @param content 列表內容
 */
@Composable
fun GlassPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    state: PullToRefreshState = rememberPullToRefreshState(),
    content: @Composable BoxScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            if (config.native) {
                PullToRefreshDefaults.Indicator(
                    state = state,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .pullToRefreshIndicator(
                            state = state,
                            isRefreshing = isRefreshing,
                            shape = PullToRefreshDefaults.shape,
                            containerColor = Color.Transparent,
                            elevation = 0.dp
                        )
                        .glassSurface(
                            shape = PullToRefreshDefaults.shape,
                            config = config.copy(shadowElevation = 0.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(REFRESH_SPINNER_SIZE),
                            color = config.contentColor,
                            strokeWidth = REFRESH_SPINNER_STROKE,
                            trackColor = Color.Transparent
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = { state.distanceFraction.coerceIn(0f, 1f) },
                            modifier = Modifier.size(REFRESH_SPINNER_SIZE),
                            color = config.contentColor,
                            strokeWidth = REFRESH_SPINNER_STROKE,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        },
        content = content
    )
}
