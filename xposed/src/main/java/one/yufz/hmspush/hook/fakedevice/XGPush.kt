package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.LPP
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.*

open class XGPush : IFakeDevice {
    companion object {
        private const val TAG = "FakeForXGPush"
    }

    override fun fake(lpparam: LPP): Boolean {
        val classLoader = lpparam.classLoader

        XLog.d(TAG, "fake() called with: classLoader = $classLoader")

        return try {
            val classChannelUtils = classLoader.findClass("com.tencent.tpns.baseapi.base.util.ChannelUtils")
            fakeChannels(classChannelUtils)
            true
        } catch (e: ClassNotFoundException) {
            XLog.e(TAG, "fake ClassNotFoundError", e)
            false
        } catch (e: Throwable) {
            XLog.e(TAG, "fake error: ", e)
            false
        }
    }

    private fun fakeChannels(classChannelUtils: Class<*>): Boolean {
        XLog.d(TAG, "fakeChannels() called")

        classChannelUtils.declaredMethods.forEach { method ->
            if (method.name == "isBrandHuaWei") {
                method.hook {
                    replace { true }
                }
            } else if (method.returnType == Boolean::class.java) {
                method.hook {
                    replace { false }
                }
            } else if (method.returnType == String::class.java) {
                method.hook {
                    replace { "" }
                }
            }
        }
        return true
    }
}
