package com.lxithral.adbtools.ui

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lxithral.adbtools.logic.WirelessDebugging
import com.lxithral.adbtools.logic.executeShellCommand
import com.lxithral.adbtools.memory.FairMemoryReceiver
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AdbViewModel(application: Application) : AndroidViewModel(application), FairMemoryReceiver.MemoryCleaner {

    companion object {
        private const val TAG = "AdbViewModel"
    }

    var wirelessAdbEnabled by mutableStateOf(value = false)
        private set

    var usbAdbEnabled by mutableStateOf(value = false)
        private set

    var developerOptionsEnabled by mutableStateOf(value = false)
        private set

    var usbInstallEnabled by mutableStateOf(value = false)
        private set

    var usbSecurityEnabled by mutableStateOf(value = false)
        private set

    var rootGranted by mutableStateOf(value = false)
        private set

    var ipAddress by mutableStateOf(value = "")
        private set

    var port by mutableStateOf(value = "")
        private set

    private val sharedPrefs = application.getSharedPreferences("adb_prefs", Context.MODE_PRIVATE)
    private var pollingJob: Job? = null

    init {
        FairMemoryReceiver.getInstance().registerCleaner(this)
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                refreshStateSync()
                delay(10.seconds)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun refreshState() {
        viewModelScope.launch(Dispatchers.IO) {
            Shell.getShell()
            refreshStateSync()
        }
    }

    private fun refreshStateSync() {
        val context = getApplication<Application>()

        // 合并多条 shell 命令为一次执行
        val batchResult = executeShellCommand(
            "echo \"$(settings get global adb_wifi_enabled)\" && " +
            "echo \"$(settings get global adb_enabled)\" && " +
            "echo \"$(settings get global development_settings_enabled)\" && " +
            "echo \"$(getprop persist.security.adbinstall)\" && " +
            "echo \"$(getprop persist.security.adbinput)\" && " +
            "echo \"$(getprop service.adb.tls.port)\"",
            context
        ).lines()

        wirelessAdbEnabled = batchResult.getOrElse(0) { "" }.trim() == "1" ||
            WirelessDebugging.getEnabled(context)
        usbAdbEnabled = batchResult.getOrElse(1) { "" }.trim() == "1"
        developerOptionsEnabled = batchResult.getOrElse(2) { "" }.trim() == "1"
        usbInstallEnabled = batchResult.getOrElse(3) { "" }.trim() == "1"
        usbSecurityEnabled = batchResult.getOrElse(4) { "" }.trim() == "1"

        rootGranted = Shell.isAppGrantedRoot() == true
        ipAddress = WirelessDebugging.getAddress(context)
        // 批量命令里已带回 service.adb.tls.port；无 Root 时批量命令拿不到，单独补查（属性全局可读）
        port = batchResult.getOrElse(5) { "" }.trim().ifEmpty { WirelessDebugging.getPort(context) }
    }

    fun toggleDeveloperOptions(enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        executeAction("settings put global development_settings_enabled $value", if (enabled) "开启" else "关闭")
    }

    fun toggleUsbDebugging(enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        executeAction("settings put global adb_enabled $value", if (enabled) "开启" else "关闭")
    }

    fun toggleUsbInstall(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val isMiui = executeShellCommand("getprop ro.miui.ui.version.name", context).isNotBlank()

            if (isMiui) {
                val scriptContent = buildMiuioUsbInstallScript(enabled)
                try {
                    val tempFile = File(context.cacheDir, "usb_install.sh")
                    tempFile.writeText(scriptContent)
                    tempFile.setExecutable(true, false)
                    executeShellCommand("sh ${tempFile.absolutePath}", context)
                    tempFile.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Script execution failed", e)
                }
            } else {
                val value = if (enabled) "1" else "0"
                executeShellCommand("setprop persist.security.adbinstall $value", context)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, if (enabled) "已开启 USB 安装" else "已关闭 USB 安装", Toast.LENGTH_SHORT).show()
            }
            delay(200.milliseconds)
            refreshStateSync()
        }
    }

    private fun buildMiuioUsbInstallScript(enable: Boolean): String {
        val xmlFile = "/data/data/com.miui.securitycenter/shared_prefs/remote_provider_preferences.xml"
        val installValue = if (enable) "true" else "false"
        val interceptValue = if (enable) "false" else "true"
        val label = if (enable) "开启" else "关闭"
        val adbInstall = if (enable) "1" else "0"
        return """
            #!/system/bin/sh
            XML_FILE=$xmlFile
            echo "$label Installation via USB"
            LINE_NO=`grep -n "security_adb_install_enable" ${'$'}XML_FILE | awk -F: '{print ${'$'}1}'`
            if [ "${'$'}LINE_NO" != "" ] && [ ${'$'}LINE_NO -gt 0 ]; then
                sed -i '/security_adb_install_enable/s/$installValue/'${if (enable) "true" else "false"}'/' ${'$'}XML_FILE
            else
                sed -i '3a \    <boolean name="security_adb_install_enable" value="$installValue" />' ${'$'}XML_FILE
            fi
            LINE_NO=`grep -n "permcenter_install_intercept_enabled" ${'$'}XML_FILE | awk -F: '{print ${'$'}1}'`
            if [ "${'$'}LINE_NO" != "" ] && [ ${'$'}LINE_NO -gt 0 ]; then
                sed -i '/permcenter_install_intercept_enabled/s/${if (enable) "true" else "false"}/$interceptValue/' ${'$'}XML_FILE
            else
                sed -i '3a \    <boolean name="permcenter_install_intercept_enabled" value="$interceptValue" />' ${'$'}XML_FILE
            fi
            kill -9 $(pidof com.miui.securitycenter.remote)
            setprop persist.security.adbinstall $adbInstall
            echo "USB 安装已$label"
        """.trimIndent()
    }

    fun toggleUsbSecurity(enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        executeAction("setprop persist.security.adbinput $value", if (enabled) "开启" else "关闭")
    }

    private fun executeAction(command: String, label: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            executeShellCommand(command, context)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "已发送命令：$label", Toast.LENGTH_SHORT).show()
            }
            delay(200.milliseconds)
            refreshStateSync()
        }
    }

    fun toggleWirelessAdb(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            WirelessDebugging.setEnabled(context, enabled)
            delay(200.milliseconds)
            refreshStateSync()
            // 端口属性由 adbd 稍后写入，补刷一次
            delay(1500)
            refreshStateSync()
        }
    }

    // MemoryCleaner 实现

    override fun onTrim(data: FairMemoryReceiver.MemoryData) {
        Log.d(TAG, "onTrim: type=${data.notifyType}, pss=${data.pss}KB/${data.pssLimit}KB, heap=${data.heapSize}KB/${data.heapCapacity}KB")

        viewModelScope.launch(Dispatchers.IO) {
            when (data.notifyType) {
                FairMemoryReceiver.NOTIFY_TYPE_PHYSICAL_MEMORY -> {
                    val context = getApplication<Application>()
                    context.cacheDir?.deleteRecursively()
                }
                FairMemoryReceiver.NOTIFY_TYPE_JAVA_HEAP -> {
                    // 依赖系统自动 GC，不手动调用
                }
            }
            delay(100)
            refreshStateSync()
        }
    }

    override fun onKill(data: FairMemoryReceiver.MemoryData): Boolean {
        Log.d(TAG, "onKill: type=${data.notifyType}, saving state...")
        viewModelScope.launch(Dispatchers.IO) {
            saveApplicationState(getApplication())
        }
        return true
    }

    private fun saveApplicationState(context: Context) {
        try {
            sharedPrefs.edit().apply {
                putBoolean("developer_options", developerOptionsEnabled)
                putBoolean("usb_debugging", usbAdbEnabled)
                putBoolean("wireless_debugging", wirelessAdbEnabled)
                putBoolean("usb_install", usbInstallEnabled)
                putBoolean("usb_security", usbSecurityEnabled)
                apply()
            }
            Log.d(TAG, "Application state saved")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save state", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        FairMemoryReceiver.getInstance().unregisterCleaner(this)
    }
}
