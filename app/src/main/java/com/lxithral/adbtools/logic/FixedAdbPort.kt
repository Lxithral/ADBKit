package com.lxithral.adbtools.logic

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell

/**
 * 固定无线调试端口（经典 TCP adb 路线）。
 * persist.* 属性由 init 落盘，重启不丢，无需任何开机机制。
 */
object FixedAdbPort {

    private const val TAG = "FixedAdbPortFeature"
    private const val PROP = "persist.adb.tcp.port"
    const val DEFAULT_PORT = 5555

    /** 允许的端口范围：避开系统保留的低端口 */
    fun isValidPort(port: Int): Boolean = port in 1024..65535

    /** 当前固定端口：getprop 全局可读；>0 为已固定端口，0 表示未开启（"-1"/空/非法） */
    fun getPort(context: Context): Int {
        val value = Shell.cmd("getprop $PROP").exec().out.firstOrNull()?.trim()
        return value?.toIntOrNull()?.takeIf { it > 0 } ?: 0
    }

    fun getEnabled(context: Context): Boolean = getPort(context) > 0

    /**
     * 重启 adbd 必须成对执行（只 stop 不 start 会把 USB 调试一并断掉），
     * 因此拼成单条命令保证成对性；执行失败返回 false，由调用方提示。
     */
    fun setEnabled(context: Context, value: Boolean, port: Int = DEFAULT_PORT): Boolean {
        val target = if (value) port else -1
        val result = Shell.cmd("setprop $PROP $target && stop adbd && start adbd").exec()
        Log.d(TAG, "setEnabled: $value port=$target, exit=${result.code}")
        return result.isSuccess
    }
}
