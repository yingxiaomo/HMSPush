package one.yufz.hmspush.hook

import android.util.Log
import one.yufz.xposed.HookContext
import java.lang.reflect.Method

object XLog {
    fun d(tag: String, message: String?) {
        Log.d("HMSPush", "$tag  $message")
    }

    fun i(tag: String, message: String?) {
        Log.i("HMSPush", "$tag  $message")
    }

    fun e(tag: String, message: String?, throwable: Throwable? = null) {
        Log.e("HMSPush", "$tag  $message", throwable)
    }

    fun HookContext.logMethod(tag: String, stackTrace: Boolean = false) {
        d(tag, "╔═══════════════════════════════════════════════════════")
        d(tag, method?.toString() ?: "null")
        d(tag, "${method?.name} called with ${args}")
        if (stackTrace) {
            d(tag, Log.getStackTraceString(Throwable()))
        }
        if (throwable != null) {
            e(tag, "${method?.name} thrown", throwable)
        } else if (method is Method && (method as Method).returnType != Void.TYPE) {
            d(tag, "${method?.name} return $result")
        }
        d(tag, "╚═══════════════════════════════════════════════════════")
    }
}
