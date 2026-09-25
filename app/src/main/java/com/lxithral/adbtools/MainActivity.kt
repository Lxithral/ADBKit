package com.lxithral.adbtools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lxithral.adbtools.memory.FairMemoryReceiver
import com.lxithral.adbtools.tile.BaseTileService
import com.lxithral.adbtools.ui.AdbScreen
import com.lxithral.adbtools.ui.AdbViewModel
import com.lxithral.adbtools.ui.theme.ThemeState
import com.topjohnwu.superuser.Shell
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {
    private val viewModel: AdbViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // 取消 tile 触发的延迟自杀
        BaseTileService.cancelPendingKill()

        // 初始化公平运行内存接收器（仅在 Activity 打开时）
        FairMemoryReceiver.getInstance().initialize(this)

        // 外观设置状态源（ThemeState 自身持久化、即时生效）
        ThemeState.init(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        Shell.getShell {
            viewModel.refreshState()
        }

        setContent {
            // 深浅色 / Monet / 主题色 → miuix ThemeController（指南 00 §8.3）
            val controller = remember(ThemeState.themeMode, ThemeState.monet, ThemeState.keyColor) {
                ThemeController(
                    colorSchemeMode = ThemeState.colorSchemeMode(),
                    keyColor = if (ThemeState.monet) null else Color(ThemeState.keyColor),
                )
            }
            MiuixTheme(controller = controller) {
                val isLight = !ThemeState.isInDarkTheme()
                WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
                WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = isLight
                AdbScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 回到前台时解除磁贴挂起的自杀计时
        BaseTileService.cancelPendingKill()
        viewModel.startPolling()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Activity 销毁时清理
        FairMemoryReceiver.getInstance().destroy()
    }
}
