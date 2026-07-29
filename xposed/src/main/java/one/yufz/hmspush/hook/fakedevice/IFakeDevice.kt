package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.LPP

interface IFakeDevice {
    fun fake(lpparam: LPP): Boolean
}
