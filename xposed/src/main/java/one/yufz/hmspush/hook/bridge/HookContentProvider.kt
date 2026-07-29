package one.yufz.hmspush.hook.bridge
import one.yufz.hmspush.hook.App

import android.net.Uri
import android.os.Binder
import one.yufz.hmspush.common.APPLICATION_ID
import one.yufz.xposed.findClass
import one.yufz.xposed.hookMethod

class HookContentProvider {
    fun hook(classLoader: ClassLoader) {
        val classModuleQueryProvider = classLoader.findClass("com.huawei.hms.dynamic.module.manager.query.ModuleQueryProvider")

        val bridge = BridgeContentProvider()
        classModuleQueryProvider.hookMethod("query", Uri::class.java, Array<String>::class.java, String::class.java, Array<String>::class.java, String::class.java) {
            doBefore {
                result = bridge.query(args[0] as Uri, args[1] as Array<String>?, args[2] as String?, args[3] as Array<String>?, args[4] as String?)
            }
        }
    }

    private fun fromHmsPush() = try {
        val callingUid = Binder.getCallingUid()
        val app = App.current()
        callingUid == app.packageManager.getPackageUid(APPLICATION_ID, 0)
                || callingUid == 2000 || callingUid == 0
    } catch (e: Throwable) {
        false
    }
}
