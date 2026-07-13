package com.lxithral.adbtools.logic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.topjohnwu.superuser.Shell

/**
 * 获取当前最高可用权限等级
 */
fun getPrivilegeLevel(
    requiredPrivilegeLevel: PrivilegeLevel = PrivilegeLevel.Root,
    context: Context? = null,
) : PrivilegeLevel {
    if (Shell.isAppGrantedRoot() == true) {
        return PrivilegeLevel.Root
    }
    
    return PrivilegeLevel.User
}

/**
 * 检查是否有足够权限
 */
fun hasSufficientPrivileges(
    requiredPrivilegeLevel: PrivilegeLevel = PrivilegeLevel.Root,
): Boolean =
    getPrivilegeLevel(requiredPrivilegeLevel).ordinal >= requiredPrivilegeLevel.ordinal

/**
 * 执行 Shell 命令。该方法使用 Root 权限执行。
 */
fun executeShellCommand(
    command: String,
    context: Context? = null,
    requiredPrivilegeLevel: PrivilegeLevel = PrivilegeLevel.Root,
): String {
    val tag = "Utilities.executeShellCommand"
    val privilegeLevel = getPrivilegeLevel(requiredPrivilegeLevel, context)

    if (privilegeLevel == PrivilegeLevel.Root) {
        val result = Shell.cmd(command).exec().out.joinToString("\n").trim()
        Log.d(tag, "Root 执行: $command -> $result")
        return result
    }

    Log.e(tag, "执行失败：无 Root 权限。")
    return ""
}

/**
 * 检查应用是否已安装
 */
fun isPackageInstalled(context: Context, name: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(name, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

/**
 * 复制文本到剪贴板
 */
fun copyText(context: Context, label: String, content: String) {
    val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, content)
    clipboardManager.setPrimaryClip(clip)
}
