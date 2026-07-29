package one.yufz.hmspush.hook

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import one.yufz.hmspush.common.ANDROID_PACKAGE_NAME
import one.yufz.hmspush.common.HMS_CORE_PROCESS
import one.yufz.hmspush.common.HMS_PACKAGE_NAME
import one.yufz.hmspush.common.doOnce
import one.yufz.hmspush.hook.fakedevice.FakeDevice
import one.yufz.hmspush.hook.hms.HookHMS
import one.yufz.hmspush.hook.system.HookSystemService
import one.yufz.xposed.initXposedInterface

class XposedMod : XposedModule() {
    companion object {
        private const val TAG = "XposedMod"
        private var systemHooked = false
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        initXposedInterface(this)
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        XLog.d(TAG, "hot reloaded")
        // Re-init interface since old handles are invalidated
        initXposedInterface(this)
        // Old hooks are automatically unhooked by default
    }

    override fun onPackageReady(param: PackageReadyParam) {
        val classLoader = param.classLoader
        val packageName = param.packageName
        val processName = param.applicationInfo.processName ?: packageName

        XLog.d(TAG, "Loaded app: $packageName process:$processName")

        if (processName.startsWith("android")) {
            if (!systemHooked) {
                systemHooked = true
                HookSystemService().hook(classLoader)
            }
            return
        }

        if (packageName == HMS_PACKAGE_NAME) {
            if (processName == HMS_CORE_PROCESS) {
                val lpparam = LPP(packageName, processName, classLoader)
                HookHMS().hook(lpparam)
            }
            return
        }

        if (packageName == "com.android.systemui") return

        val lpparam = LPP(packageName, processName, classLoader)
        doOnce(classLoader) { FakeDevice.fake(lpparam) }
    }
}

// Minimal LoadPackageParam stand-in so existing hook code compiles without changes
data class LPP(
    val packageName: String,
    val processName: String,
    val classLoader: ClassLoader
)
