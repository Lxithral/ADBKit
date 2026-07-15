package com.lxithral.adbtools.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lxithral.adbtools.logic.AdbManager
import com.lxithral.adbtools.logic.WirelessDebugging
import com.lxithral.adbtools.logic.executeShellCommand
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

class AdbViewModel(application: Application) : AndroidViewModel(application) {

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

    var fixedPortEnabled by mutableStateOf(value = false)
        private set

    var fixedPortValue by mutableStateOf(value = "")
        private set

    var themeMode by mutableStateOf(value = 0) // 0: System, 1: Light, 2: Dark
        private set

    private val sharedPrefs = application.getSharedPreferences("adb_prefs", Context.MODE_PRIVATE)
    private var pollingJob: Job? = null

    init {
        themeMode = sharedPrefs.getInt("theme_mode", 0)
        fixedPortEnabled = sharedPrefs.getBoolean("fixed_port_enabled", false)
        fixedPortValue = sharedPrefs.getString("fixed_port", "") ?: ""

        startPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                refreshStateSync()
                delay(1.seconds)
            }
        }
    }

    fun refreshState() {
        viewModelScope.launch(Dispatchers.IO) {
            // 重新触发 Root 检查
            Shell.getShell()
            refreshStateSync()
        }
    }

    private fun refreshStateSync() {
        val context = getApplication<Application>()
        wirelessAdbEnabled = WirelessDebugging.getEnabled(context)
        usbAdbEnabled = AdbManager.isUsbAdbEnabled(context)

        developerOptionsEnabled = executeShellCommand("settings get global development_settings_enabled", context).trim() == "1"
        usbInstallEnabled = executeShellCommand("getprop persist.security.adbinstall", context).trim() == "1"
        usbSecurityEnabled = executeShellCommand("getprop persist.security.adbinput", context).trim() == "1"

        rootGranted = Shell.isAppGrantedRoot() == true
        ipAddress = WirelessDebugging.getAddress(context)
        port = WirelessDebugging.getPort(context)
    }

    fun toggleDeveloperOptions(enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        executeAction("settings put global development_settings_enabled $value", "${if (enabled) "开启" else "关闭"}开发者选项")
    }

    fun toggleUsbDebugging(enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        executeAction("settings put global adb_enabled $value", "${if (enabled) "开启" else "关闭"} USB 调试")
    }

    fun toggleUsbInstall(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val isMiui = executeShellCommand("getprop ro.miui.ui.version.name", context).isNotBlank()

            if (isMiui) {
                val xmlFile = "/data/data/com.miui.securitycenter/shared_prefs/remote_provider_preferences.xml"
                val scriptContent = if (enabled) {
                    """
                    #!/system/bin/sh
                    XML_FILE=$xmlFile
                    echo "Enable Installation via USB"
                    LINE_NO=`grep -n "security_adb_install_enable" ${'$'}XML_FILE | awk -F: '{print ${'$'}1}'`
                    if [ "${'$'}LINE_NO" != "" ] && [ ${'$'}LINE_NO -gt 0 ]; then
                        sed -i '/security_adb_install_enable/s/false/true/' ${'$'}XML_FILE
                    else
                        sed -i '3a \    <boolean name="security_adb_install_enable" value="true" />' ${'$'}XML_FILE
                    fi
                    echo "Disable install intercept"
                    LINE_NO=`grep -n "permcenter_install_intercept_enabled" ${'$'}XML_FILE | awk -F: '{print ${'$'}1}'`
                    if [ "${'$'}LINE_NO" != "" ] && [ ${'$'}LINE_NO -gt 0 ]; then
                        sed -i '/permcenter_install_intercept_enabled/s/true/false/' ${'$'}XML_FILE
                    else
                        sed -i '3a \    <boolean name="permcenter_install_intercept_enabled" value="false" />' ${'$'}XML_FILE
                    fi
                    kill -9 $(pidof com.miui.securitycenter.remote)
                    setprop persist.security.adbinstall 1
                    echo "USB 安装已开启"
                    """.trimIndent()
                } else {
                    """
                    #!/system/bin/sh
                    XML_FILE=$xmlFile
                    echo "Disable Installation via USB"
                    LINE_NO=`grep -n "security_adb_install_enable" ${'$'}XML_FILE | awk -F: '{print ${'$'}1}'`
                    if [ "${'$'}LINE_NO" != "" ] && [ ${'$'}LINE_NO -gt 0 ]; then
                        sed -i '/security_adb_install_enable/s/true/false/' ${'$'}XML_FILE
                    else
                        sed -i '3a \    <boolean name="security_adb_install_enable" value="false" />' ${'$'}XML_FILE
                    fi
                    echo "Enable install intercept"
                    LINE_NO=`grep -n "permcenter_install_intercept_enabled" ${'$'}XML_FILE | awk -F: '{print ${'$'}1}'`
                    if [ "${'$'}LINE_NO" != "" ] && [ ${'$'}LINE_NO -gt 0 ]; then
                        sed -i '/permcenter_install_intercept_enabled/s/false/true/' ${'$'}XML_FILE
                    else
                        sed -i '3a \    <boolean name="permcenter_install_intercept_enabled" value="true" />' ${'$'}XML_FILE
                    fi
                    kill -9 $(pidof com.miui.securitycenter.remote)
                    setprop persist.security.adbinstall 0
                    echo "USB 安装已关闭"
                    """.trimIndent()
                }

                try {
                    val tempFile = File(context.cacheDir, "usb_install.sh")
                    tempFile.writeText(scriptContent)
                    // 使用 sh 执行，不一定需要文件系统层级的可执行权限，但加上更稳
                    tempFile.setExecutable(true, false)

                    val result = executeShellCommand("sh ${tempFile.absolutePath}", context)
                    android.util.Log.d("AdbViewModel", "toggleUsbInstall result: ${'$'}result")

                    tempFile.delete()
                } catch (e: Exception) {
                    android.util.Log.e("AdbViewModel", "Script execution failed", e)
                }
            } else {
                val value = if (enabled) "1" else "0"
                executeShellCommand("setprop persist.security.adbinstall $value", context)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "${if (enabled) "开启" else "关闭"} USB 安装", Toast.LENGTH_SHORT).show()
            }
            delay(200.milliseconds)
            refreshStateSync()
        }
    }

    fun toggleUsbSecurity(enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        executeAction("setprop persist.security.adbinput $value", "${if (enabled) "开启" else "关闭"} USB 调试安全设置")
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
            if (enabled) {
                delay(300.milliseconds)
                WirelessDebugging.syncConnectionData(context)
            }
            delay(200.milliseconds)
            refreshStateSync()
        }
    }

    fun setTheme(mode: Int) {
        themeMode = mode
        sharedPrefs.edit { putInt("theme_mode", mode) }
    }

    fun toggleFixedPortEnabled(enabled: Boolean) {
        fixedPortEnabled = enabled
        sharedPrefs.edit { putBoolean("fixed_port_enabled", enabled) }
        if (enabled && fixedPortValue.isNotEmpty()) {
            applyFixedPort(fixedPortValue)
        }
    }

    fun updateFixedPort(port: String) {
        fixedPortValue = port
        sharedPrefs.edit { putString("fixed_port", port) }
        if (fixedPortEnabled && port.isNotEmpty()) {
            applyFixedPort(port)
        }
    }

    private fun applyFixedPort(port: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val command = "setprop service.adb.tls.port $port"
            executeShellCommand(command, context)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "已设置固定端口: $port", Toast.LENGTH_SHORT).show()
            }
            delay(200.milliseconds)
            refreshStateSync()
        }
    }

    private inline fun android.content.SharedPreferences.edit(action: android.content.SharedPreferences.Editor.() -> Unit) {
        val editor = edit()
        action(editor)
        editor.apply()
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
