package one.yufz.hmspush.hook.hms
import one.yufz.hmspush.hook.App


import android.content.Context

object StorageContext {

    fun get(): Context {
        return App.current().createDeviceProtectedStorageContext()
    }
}