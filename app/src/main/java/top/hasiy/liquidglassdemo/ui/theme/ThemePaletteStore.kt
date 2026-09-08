package top.hasiyliquidglassdemo.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.drop
import top.hasiy.designsystem.tokens.ThemePalette
import top.hasiy.designsystem.tokens.ThemePaletteId

/**
 * 主題色的本機持久化。
 *
 * 只存穩定 ID（[ThemePaletteId.id]），不存色值——色值是設計交付的一部分，
 * 存下來會讓舊版本的顏色在改版後留在使用者機器上。
 *
 * 用 [android.content.SharedPreferences] 而不是 DataStore：專案目前沒有
 * DataStore 依賴，一個字串鍵值不值得為此引入。
 *
 * @param context 任何 Context；內部只保留 application context
 */
class ThemePaletteStore(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** 讀取已存主題；未存過或 ID 已失效時回退到預設主題。 */
    fun load(): ThemePalette = ThemePalette.fromId(preferences.getString(KEY_PALETTE_ID, null))

    /** 存下當前主題。 */
    fun save(id: ThemePaletteId) {
        preferences.edit().putString(KEY_PALETTE_ID, id.id).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "state_matrix_theme"
        const val KEY_PALETTE_ID = "palette_id"
    }
}

/**
 * 記住當前主題色，並在切換時自動存回本機。
 *
 * 首次組合時從本機讀取，因此頁面重進會回到上次選的主題。
 *
 * ## 為什麼讀取是同步的
 *
 * [ThemePaletteStore.load] 會在組合期間於主執行緒做一次磁碟讀取（建立
 * SharedPreferences + 讀一個字串）。改成非同步就會先用預設主題畫一帧、
 * 讀完再換色，等於每次進頁面都閃一下主題——一次幾百微秒的讀取換掉這個
 * 閃爍是划算的。若專案之後引入 DataStore，正確做法是在 `onCreate` 之前
 * 就把值準備好，而不是讓畫面先畫錯再改。
 *
 * @return 可直接寫入的主題狀態
 */
@Composable
fun rememberThemePaletteState(): MutableState<ThemePalette> {
    val context = LocalContext.current
    val store = remember(context) { ThemePaletteStore(context) }
    val state = remember(store) { mutableStateOf(store.load()) }
    // 集中在這裡存，而不是要求每個呼叫端在 onSelect 裡自己記得存——漏一處就
    // 出現「切了但沒記住」。drop(1) 跳過剛讀出來的初值，避免每次進頁面都白寫一次。
    LaunchedEffect(store) {
        snapshotFlow { state.value.id }
            .drop(1)
            .collect { store.save(it) }
    }
    return state
}
