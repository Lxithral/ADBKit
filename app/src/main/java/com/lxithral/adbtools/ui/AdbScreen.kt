package com.lxithral.adbtools.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
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
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.preference.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun AdbScreen(viewModel: AdbViewModel) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(topAppBarState)

    Scaffold(
        topBar = {
            TopAppBar(
                title = if (pagerState.currentPage == 0) "ADBKit" else "设置",
                largeTitle = if (pagerState.currentPage == 0) "ADBKit" else "设置",
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    icon = MiuixIcons.Home,
                    label = "主页"
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    icon = MiuixIcons.Settings,
                    label = "设置"
                )
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> HomeContent(viewModel, padding, scrollBehavior)
                1 -> SettingsContent(viewModel, padding, scrollBehavior)
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
                SwitchPreference(
                    title = "USB 调试",
                    summary = if (viewModel.usbAdbEnabled) "已开启" else "已关闭",
                    checked = viewModel.usbAdbEnabled,
                    enabled = viewModel.developerOptionsEnabled,
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
                    enabled = viewModel.developerOptionsEnabled,
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
                    enabled = viewModel.developerOptionsEnabled,
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

        item {
            SmallTitle(text = "无线调试")
            Card(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                SwitchPreference(
                    title = "无线调试",
                    summary = if (viewModel.wirelessAdbEnabled) "已开启 (${viewModel.ipAddress}:${viewModel.port})" else "已关闭",
                    checked = viewModel.wirelessAdbEnabled,
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
            }
        }
    }
}

@Composable
fun SettingsContent(viewModel: AdbViewModel, padding: PaddingValues, scrollBehavior: ScrollBehavior) {
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
                val themeOptions = listOf("跟随系统", "浅色", "深色")
                OverlayDropdownPreference(
                    title = "主题",
                    items = themeOptions,
                    selectedIndex = viewModel.themeMode,
                    onSelectedIndexChange = { viewModel.setTheme(it) }
                )
            }
        }
        item {
            SmallTitle(text = "关于")
            val context = LocalContext.current
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                ArrowPreference(
                    title = "关于本项目",
                    summary = "一个简洁的 ADB 调试工具",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Lxithral/ADBKit")))
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Lxithral")))
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.avatar),
                        contentDescription = "开发者头像",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "开发者",
                            style = MiuixTheme.textStyles.title3
                        )
                        Text(
                            text = "L'xithral",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }
        }
    }
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