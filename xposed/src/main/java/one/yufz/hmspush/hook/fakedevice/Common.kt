package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.LPP
import one.yufz.hmspush.hook.XLog

open class Common : IFakeDevice {
    companion object {
        private const val TAG = "Common"
    }

    override fun fake(lpparam: LPP): Boolean {
        XLog.d(TAG, "fake() called with: packageName = ${lpparam.packageName}")
        fakeAllBuildInProperties()
        return true
    }
}