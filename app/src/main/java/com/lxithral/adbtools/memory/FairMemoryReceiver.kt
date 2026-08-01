package com.lxithral.adbtools.memory

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.RemoteException
import android.util.Log
import java.lang.ref.WeakReference

/**
 * 公平运行内存机制广播接收器
 * 监听内存预警(TRIM)和异常查杀(KILL)广播
 */
class FairMemoryReceiver private constructor() : IBinder.DeathRecipient {

    companion object {
        private const val TAG = "FairMemoryReceiver"

        // 两个不同的 Action
        private const val ACTION_TRIM = "itgsa.intent.action.TRIM"
        private const val ACTION_KILL = "itgsa.intent.action.KILL"
        private const val TRANSACTION_EXCEPTION_REPLY = IBinder.FIRST_CALL_TRANSACTION

        // Bundle Key
        private const val BUNDLE_KEY_COMMON = "common"
        private const val BUNDLE_KEY_EXTRA = "extra"

        // Common Fields
        private const val KEY_NOTIFY_TYPE = "notifyType"
        private const val KEY_NOTIFY_ID = "notifyId"
        private const val KEY_REASON = "reason"
        private const val KEY_ACTION = "action"
        private const val KEY_CALLBACK = "callback"

        // Extra Fields (单位: KB)
        private const val KEY_HEAP_SIZE = "heapSize"
        private const val KEY_HEAP_CAPACITY = "heapCapacity"
        private const val KEY_PSS = "pss"
        private const val KEY_PSS_LIMIT = "pssLimit"

        // Result Codes
        const val RESULT_SUCCESS = 0
        const val RESULT_FAIL = 1

        // Notify Types
        const val NOTIFY_TYPE_PHYSICAL_MEMORY = 1000
        const val NOTIFY_TYPE_JAVA_HEAP = 2000

        // Reason 字符串
        const val REASON_EXCESSIVE_PSS = "Excessive PSS Usage"
        const val REASON_EXCESSIVE_JAVA_HEAP = "Excessive Java Heap Usage"

        // Action 字符串
        const val ACTION_TYPE_TRIM = "trim"
        const val ACTION_TYPE_KILL = "kill"

        @Volatile
        private var instance: FairMemoryReceiver? = null

        fun getInstance(): FairMemoryReceiver {
            return instance ?: synchronized(this) {
                instance ?: FairMemoryReceiver().also { instance = it }
            }
        }
    }

    /**
     * 内存数据类 (单位: KB)
     */
    data class MemoryData(
        val notifyType: Int,
        val notifyId: Int,
        val reason: String,
        val action: String,
        val heapSize: Int = 0,
        val heapCapacity: Int = 0,
        val pss: Int = 0,
        val pssLimit: Int = 0
    )

    /**
     * 内存清理回调接口
     */
    interface MemoryCleaner {
        /**
         * 收到 TRIM 广播时调用 - 应用应释放内存
         */
        fun onTrim(data: MemoryData)

        /**
         * 收到 KILL 广播时调用 - 应用应保存数据
         * @return true 表示数据已保存
         */
        fun onKill(data: MemoryData): Boolean
    }

    private var mRemote: IBinder? = null
    private var mInitialized = false
    private var mHandler: Handler? = null
    private var mHandlerThread: android.os.HandlerThread? = null
    private var mContextRef: WeakReference<Context>? = null
    private val mCleaners = mutableListOf<MemoryCleaner>()

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != ACTION_TRIM && action != ACTION_KILL) {
                return
            }

            val data = intent.extras ?: return

            // 获取公共数据
            val commonBundle = data.getBundle(BUNDLE_KEY_COMMON) ?: return
            val notifyType = commonBundle.getInt(KEY_NOTIFY_TYPE)
            val notifyId = commonBundle.getInt(KEY_NOTIFY_ID)
            val reason = commonBundle.getString(KEY_REASON, "")
            val actionType = commonBundle.getString(KEY_ACTION, "")
            val callbackBinder = commonBundle.getBinder(KEY_CALLBACK)

            // 获取额外数据 (单位: KB)
            val extraData = data.getBundle(BUNDLE_KEY_EXTRA)
            val heapSize = extraData?.getInt(KEY_HEAP_SIZE, 0) ?: 0
            val heapCapacity = extraData?.getInt(KEY_HEAP_CAPACITY, 0) ?: 0
            val pss = extraData?.getInt(KEY_PSS, 0) ?: 0
            val pssLimit = extraData?.getInt(KEY_PSS_LIMIT, 0) ?: 0

            Log.d(TAG, "Received $action: type=$notifyType, id=$notifyId, reason=$reason, actionType=$actionType")
            Log.d(TAG, "Memory: heapSize=${heapSize}KB, heapCapacity=${heapCapacity}KB, pss=${pss}KB, pssLimit=${pssLimit}KB")

            val memoryData = MemoryData(
                notifyType = notifyType,
                notifyId = notifyId,
                reason = reason,
                action = actionType,
                heapSize = heapSize,
                heapCapacity = heapCapacity,
                pss = pss,
                pssLimit = pssLimit
            )

            if (callbackBinder != null) {
                handleReceived(actionType, memoryData, callbackBinder)
            } else {
                Log.w(TAG, "callback binder not found in intent extras.")
            }
        }
    }

    fun initialize(context: Context) {
        synchronized(this) {
            if (!mInitialized) {
                mContextRef = WeakReference(context)
                val handlerThread = android.os.HandlerThread(TAG)
                handlerThread.start()
                mHandlerThread = handlerThread
                mHandler = Handler(handlerThread.looper)

                // 注册两个 Action
                val filter = IntentFilter().apply {
                    addAction(ACTION_TRIM)
                    addAction(ACTION_KILL)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(mReceiver, filter, null, mHandler, Context.RECEIVER_EXPORTED)
                } else {
                    context.registerReceiver(mReceiver, filter, null, mHandler)
                }

                mInitialized = true
                Log.d(TAG, "FairMemoryReceiver initialized (registered for TRIM and KILL)")
            }
        }
    }

    fun registerCleaner(cleaner: MemoryCleaner) {
        synchronized(this) {
            mCleaners.add(cleaner)
        }
    }

    fun unregisterCleaner(cleaner: MemoryCleaner) {
        synchronized(this) {
            mCleaners.remove(cleaner)
        }
    }

    private fun handleReceived(actionType: String, data: MemoryData, callback: IBinder) {
        if (!checkRemote(callback)) {
            return
        }

        when (actionType) {
            ACTION_TYPE_TRIM -> {
                Log.d(TAG, "Processing TRIM request")
                handleTrim(data)
            }
            ACTION_TYPE_KILL -> {
                Log.d(TAG, "Processing KILL request")
                handleKill(data)
            }
            else -> {
                Log.w(TAG, "Unknown action type: $actionType")
            }
        }

        // 回复系统
        val replyData = Bundle().apply {
            putString("reply", "processed")
        }
        reply(data.notifyType, data.notifyId, RESULT_SUCCESS, replyData)
    }

    private fun handleTrim(data: MemoryData) {
        // 通知所有注册的清理器
        synchronized(this) {
            mCleaners.forEach { cleaner ->
                try {
                    cleaner.onTrim(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Cleaner.onTrim error", e)
                }
            }
        }

        // 执行默认清理
        performDefaultTrim(data)
    }

    private fun handleKill(data: MemoryData) {
        // 通知所有注册的清理器
        synchronized(this) {
            mCleaners.forEach { cleaner ->
                try {
                    cleaner.onKill(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Cleaner.onKill error", e)
                }
            }
        }

        // 执行默认保存
        performDefaultKill(data)
    }

    private fun performDefaultTrim(data: MemoryData) {
        val context = mContextRef?.get() ?: return

        when (data.notifyType) {
            NOTIFY_TYPE_PHYSICAL_MEMORY -> {
                Log.d(TAG, "Performing physical memory cleanup (PSS: ${data.pss}KB, Limit: ${data.pssLimit}KB)")
                // 清理应用缓存
                try {
                    context.cacheDir?.deleteRecursively()
                } catch (e: Exception) {
                    Log.e(TAG, "Cache cleanup failed", e)
                }
            }
            NOTIFY_TYPE_JAVA_HEAP -> {
                Log.d(TAG, "Java heap cleanup (Size: ${data.heapSize}KB, Capacity: ${data.heapCapacity}KB)")
            }
        }
    }

    private fun performDefaultKill(data: MemoryData) {
        Log.d(TAG, "Performing default kill handling")
        // KILL 广播时，应用应保存现场数据
        // 默认实现不做特殊处理，依赖注册的 cleaner 处理
    }

    private fun checkRemote(callback: IBinder): Boolean {
        synchronized(this) {
            if (mRemote == null) {
                try {
                    mRemote = callback
                    mRemote?.linkToDeath(this, 0)
                } catch (e: RemoteException) {
                    mRemote = null
                    return false
                }
            }
        }
        return true
    }

    override fun binderDied() {
        synchronized(this) {
            mRemote?.let {
                try {
                    it.unlinkToDeath(this, 0)
                } catch (ignore: Exception) {}
            }
            mRemote = null
        }
    }

    fun reply(notifyType: Int, notifyId: Int, result: Int, extra: Bundle?) {
        synchronized(this) {
            val remote = mRemote ?: return
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInt(notifyType)
                data.writeInt(notifyId)
                data.writeInt(result)
                data.writeBundle(extra ?: Bundle())
                remote.transact(TRANSACTION_EXCEPTION_REPLY, data, reply, IBinder.FLAG_ONEWAY)
                reply.readException()
                Log.d(TAG, "Reply sent: notifyType=$notifyType, notifyId=$notifyId, result=$result")
            } catch (e: Exception) {
                Log.e(TAG, "reply failed.", e)
            } finally {
                reply.recycle()
                data.recycle()
            }
        }
    }

    fun destroy() {
        synchronized(this) {
            // 注销广播接收器
            val context = mContextRef?.get()
            if (context != null) {
                try {
                    context.unregisterReceiver(mReceiver)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unregister receiver", e)
                }
            }

            // 停止 HandlerThread
            mHandlerThread?.quitSafely()
            mHandlerThread = null
            mHandler = null

            mRemote?.let {
                try {
                    it.unlinkToDeath(this, 0)
                } catch (ignore: Exception) {}
            }
            mRemote = null
            mCleaners.clear()
            mContextRef = null
            mInitialized = false
        }
    }
}
