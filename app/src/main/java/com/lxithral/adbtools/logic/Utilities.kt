package com.lxithral.adbtools.logic

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell

fun getPrivilegeLevel(
    requiredPrivilegeLevel: PrivilegeLevel = PrivilegeLevel.Root,
    @Suppress("UNUSED_PARAMETER") context: Context? = null,
): PrivilegeLevel {
    if (Shell.isAppGrantedRoot() == true) {
        return PrivilegeLevel.Root
    }
    return PrivilegeLevel.User
}

fun executeShellCommand(
    command: String,
    @Suppress("UNUSED_PARAMETER") context: Context? = null,
    requiredPrivilegeLevel: PrivilegeLevel = PrivilegeLevel.Root,
): String {
    val tag = "Utilities.executeShellCommand"
    val privilegeLevel = getPrivilegeLevel(requiredPrivilegeLevel, context)

    if (privilegeLevel == PrivilegeLevel.Root) {
        val result = Shell.cmd(command).exec().out.joinToString("\n").trim()
        Log.d(tag, "Root: $command -> $result")
        return result
    }

    Log.e(tag, "Failed: no root permission")
    return ""
}
