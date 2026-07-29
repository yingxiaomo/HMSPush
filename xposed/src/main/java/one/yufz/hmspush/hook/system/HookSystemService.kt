package one.yufz.hmspush.hook.system

import android.app.NotificationManager
import android.content.Context
import android.os.Binder
import android.os.Build
import one.yufz.hmspush.common.ANDROID_PACKAGE_NAME
import one.yufz.hmspush.common.IS_SYSTEM_HOOK_READY
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.callMethod
import one.yufz.xposed.callStaticMethod
import one.yufz.xposed.deoptimizeMethod
import one.yufz.xposed.findClass
import one.yufz.xposed.findMethodExact
import one.yufz.xposed.get
import one.yufz.xposed.hook
import one.yufz.xposed.hookMethod

class HookSystemService {
    companion object {
        private const val TAG = "HookSystemService"

        val isSystemHookReady: Boolean by lazy {
            try {
                val nm = NotificationManager::class.java.callStaticMethod("getService")
                nm?.callMethod("isSystemConditionProviderEnabled", IS_SYSTEM_HOOK_READY) as Boolean
            } catch (t: Throwable) {
                XLog.e(TAG, "isSystemHookReady error", t)
                false
            }
        }
    }

    fun hook(classLoader: ClassLoader) {
        val classNotificationManagerService = classLoader.findClass("com.android.server.notification.NotificationManagerService")

        classNotificationManagerService.hookMethod("onStart") {
            doAfter {
                val context = thisObject!!.callMethod("getContext") as Context
                KeepHmsAlive(context).start()
                val stubClass = thisObject!!.get<Any>("mService").javaClass
                hookPermission(stubClass)
                hookSystemReadyFlag(stubClass)
            }
        }

        classNotificationManagerService.hookMethod("isPackageSuspendedForUser", String::class.java, Int::class.java) {
            doBefore {
                if (Binder.getCallingUid() == 1000) {
                    result = false
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            findMethodExact(classNotificationManagerService, "resolveNotificationUid",
                String::class.java, String::class.java, Int::class.java, Int::class.java)
                .deoptimizeMethod()

            try {
                classNotificationManagerService.hookMethod("isCallerAndroid", String::class.java, Int::class.java) {
                    doBefore {
                        val callingPkg = args[0] as String
                        if (callingPkg == ANDROID_PACKAGE_NAME) {
                            result = true
                        }
                    }
                }
            } catch (e: NoSuchMethodError) {
                XLog.d(TAG, "hook isCallerAndroid error, NoSuchMethodError")
            }
        }

        val classShortcutService = classLoader.findClass("com.android.server.pm.ShortcutService")
        ShortcutPermissionHooker.hook(classShortcutService)
    }

    private fun hookSystemReadyFlag(stubClass: Class<Any>) {
        stubClass.hookMethod("isSystemConditionProviderEnabled", String::class.java) {
            doBefore {
                if (args[0] == IS_SYSTEM_HOOK_READY) {
                    result = true
                }
            }
        }
    }

    private fun hookPermission(stubClass: Class<Any>) {
        NmsPermissionHooker.hook(stubClass)
    }
}
