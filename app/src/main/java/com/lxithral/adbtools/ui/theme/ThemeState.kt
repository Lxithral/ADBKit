package com.lxithral.adbtools.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

/**
 * 全部外观设置的唯一状态源：
 * 深浅色 / 底栏形态，全部持久化、全部即时生效。
 * 模糊固定开启（设备支持 RuntimeShader 即生效，不支持自动降级）。
 */
object ThemeState {

    // 底栏形态（指南 00 §6.4：标准 / 悬浮 / 液态玻璃）
    const val BOTTOM_BAR_STANDARD = 0
    const val BOTTOM_BAR_FLOATING = 1
    const val BOTTOM_BAR_LIQUID_GLASS = 2

    private const val PREFS_NAME = "adb_prefs"

    private lateinit var themeModeState: MutableIntState
    private lateinit var bottomBarStyleState: MutableIntState

    val themeMode: Int get() = themeModeState.intValue // 0 跟随系统 1 浅色 2 深色
    val bottomBarStyle: Int get() = bottomBarStyleState.intValue

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        themeModeState = mutableIntStateOf(p.getInt("theme_mode", 0))
        bottomBarStyleState = mutableIntStateOf(p.getInt("bottom_bar_style", BOTTOM_BAR_STANDARD))
    }

    private fun edit(block: (SharedPreferences.Editor) -> Unit) {
        prefs?.let { p ->
            p.edit().also(block).apply()
        }
    }

    fun setThemeMode(mode: Int) {
        themeModeState.intValue = mode
        edit { it.putInt("theme_mode", mode) }
    }

    fun setBottomBarStyle(style: Int) {
        bottomBarStyleState.intValue = style
        edit { it.putInt("bottom_bar_style", style) }
    }

    /** 主题模式 → miuix ColorSchemeMode */
    fun colorSchemeMode(): ColorSchemeMode = when (themeMode) {
        1 -> ColorSchemeMode.Light
        2 -> ColorSchemeMode.Dark
        else -> ColorSchemeMode.System
    }

    /** 深色判定：全 App 唯一收敛点 */
    @Composable
    fun isInDarkTheme(): Boolean = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
}

/** 顶层深色判定（液态玻璃底栏等组件使用） */
@Composable
fun isInDarkTheme(): Boolean = ThemeState.isInDarkTheme()
