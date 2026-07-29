package one.yufz.hmspush.hook

import android.app.Application

private var _app: Application? = null

object App {
    fun init(app: Application) {
        _app = app
    }

    fun current(): Application {
        return _app ?: try {
            // Fallback: use ActivityThread hidden API
            val atClass = Class.forName("android.app.ActivityThread")
            val method = atClass.getDeclaredMethod("currentApplication").apply { isAccessible = true }
            method.invoke(null) as Application
        } catch (e: Throwable) {
            throw RuntimeException("Cannot get application", e)
        }
    }
}
