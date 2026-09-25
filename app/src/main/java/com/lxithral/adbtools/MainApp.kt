package com.lxithral.adbtools

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import com.topjohnwu.superuser.Shell
import org.lsposed.hiddenapibypass.HiddenApiBypass

class MainApp : Application() {

    companion object {
        /**
         * 主界面是否处于前台。
         * 磁贴自杀前会检查它，避免把正在使用的应用误杀。
         */
        @Volatile
        var isMainActivityInForeground: Boolean = false
            private set

        /**
         * 运行时开关系统级预测性返回手势（指南 00 §10）。
         * Manifest 不写静态属性，写了开关就失效；改为反射设置，
         * 切换后需要 activity.recreate() 才会生效。
         */
        fun setEnableOnBackInvokedCallback(appInfo: ApplicationInfo, enable: Boolean) {
            runCatching {
                val method = ApplicationInfo::class.java
                    .getDeclaredMethod("setEnableOnBackInvokedCallback", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(appInfo, enable)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(20)
        )

        // 预测性返回（仅 Android 14+，设置页开关控制，默认关闭）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val prefs: SharedPreferences = getSharedPreferences("adb_prefs", MODE_PRIVATE)
            if (prefs.getBoolean("enable_predictive_back", false)) {
                HiddenApiBypass.addHiddenApiExemptions(
                    "Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback"
                )
                setEnableOnBackInvokedCallback(applicationInfo, true)
            }
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity is MainActivity) isMainActivityInForeground = true
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity is MainActivity) isMainActivityInForeground = false
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
