package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.LPP
import one.yufz.xposed.findClass
import one.yufz.xposed.hook

class Alipay : IFakeDevice {
    override fun fake(lpparam: LPP): Boolean {
        lpparam.classLoader.findClass("com.alipay.pushsdk.thirdparty.hw.HuaWeiPushWorker")
            .declaredMethods
            .find { it.returnType == Boolean::class.java }
            ?.hook { replace { true } }

        return true
    }
}