package com.lxithral.adbtools

import android.app.Application
import com.topjohnwu.superuser.Shell

class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化 libsu
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(20)
        )
        
        // 预先请求 Root 权限
        Shell.getShell {
            // 获取成功后的处理（可选）
        }
    }
}
