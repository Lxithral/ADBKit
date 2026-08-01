package com.lxithral.adbtools

import android.app.Application
import com.topjohnwu.superuser.Shell

class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(20)
        )
    }
}
