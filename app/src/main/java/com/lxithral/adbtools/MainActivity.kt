package com.lxithral.adbtools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lxithral.adbtools.logic.getPrivilegeLevel
import com.lxithral.adbtools.ui.AdbScreen
import com.lxithral.adbtools.ui.AdbViewModel
import com.topjohnwu.superuser.Shell
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {
    private val viewModel: AdbViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 尝试获取 Shell 以触发授权弹窗
        Shell.getShell {
            viewModel.refreshState()
        }

        setContent {
            val controller = remember(viewModel.themeMode) {
                val mode = when (viewModel.themeMode) {
                    1 -> ColorSchemeMode.Light
                    2 -> ColorSchemeMode.Dark
                    else -> ColorSchemeMode.System
                }
                ThemeController(mode)
            }
            MiuixTheme(controller = controller) {
                val isLight = when (viewModel.themeMode) {
                    1 -> true
                    2 -> false
                    else -> resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK != android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
                WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
                WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = isLight
                AdbScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
    }
}
