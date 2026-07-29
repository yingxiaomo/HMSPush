package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.LPP

class FakeEmuiOnly : IFakeDevice {
    override fun fake(lpparam: LPP): Boolean {
        fakeProperty(Property.EMUI_VERSION)
        return true
    }
}