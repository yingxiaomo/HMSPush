package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.LPP

class QQ : Common() {

    override fun fake(lpparam: LPP): Boolean {
        if (lpparam.packageName == lpparam.processName || lpparam.processName.endsWith(":MSF")) {
            return super.fake(lpparam)
        }
        return false
    }
}