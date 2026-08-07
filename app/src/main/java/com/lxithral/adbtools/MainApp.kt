package com.lxithral.adbtools

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.topjohnwu.superuser.Shell

class MainApp : Application() {

    companion object {
        /**
         * 主界面是否处于前台。
         * 磁贴自杀前会检查它，避免把正在使用的应用误杀。
         */
        @Volatile
        var isMainActivityInForeground: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()

        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(20)
        )

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
