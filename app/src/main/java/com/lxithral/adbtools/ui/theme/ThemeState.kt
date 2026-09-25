package com.lxithral.adbtools.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.lxithral.adbtools.MainApp
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

/**
 * 全部外观设置的唯一状态源（指南 00 §8）：
 * 深浅色 / Monet / 主题色 / 模糊 / 底栏形态 / 预测性返回，全部持久化、全部即时生效。
 */
object ThemeState {

    // 底栏形态（指南 00 §6.4：标准 / 悬浮 / 液态玻璃）
    const val BOTTOM_BAR_STANDARD = 0
    const val BOTTOM_BAR_FLOATING = 1
    const val BOTTOM_BAR_LIQUID_GLASS = 2

    private const val PREFS_NAME = "adb_prefs"
    private const val DEFAULT_KEY_COLOR = 0xFF3482FF.toInt()

    private lateinit var themeModeState: MutableIntState
    private lateinit var monetState: MutableState<Boolean>
    private lateinit var keyColorState: MutableIntState
    private lateinit var blurEnabledState: MutableState<Boolean>
    private lateinit var bottomBarStyleState: MutableIntState
    private lateinit var predictiveBackState: MutableState<Boolean>

    val themeMode: Int get() = themeModeState.intValue // 0 跟随系统 1 浅色 2 深色
    val monet: Boolean get() = monetState.value
    val keyColor: Int get() = keyColorState.intValue
    val blurEnabled: Boolean get() = blurEnabledState.value
    val bottomBarStyle: Int get() = bottomBarStyleState.intValue
    val predictiveBack: Boolean get() = predictiveBackState.value

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        themeModeState = mutableIntStateOf(p.getInt("theme_mode", 0))
        monetState = mutableStateOf(p.getBoolean("monet", false))
        keyColorState = mutableIntStateOf(p.getInt("key_color", DEFAULT_KEY_COLOR))
        blurEnabledState = mutableStateOf(p.getBoolean("enable_blur", true))
        bottomBarStyleState = mutableIntStateOf(p.getInt("bottom_bar_style", BOTTOM_BAR_STANDARD))
        predictiveBackState = mutableStateOf(p.getBoolean("enable_predictive_back", false))
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

    fun setMonet(value: Boolean) {
        monetState.value = value
        edit { it.putBoolean("monet", value) }
    }

    fun setKeyColor(color: Int) {
        keyColorState.intValue = color
        edit { it.putInt("key_color", color) }
    }

    fun setBlurEnabled(value: Boolean) {
        blurEnabledState.value = value
        edit { it.putBoolean("enable_blur", value) }
    }

    fun setBottomBarStyle(style: Int) {
        bottomBarStyleState.intValue = style
        edit { it.putInt("bottom_bar_style", style) }
    }

    fun setPredictiveBack(enable: Boolean, context: Context) {
        predictiveBackState.value = enable
        edit { it.putBoolean("enable_predictive_back", enable) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // 运行时反射开关系统级预测性返回（指南 00 §10）
            org.lsposed.hiddenapibypass.HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback"
            )
            MainApp.setEnableOnBackInvokedCallback(context.applicationInfo, enable)
        }
    }

    /** 主题模式 → miuix ColorSchemeMode（Monet 三分支，指南 00 §8.3） */
    fun colorSchemeMode(): ColorSchemeMode = when {
        monet -> when (themeMode) {
            1 -> ColorSchemeMode.MonetLight
            2 -> ColorSchemeMode.MonetDark
            else -> ColorSchemeMode.MonetSystem
        }
        themeMode == 1 -> ColorSchemeMode.Light
        themeMode == 2 -> ColorSchemeMode.Dark
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
