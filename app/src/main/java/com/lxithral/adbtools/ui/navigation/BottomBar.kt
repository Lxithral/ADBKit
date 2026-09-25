package com.lxithral.adbtools.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxithral.adbtools.ui.component.BlurredBar
import com.lxithral.adbtools.ui.component.FloatingBottomBar
import com.lxithral.adbtools.ui.component.FloatingBottomBarItem
import com.lxithral.adbtools.ui.theme.ThemeState
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class BottomBarItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomBarItems = listOf(
    BottomBarItem("主页", MiuixIcons.Home),
    BottomBarItem("设置", MiuixIcons.Settings),
)

/**
 * 三形态底栏（指南 00 §6 / 03 §2）：
 * STANDARD = 贴底全宽 miuix NavigationBar（模糊开启时包毛玻璃）；
 * FLOATING = 悬浮胶囊（不透明 surfaceContainer + 主色 15% 指示器）；
 * LIQUID_GLASS = 液态玻璃胶囊（drawBackdrop 折射透镜）。
 */
@Composable
fun BottomBar(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    when (ThemeState.bottomBarStyle) {
        ThemeState.BOTTOM_BAR_STANDARD -> {
            BlurredBar(blurBackdrop, blurActive = true) {
                NavigationBar(
                    modifier = modifier.fillMaxWidth(),
                    color = if (blurBackdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                ) {
                    bottomBarItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            modifier = Modifier.weight(1f),
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            icon = item.icon,
                            label = item.label,
                        )
                    }
                }
            }
        }

        ThemeState.BOTTOM_BAR_FLOATING, ThemeState.BOTTOM_BAR_LIQUID_GLASS -> {
            val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                .let { inset -> if (inset != 0.dp) 8.dp + inset else 28.dp }
            FloatingBottomBar(
                modifier = modifier.padding(start = 28.dp, end = 28.dp, bottom = bottomPadding),
                selectedIndex = pagerState.currentPage,
                onSelected = { scope.launch { pagerState.animateScrollToPage(it) } },
                backdrop = backdrop,
                tabsCount = bottomBarItems.size,
                // ★ 关键开关：液态玻璃 = 折射透镜；设备不支持 RuntimeShader 时优雅降级为不透明胶囊
                isBlurEnabled = ThemeState.bottomBarStyle == ThemeState.BOTTOM_BAR_LIQUID_GLASS &&
                    isRuntimeShaderSupported(),
            ) { activateTab ->
                bottomBarItems.forEachIndexed { index, item ->
                    FloatingBottomBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { activateTab(index) },
                        modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                    ) {
                        Icon(item.icon, contentDescription = item.label)
                        Text(item.label, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Scaffold bottomBar 槽位装配：悬浮胶囊底栏必须整体水平居中（指南 00 §6.1） */
@Composable
fun BottomBarSlot(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop,
    pagerState: PagerState,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        BottomBar(
            blurBackdrop = blurBackdrop,
            backdrop = backdrop,
            pagerState = pagerState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
