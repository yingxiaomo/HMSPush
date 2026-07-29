package one.yufz.hmspush.hook.system

import android.app.Notification
import android.content.Context
import android.os.Build
import one.yufz.hmspush.common.ANDROID_PACKAGE_NAME
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.HookContext
import one.yufz.xposed.findMethodExact
import one.yufz.xposed.hookMethod

object ShortcutPermissionHooker {
    private const val TAG = "ShortcutPermissionHooker"

    fun hook(classShortcutService: Class<*>) {
        try {
            classShortcutService.hookMethod("hasShortcutHostPermission", String::class.java, Int::class.java) {
                doBefore {
                    val callingPkg = args[0] as String
                    if (callingPkg == ANDROID_PACKAGE_NAME) {
                        result = true
                    }
                }
            }
        } catch (e: NoSuchMethodException) {
            // Different Android versions have different params
        }
    }
}
