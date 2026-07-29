package one.yufz.hmspush.hook.hms.dummy

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import one.yufz.hmspush.common.FLAG_HMS_DUMMY_HOOKED
import one.yufz.hmspush.common.HMS_CORE_DUMMY_ACTIVITY
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findClass
import one.yufz.xposed.hookMethod
import java.util.WeakHashMap

object HookDummyActivity {
    private const val TAG = "HookDummyActivity"
    private val instanceFields = WeakHashMap<Any, MutableMap<String, Any?>>()

    private fun setAdditionalInstanceField(obj: Any, key: String, value: Any?) {
        instanceFields.getOrPut(obj) { mutableMapOf() }[key] = value
    }

    private fun getAdditionalInstanceField(obj: Any, key: String): Any? {
        return instanceFields[obj]?.get(key)
    }

    private fun removeAdditionalInstanceField(obj: Any, key: String) {
        instanceFields[obj]?.remove(key)
    }

    fun hook(classLoader: ClassLoader) {
        XLog.d(TAG, "hook() called")

        HookDummyActivityTask.hook(classLoader)

        val classDummyActivity = classLoader.findClass(HMS_CORE_DUMMY_ACTIVITY)
        classDummyActivity.hookMethod("onCreate", Bundle::class.java) {
            doBefore {
                XLog.d(TAG, "onCreate doBefore() called")
                setAdditionalInstanceField(thisObject!!, KEY_IGNORE_FIRST_FINISH, true)
                val activity = thisObject as Activity
                activity.setTheme(android.R.style.Theme_Material_Light_NoActionBar)
                if (args[0] != null) {
                    args[0] = null
                }
            }
            doAfter {
                val activity = thisObject as Activity
                val intent = activity.intent
                val hooked = intent.getBooleanExtra(FLAG_HMS_DUMMY_HOOKED, false)
                XLog.d(TAG, "onCreate doAfter() called, hooked = $hooked")
                if (hooked) {
                    makeActivityFullscreen(thisObject as Activity)
                    addDummyFragment(activity)
                }
            }
        }

        classDummyActivity.hookMethod("finish") {
            doBefore {
                val activity = thisObject as Activity
                val hooked = activity.intent.getBooleanExtra(FLAG_HMS_DUMMY_HOOKED, false)
                val ignoreFirstFinish = getAdditionalInstanceField(activity, KEY_IGNORE_FIRST_FINISH) != null
                XLog.d(TAG, "finish() called, hooked = $hooked, ignoreFirstFinish = $ignoreFirstFinish")
                if (hooked && ignoreFirstFinish) {
                    result = null
                }
                if (ignoreFirstFinish) {
                    removeAdditionalInstanceField(activity, KEY_IGNORE_FIRST_FINISH)
                }
            }
        }
    }

    private fun makeActivityFullscreen(activity: Activity) {
        activity.window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            decorView.systemUiVisibility = decorView.systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun addDummyFragment(activity: Activity) {
        XLog.d(TAG, "addHmsDummyFragment() called")
        activity.fragmentManager.beginTransaction()
            .add(Window.ID_ANDROID_CONTENT, DummyFragment(), "hms_push_dummy")
            .commitNowAllowingStateLoss()
    }
}
