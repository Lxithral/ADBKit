package com.lxithral.adbtools.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lxithral.adbtools.R
import com.lxithral.adbtools.ui.component.BlurredBar
import com.lxithral.adbtools.ui.component.rememberBlurBackdrop
import com.lxithral.adbtools.ui.navigation.BottomBarSlot
import com.lxithral.adbtools.ui.theme.AccentColorPalette
import com.lxithral.adbtools.ui.theme.ThemeState

import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.preference.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun AdbScreen(viewModel: AdbViewModel) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(topAppBarState)

    // 玻璃采样层（指南 00 §9.1）：blurBackdrop 供标准栏毛玻璃用，backdrop 供液态玻璃折射用
    val surfaceColor = MiuixTheme.colorScheme.surface
    val blurBackdrop = rememberBlurBackdrop(ThemeState.blurEnabled)
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val liquidGlassActive = ThemeState.bottomBarStyle == ThemeState.BOTTOM_BAR_LIQUID_GLASS &&
        ThemeState.blurEnabled &&
        isRuntimeShaderSupported()

    Scaffold(
        topBar = {
            TopAppBar(
                title = if (pagerState.currentPage == 0) "ADBKit" else "设置",
                largeTitle = if (pagerState.currentPage == 0) "ADBKit" else "设置",
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            BottomBarSlot(
                blurBackdrop = blurBackdrop,
                backdrop = backdrop,
                pagerState = pagerState,
            )
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { padding ->
        Box(
            modifier = if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (liquidGlassActive) Modifier.layerBackdrop(backdrop) else Modifier),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> HomeContent(viewModel, padding, scrollBehavior)
                    1 -> SettingsContent(viewModel, padding, scrollBehavior)
                }
            }
        }
    }
}

@Composable
fun HomeContent(viewModel: AdbViewModel, padding: PaddingValues, scrollBehavior: ScrollBehavior) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + 16.dp
        )
    ) {
        item {
            StatusCardSection(viewModel)
        }

        item {
            SmallTitle(text = "开发者控制")
            Card(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                SwitchPreference(
                    title = "开发者选项",
                    summary = if (viewModel.developerOptionsEnabled) "已开启" else "已关闭",
                    checked = viewModel.developerOptionsEnabled,
                    onCheckedChange = { viewModel.toggleDeveloperOptions(it) },
                    startAction = {
                        Icon(
                            painter = painterResource(R.drawable.tile_ic_developer_options),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                )
                AnimatedVisibility(
                    visible = viewModel.developerOptionsEnabled,
                    enter = expandVertically(expandFrom = Alignment.Top),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    Column {
                        SwitchPreference(
                            title = "USB 调试",
                            summary = if (viewModel.usbAdbEnabled) "已开启" else "已关闭",
                            checked = viewModel.usbAdbEnabled,
                            onCheckedChange = { viewModel.toggleUsbDebugging(it) },
                            startAction = {
                                Icon(
                                    painter = painterResource(R.drawable.tile_ic_usb_debugging),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MiuixTheme.colorScheme.onSurface
                                )
                            }
                        )
                        SwitchPreference(
                            title = "USB 安装",
                            summary = if (viewModel.usbInstallEnabled) "已开启" else "已关闭",
                            checked = viewModel.usbInstallEnabled,
                            onCheckedChange = { viewModel.toggleUsbInstall(it) },
                            startAction = {
                                Icon(
                                    painter = painterResource(R.drawable.tile_ic_usb_install),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MiuixTheme.colorScheme.onSurface
                                )
                            }
                        )
                        SwitchPreference(
                            title = "USB 调试(安全设置)",
                            summary = if (viewModel.usbSecurityEnabled) "已开启" else "已关闭",
                            checked = viewModel.usbSecurityEnabled,
                            onCheckedChange = { viewModel.toggleUsbSecurity(it) },
                            startAction = {
                                Icon(
                                    painter = painterResource(R.drawable.tile_ic_usb_security),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MiuixTheme.colorScheme.onSurface
                                )
                            }
                        )
                    }
                }
            }
        }

        item {
            SmallTitle(text = "无线调试")
            Card(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                val ipValid = isIpv4(viewModel.ipAddress)
                SwitchPreference(
                    title = "无线调试",
                    summary = when {
                        !viewModel.developerOptionsEnabled -> "请先开启开发者选项"
                        viewModel.wirelessAdbEnabled && ipValid && viewModel.port.isNotEmpty() ->
                            "已开启 (${viewModel.ipAddress}:${viewModel.port})"
                        viewModel.wirelessAdbEnabled -> "已开启"
                        else -> "已关闭"
                    },
                    checked = viewModel.wirelessAdbEnabled,
                    enabled = viewModel.developerOptionsEnabled,
                    onCheckedChange = { viewModel.toggleWirelessAdb(it) },
                    startAction = {
                        Icon(
                            painter = painterResource(R.drawable.tile_ic_wireless_debugging),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                )
                AnimatedVisibility(
                    visible = viewModel.wirelessAdbEnabled,
                    enter = expandVertically(expandFrom = Alignment.Top),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    // 端口由 adbd 稍后写入；拿不到时绝不显示悬空的"IP:"
                    val addressText = when {
                        !isIpv4(viewModel.ipAddress) -> "未连接 Wi-Fi"
                        viewModel.port.isEmpty() -> "端口获取中…"
                        else -> "连接地址：${viewModel.ipAddress}:${viewModel.port}"
                    }
                    Text(
                        text = addressText,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsContent(viewModel: AdbViewModel, padding: PaddingValues, scrollBehavior: ScrollBehavior) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + 16.dp
        )
    ) {
        item {
            SmallTitle(text = "外观")
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                    TabRow(
                        tabs = listOf("跟随系统", "浅色", "深色"),
                        selectedTabIndex = ThemeState.themeMode,
                        onTabSelected = { ThemeState.setThemeMode(it) },
                    )
                }
                SwitchPreference(
                    title = "动态取色",
                    summary = "跟随系统壁纸配色（Monet）",
                    checked = ThemeState.monet,
                    onCheckedChange = { ThemeState.setMonet(it) }
                )
                AnimatedVisibility(visible = !ThemeState.monet) {
                    OverlayDropdownPreference(
                        title = "主题色",
                        summary = "关闭动态取色时生效",
                        items = AccentColorPalette.map { it.first },
                        selectedIndex = AccentColorPalette
                            .indexOfFirst { it.second == ThemeState.keyColor }
                            .takeIf { it >= 0 } ?: 0,
                        onSelectedIndexChange = { ThemeState.setKeyColor(AccentColorPalette[it].second) }
                    )
                }
                SwitchPreference(
                    title = "模糊",
                    summary = "毛玻璃与液态玻璃效果（需要 Android 13+）",
                    checked = ThemeState.blurEnabled,
                    onCheckedChange = { ThemeState.setBlurEnabled(it) }
                )
                OverlayDropdownPreference(
                    title = "底栏形态",
                    items = listOf("标准", "悬浮", "液态玻璃"),
                    selectedIndex = ThemeState.bottomBarStyle,
                    onSelectedIndexChange = { ThemeState.setBottomBarStyle(it) }
                )
                val predictiveBackSupported =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                SwitchPreference(
                    title = "预测性返回手势",
                    summary = if (predictiveBackSupported) "系统返回时预览上一页，切换后立即生效"
                              else "需要 Android 14 及以上",
                    checked = ThemeState.predictiveBack,
                    enabled = predictiveBackSupported,
                    onCheckedChange = {
                        ThemeState.setPredictiveBack(it, context)
                        context.findActivity()?.recreate()
                    }
                )
            }
        }
        item {
            SmallTitle(text = "关于")
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                ArrowPreference(
                    title = "关于本项目",
                    summary = "一个简洁的 ADB 调试工具",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Lxithral/ADBKit")))
                    }
                )
                ArrowPreference(
                    title = "开发者",
                    summary = "L'xithral",
                    startAction = {
                        Image(
                            painter = painterResource(R.drawable.avatar),
                            contentDescription = "开发者头像",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    },
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Lxithral")))
                    }
                )
            }
        }
    }
}

private fun isIpv4(value: String): Boolean =
    value.matches(Regex("""^(\d{1,3}\.){3}\d{1,3}$"""))

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun StatusCardSection(viewModel: AdbViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RootStatusCard(
            granted = viewModel.rootGranted,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DebugStatCard(
                title = "USB 调试状态",
                status = if (viewModel.usbAdbEnabled) "已开启" else "已关闭",
                modifier = Modifier.weight(1f)
            )
            DebugStatCard(
                title = "无线调试",
                status = if (viewModel.wirelessAdbEnabled) "已开启" else "已关闭",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RootStatusCard(granted: Boolean, modifier: Modifier = Modifier) {
    val statusColor = if (granted) Color(0xFF36D167) else Color(0xFFFF5A52)
    val statusBackground = if (granted) Color(0xFFDFFAE4) else Color(0xFFFFE5E3)

    val textPrimaryColor = Color(0xFF2F3A32)
    val textSecondaryColor = if (granted) Color(0xFF2F3A32).copy(alpha = 0.78f) else Color(0xFFFF5A52)

    Card(
        modifier = modifier,
        colors = CardDefaults.defaultColors(color = statusBackground),
        pressFeedbackType = PressFeedbackType.Tilt,
        onClick = { /* Feedback only */ }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 20.dp, y = 20.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    modifier = Modifier.size(136.dp),
                    painter = painterResource(R.drawable.ic_android),
                    contentDescription = null,
                    tint = statusColor.copy(alpha = 0.35f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Root 权限",
                    style = MiuixTheme.textStyles.title3,
                    color = textPrimaryColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (granted) "已获取" else "未获取",
                    style = MiuixTheme.textStyles.body2,
                    color = textSecondaryColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DebugStatCard(title: String, status: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        pressFeedbackType = PressFeedbackType.Tilt,
        onClick = { /* Feedback only */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Text(
                text = status,
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
