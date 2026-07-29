package one.yufz.hmspush.hook.hms


import android.content.Context

object StorageContext {

    fun get(): Context {
        return App.current().createDeviceProtectedStorageContext()
    }
}