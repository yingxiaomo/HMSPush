package one.yufz.xposed

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import one.yufz.hmspush.common.doOnce
import one.yufz.hmspush.hook.XLog

private const val TAG = "AndroidHookUtils"

fun onApplicationAttachContext(callback: Application.() -> Unit) {
    ContextWrapper::class.java.hookMethod("attachBaseContext", Context::class.java) {
        doAfter {
            if (thisObject is Application) {
                callback(thisObject as Application)
            }
        }
    }
}

fun onDexClassLoaderLoaded(callback: ClassLoader.(unhook: () -> Unit) -> Unit) {
    var unhooks = mutableListOf<Any>()  // Just a placeholder

    dalvik.system.BaseDexClassLoader::class.java.hookAllConstructor {
        doAfter {
            val hookContext = this@hookAllConstructor
            hookContext.doOnce(thisObject!!) {
                callback(thisObject as ClassLoader) {
                    // unhook placeholder - no-op in new API
                }
            }
        }
    }
}
