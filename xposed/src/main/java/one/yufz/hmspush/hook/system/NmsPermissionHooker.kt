package one.yufz.hmspush.hook.system

import one.yufz.hmspush.hook.XLog

object NmsPermissionHooker {
    private const val TAG = "NmsPermissionHooker"

    fun hook(classINotificationManager: Class<*>) {
        XLog.d(TAG, "hook() called - simplified for libxposed migration")
    }
}
