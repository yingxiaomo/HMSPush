package one.yufz.hmspush.hook.system

import android.app.Notification
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.Process
import one.yufz.hmspush.common.ANDROID_PACKAGE_NAME
import one.yufz.hmspush.common.HMS_PACKAGE_NAME
import one.yufz.hmspush.hook.App
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.HookContext
import one.yufz.xposed.hookMethod

object NmsPermissionHooker {
    private const val TAG = "NmsPermissionHooker"

    private fun fromHms() = try {
        Binder.getCallingUid() == getPackageUid(HMS_PACKAGE_NAME)
    } catch (e: Throwable) {
        false
    }

    private fun getPackageUid(packageName: String) = getContext().packageManager.getPackageUid(packageName, 0)

    private fun getContext(): Context = App.current()

    private fun tryHookPermission(packageName: String): Boolean {
        if (packageName != HMS_PACKAGE_NAME && fromHms()) {
            Binder.clearCallingIdentity()
            return true
        }
        return false
    }

    fun hook(classINotificationManager: Class<*>) {
        classINotificationManager.hookMethod("areNotificationsEnabledForPackage", String::class.java, Int::class.java) {
            doBefore { if (tryHookPermission(args[0] as String)) {} }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            classINotificationManager.hookMethod("getNotificationChannelForPackage", String::class.java, Int::class.java, String::class.java, String::class.java, Boolean::class.java) {
                doBefore { if (tryHookPermission(args[0] as String)) {} }
            }
        } else {
            classINotificationManager.hookMethod("getNotificationChannelForPackage", String::class.java, Int::class.java, String::class.java, Boolean::class.java) {
                doBefore { if (tryHookPermission(args[0] as String)) {} }
            }
        }

        classINotificationManager.hookMethod("enqueueNotificationWithTag", String::class.java, String::class.java, String::class.java, Int::class.java, Notification::class.java, Int::class.java) {
            doBefore {
                if (tryHookPermission(args[0] as String)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        args[1] = ANDROID_PACKAGE_NAME
                    }
                }
            }
        }

        try {
            val pcls = classINotificationManager.classLoader?.loadClass("android.content.pm.ParceledListSlice") ?: return
            classINotificationManager.hookMethod("createNotificationChannelsForPackage", String::class.java, Int::class.java, pcls) {
                doBefore { if (tryHookPermission(args[0] as String)) {} }
            }
        } catch (_: Throwable) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            classINotificationManager.hookMethod("cancelNotificationWithTag", String::class.java, String::class.java, String::class.java, Int::class.java, Int::class.java) {
                doBefore { if (tryHookPermission(args[0] as String)) { args[1] = ANDROID_PACKAGE_NAME } }
            }
        } else {
            classINotificationManager.hookMethod("cancelNotificationWithTag", String::class.java, String::class.java, Int::class.java, Int::class.java) {
                doBefore { if (tryHookPermission(args[0] as String)) {} }
            }
        }

        classINotificationManager.hookMethod("deleteNotificationChannel", String::class.java, String::class.java) {
            doBefore { if (tryHookPermission(args[0] as String)) {} }
        }

        classINotificationManager.hookMethod("getAppActiveNotifications", String::class.java, Int::class.java) {
            doBefore { if (tryHookPermission(args[0] as String)) {} }
        }

        classINotificationManager.hookMethod("getNotificationChannelsForPackage", String::class.java, Int::class.java, Boolean::class.java) {
            doBefore { if (tryHookPermission(args[0] as String)) {} }
        }

        val deleteHook: HookContext.() -> Unit = {
            doBefore {
                val pn = args[0] as String
                if (pn != HMS_PACKAGE_NAME && Binder.getCallingUid() == Process.SYSTEM_UID) {
                    args[1] = getPackageUid(pn)
                }
            }
        }
        try {
            val prefsHelper = classINotificationManager.classLoader?.loadClass("com.android.server.notification.PreferencesHelper") ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                prefsHelper.hookMethod("deleteNotificationChannel", String::class.java, Int::class.java, String::class.java, Int::class.java, Boolean::class.java, callback = deleteHook)
            } else {
                prefsHelper.hookMethod("deleteNotificationChannel", String::class.java, Int::class.java, String::class.java, callback = deleteHook)
            }
        } catch (e: NoSuchMethodError) {
            XLog.d(TAG, "hook deleteNotificationChannel error, NoSuchMethodError")
        }
    }
}
